/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.keyvalue.repository.query.KeyValuePartTreeQuery;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.StreamUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import static io.tarantool.spring.data27.query.PaginationUtils.castToTarantoolPageable;
import static io.tarantool.spring.data27.query.PaginationUtils.doPageQuery;
import static io.tarantool.spring.data27.query.PaginationUtils.doPaginationQuery;
import io.tarantool.client.crud.options.SelectOptions;
import io.tarantool.spring.data.query.PaginationDirection;

/**
 * There is one instance for each query method defined for a repository, providing a query from the
 * bind parameters.
 *
 * @author Artyom Dubinin
 */
public class TarantoolPartTreeQuery extends KeyValuePartTreeQuery {

  public static final String ILLEGAL_RETURN_TYPE_FOR_DELETE =
      "Illegal returned type: %s. The operation 'deleteBy' accepts only 'long' and 'Collection' as"
          + " the returned object type";
  public static final String QUERY_METHOD_S_NOT_SUPPORTED = "Query method '%s' not supported.";
  private final QueryMethod queryMethod;
  private final KeyValueOperations keyValueOperations;
  private final PartTree tree;

  private final boolean isCount;
  private final boolean isDelete;
  private final boolean isDistinct;
  private final boolean isExists;
  private final Class<?> targetType;
  private final Class<?> returnType;

  private boolean isRearrangeKnown;
  private boolean isRearrangeRequired;
  private int[] rearrangeIndex;

  /**
   * Create a {@link RepositoryQuery} implementation for each query method defined in a tarantool
   * repository.
   *
   * @param queryMethod Method defined in Tarantool Repositories
   * @param evaluationContextProvider Not used
   * @param keyValueOperations Interface to Tarantool
   * @param queryCreator Not used
   */
  public TarantoolPartTreeQuery(
      QueryMethod queryMethod,
      QueryMethodEvaluationContextProvider evaluationContextProvider,
      KeyValueOperations keyValueOperations,
      Class<? extends AbstractQueryCreator<?, ?>> queryCreator) {
    super(queryMethod, evaluationContextProvider, keyValueOperations, queryCreator);
    this.queryMethod = queryMethod;
    this.keyValueOperations = keyValueOperations;
    this.isRearrangeKnown = false;
    this.targetType = queryMethod.getEntityInformation().getJavaType();
    this.returnType = queryMethod.getReturnedObjectType();
    this.tree = new PartTree(getQueryMethod().getName(), targetType);
    if (queryMethod.getParameters().getNumberOfParameters() > 0) {
      this.isCount = tree.isCountProjection();
      this.isDelete = tree.isDelete();
      this.isDistinct = tree.isDistinct();
      this.isExists = tree.isExistsProjection();
    } else {
      this.isCount = false;
      this.isDelete = false;
      this.isDistinct = false;
      this.isExists = false;
    }
  }

  /**
   * Execute this query instance, using any invocation parameters.
   *
   * <p>Expecting {@code findBy...()}, {@code countBy...()} or {@code deleteBy...()}
   *
   * @param parameters Any parameters
   * @return Query result
   */
  @Override
  public Object execute(@NonNull Object[] parameters) {
    ParametersParameterAccessor accessor = this.prepareAccessor(parameters, tree);

    KeyValueQuery<?> query = prepareQuery(accessor);

    if (this.isCount) {
      if (this.isDistinct) {
        final Iterable<?> iterable = this.keyValueOperations.find(query, targetType);
        return StreamUtils.createStreamFromIterator(iterable.iterator()).distinct().count();
      }
      return this.keyValueOperations.count(query, targetType);
    }

    if (this.isDelete) {
      return this.executeDeleteQuery(query);
    }

    if (this.isExists) {
      query.setOffset(0);
      query.setRows(1);
      final Iterable<?> result = this.keyValueOperations.find(query, targetType);
      return result.iterator().hasNext();
    }

    if (queryMethod.isPageQuery()) {
      return this.executePageQuery(query, accessor);
    }

    if (queryMethod.isSliceQuery()) {
      return this.executeSliceQuery(query, accessor);
    }

    if (queryMethod.isCollectionQuery()
        || queryMethod.isQueryForEntity()
        || queryMethod.isStreamQuery()) {
      return this.executeFindQuery(query);
    }

    throw new UnsupportedOperationException(
        String.format(QUERY_METHOD_S_NOT_SUPPORTED, queryMethod.getName()));
  }

  /**
   * Execute a "delete" query, not really a query more of an operation.
   *
   * <p>
   *
   * @param query The query to run
   * @return Collection of deleted objects or the number of deleted objects
   */
  private Object executeDeleteQuery(final KeyValueQuery<?> query) {

    Iterable<?> resultSet = this.keyValueOperations.find(query, targetType);
    Iterator<?> iterator = resultSet.iterator();

    List<Object> result = new ArrayList<>();
    while (iterator.hasNext()) {
      result.add(this.keyValueOperations.delete(iterator.next()));
    }

    if (queryMethod.isCollectionQuery()) {
      return result;
    }
    if (long.class.equals(returnType) || Long.class.equals(returnType)) {
      return result.size();
    }
    throw new UnsupportedOperationException(
        String.format(ILLEGAL_RETURN_TYPE_FOR_DELETE, returnType));
  }

  /**
   * Execute a retrieval query. The query engine will return this in an iterator, which may need
   * conversion to a single domain entity or a stream.
   *
   * @param query The query to run
   * @return Query result
   */
  private Object executeFindQuery(final KeyValueQuery<?> query) {

    Iterable<?> resultSet = this.keyValueOperations.find(query, targetType);

    if (!queryMethod.isCollectionQuery()
        && !queryMethod.isPageQuery()
        && !queryMethod.isSliceQuery()
        && !queryMethod.isStreamQuery()) {
      // Singleton result
      return resultSet.iterator().hasNext() ? resultSet.iterator().next() : null;
    }

    Stream<?> stream = StreamUtils.createStreamFromIterator(resultSet.iterator());
    if (this.isDistinct) {
      stream = stream.distinct();
    }

    if (queryMethod.isStreamQuery()) {
      return stream;
    }

    // optimization:
    // we can omit if condition
    // but this will recreate result even if it's not distinct and isn't stream query
    if (this.isDistinct) {
      return stream.collect(Collectors.toList());
    }

    return resultSet;
  }

  /**
   * Execute the slice request.
   *
   * @param query query
   * @param accessor accessor
   * @return slice selection result.
   */
  private Object executePageQuery(
      final KeyValueQuery<?> query, ParametersParameterAccessor accessor) {

    Pageable pageParams = accessor.getPageable();
    return doPageQuery(pageParams, query, this.keyValueOperations, targetType);
  }

  private Object executeSliceQuery(
      final KeyValueQuery<?> query, ParametersParameterAccessor accessor) {

    Pageable sliceParams = accessor.getPageable();

    if (sliceParams.isUnpaged()) {
      return new TarantoolSliceImpl<>();
    }

    TarantoolPageable<?> resultSliceParams = castToTarantoolPageable(sliceParams);

    int pageSize = resultSliceParams.getPageSize();

    List<?> content =
        doPaginationQuery(
            query, resultSliceParams, pageSize + 1, this.keyValueOperations, targetType);

    if (content.isEmpty()) {
      return new TarantoolSliceImpl<>();
    }

    boolean hasNext = content.size() > resultSliceParams.getPageSize();
    PaginationDirection paginationDirection = resultSliceParams.getPaginationDirection();

    List<?> result = content;
    if (hasNext) {
      switch (paginationDirection) {
        case FORWARD:
          {
            result = content.subList(0, pageSize);
            break;
          }
        case BACKWARD:
          {
            result = content.subList(1, content.size());
            break;
          }
      }
    }
    return new TarantoolSliceImpl<>(result, resultSliceParams, hasNext);
  }

  /**
   * Create the query from the bind parameters.
   *
   * @return A ready-to-use query
   */
  @NonNull
  protected KeyValueQuery<?> prepareQuery(ParametersParameterAccessor accessor) {
    KeyValueQuery<?> query = createQuery(accessor);

    /*
    If there is no limitation in the name of the method, we always put a
    limit so that don't think about this in the engine, especially when distinguishing between a regular query and a
    paginated query.
    */
    if (!tree.isLimiting()) {
      query.setRows(SelectOptions.DEFAULT_LIMIT);
    }

    if (accessor.getSort() != Sort.unsorted()) {
      query.setSort(accessor.getSort());
    }

    return query;
  }

  /**
   * Handle {@code @Param}.
   *
   * <OL>
   *   <li><b>Without {@code @Param}</b>
   *       <p>Arguments to the call are assumed to follow the same sequence as cited in the method
   *       name. <br>
   *       Eg.
   *       <pre>
   *     findBy<U>One</U>And<U>Two</U>(String <U>one</U>, String <U>two</U>);
   *     </pre>
   *   <li><b>With {@code @Param}</b>
   *       <p>Arguments to the call are use the {@code @Param} to match them against the fields.
   *       <p>Eg.
   *       <pre>
   *   findBy<U>One</U>And<U>Two</U>(@Param("two") String <U>two</U>, @Param("one") String <U>one</U>);
   *   </pre>
   * </ol>
   *
   * @param originalParameters Possibly empty
   * @param partTree Query tree to traverse
   * @return Parameters in correct order
   */
  private ParametersParameterAccessor prepareAccessor(
      final Object[] originalParameters, final PartTree partTree) {

    if (!this.isRearrangeKnown) {
      this.prepareRearrange(partTree, this.queryMethod.getParameters().getBindableParameters());
      this.isRearrangeKnown = true;
    }

    Object[] parameters = originalParameters;
    Assert.notNull(parameters, "Parameters must not be null.");

    if (this.isRearrangeRequired) {
      parameters = new Object[originalParameters.length];

      for (int i = 0; i < parameters.length; i++) {
        int index = (i < rearrangeIndex.length) ? rearrangeIndex[i] : i;
        parameters[i] = originalParameters[index];
      }
    }

    return new ParametersParameterAccessor(this.queryMethod.getParameters(), parameters);
  }

  /**
   * Determine if the arguments to the method need reordered.
   *
   * <p>For searches such as {@code findBySomethingNotNull} there may be more parts than parameters
   * needed to be bound to them.
   *
   * @param partTree Query parts
   * @param bindableParameters Parameters expected
   */
  @SuppressWarnings("unchecked")
  private void prepareRearrange(
      final PartTree partTree, final Parameters<?, ?> bindableParameters) {

    this.isRearrangeRequired = false;
    if (partTree == null || bindableParameters == null) {
      return;
    }

    List<String> queryParams = new ArrayList<>();
    List<String> methodParams = new ArrayList<>();

    for (Part part : partTree.getParts()) {
      queryParams.add(part.getProperty().getSegment());
    }

    Iterator<Parameter> bindableParameterIterator =
        (Iterator<Parameter>) bindableParameters.iterator();
    while (bindableParameterIterator.hasNext()) {
      Parameter parameter = bindableParameterIterator.next();
      parameter.getName().ifPresent(methodParams::add);
    }

    this.rearrangeIndex = new int[queryParams.size()];

    String[] paramsExpected = queryParams.toArray(new String[0]);
    String[] paramsProvided = methodParams.toArray(new String[0]);

    for (int i = 0; i < this.rearrangeIndex.length; i++) {
      this.rearrangeIndex[i] = i;

      for (int j = 0; j < paramsProvided.length; j++) {
        if (paramsProvided[j] != null && paramsProvided[j].equals(paramsExpected[i])) {
          this.rearrangeIndex[i] = j;
          this.isRearrangeRequired = true;
        }
      }
    }
  }
}
