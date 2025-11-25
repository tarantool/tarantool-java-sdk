/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;
import org.msgpack.value.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.core.protocol.BoxIterator;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.mapping.TarantoolJacksonMapping;
import io.tarantool.mapping.Tuple;

/**
 * @author Artyom Dubinin
 */
public class TarantoolSchemaFetcher {

  static final Logger log = LoggerFactory.getLogger(TarantoolSchemaFetcher.class);

  private static final int BOX_VSPACE_ID = 281;
  private static final int BOX_VINDEX_ID = 289;
  public static final int PRIMARY = 0; // Primary index has always ID 0;
  private static final int SPACE_MAX =
      65_000; // ref: https://www.tarantool.io/en/doc/latest/book/box/limitations/
  private static final int SPACE_INDEX_MAX =
      128; // ref: https://www.tarantool.io/en/doc/latest/book/box/limitations/
  private static final int INDEX_MAX = SPACE_MAX * SPACE_INDEX_MAX;
  private static final int OFFSET = 0;
  private static final BoxIterator ITERATOR = BoxIterator.EQ;
  public static final TypeReference<List<Tuple<Space>>> LIST_TUPLE_SPACE =
      new TypeReference<List<Tuple<Space>>>() {};
  public static final TypeReference<List<Tuple<Index>>> LIST_TUPLE_INDEX =
      new TypeReference<List<Tuple<Index>>>() {};
  private final TarantoolBalancer balancer;
  private final boolean ignoreOldSchemaVersion;

  private Long schemaVersion;
  private final Map<String, Space> spaceHolderByName;
  private final Map<Integer, Space> spaceHolderById;

  public TarantoolSchemaFetcher(TarantoolBalancer balancer, boolean ignoreOldSchemaVersion) {
    this.balancer = balancer;
    this.schemaVersion = 0L;
    this.spaceHolderByName = new HashMap<>();
    this.spaceHolderById = new HashMap<>();
    this.ignoreOldSchemaVersion = ignoreOldSchemaVersion;
    fetchSchema();
  }

  public CompletableFuture<IProtoResponse> processRequest(
      CompletableFuture<IProtoResponse> request) {
    return request.thenCompose(
        requestResponse -> {
          Long responseSchemaVersion = requestResponse.getSchemaVersion();
          if (responseSchemaVersion.equals(schemaVersion)) {
            return CompletableFuture.completedFuture(requestResponse);
          } else if (responseSchemaVersion < schemaVersion) {
            log.error("Response has older schema version than client has");
            if (ignoreOldSchemaVersion) {
              return CompletableFuture.completedFuture(requestResponse);
            }
            throw new SchemaFetchingException("Response has older schema version than client has");
          }

          return vspaceSelect()
              .thenCombine(
                  vindexSelect(),
                  (schemaResponse, indexesResponse) -> {
                    updateSchema(
                        TarantoolJacksonMapping.readResponse(schemaResponse, LIST_TUPLE_SPACE)
                            .get(),
                        TarantoolJacksonMapping.readResponse(indexesResponse, LIST_TUPLE_INDEX)
                            .get());
                    long newSchemaVersion = schemaResponse.getSchemaVersion();
                    if (newSchemaVersion > schemaVersion) {
                      schemaVersion = newSchemaVersion;
                    }
                    return requestResponse;
                  });
        });
  }

  public void updateSchema(List<Tuple<Space>> spaces, List<Tuple<Index>> indexes) {
    int i = 0;
    int indexesSize = indexes.size();
    for (Tuple<Space> tuple : spaces) {
      Space space = tuple.get();
      int spaceId = space.getId();
      while (i < indexesSize && indexes.get(i).get().getSpaceId() <= spaceId) {
        Index index = indexes.get(i).get();
        if (index.getSpaceId() == spaceId) {
          space.addIndex(index);
        }
        i++;
      }

      spaceHolderById.put(space.getId(), space);
      spaceHolderByName.put(space.getName(), space);
    }
  }

  public void fetchSchema() {
    CompletableFuture<IProtoResponse> vspaceRequest = vspaceSelect();
    CompletableFuture<IProtoResponse> vindexRequest = vindexSelect();
    CompletableFuture.allOf(vspaceRequest, vindexRequest).join();
    IProtoResponse vspaceRequestResponse = vspaceRequest.join();
    IProtoResponse vindexRequestResponse = vindexRequest.join();
    List<Tuple<Space>> spaces =
        TarantoolJacksonMapping.readResponse(vspaceRequestResponse, LIST_TUPLE_SPACE).get();
    List<Tuple<Index>> indexes =
        TarantoolJacksonMapping.readResponse(vindexRequestResponse, LIST_TUPLE_INDEX).get();
    updateSchema(spaces, indexes);
    schemaVersion = vspaceRequestResponse.getSchemaVersion();
  }

  private CompletableFuture<IProtoResponse> vspaceSelect() {
    return balancer
        .getNext()
        .thenCompose(
            c ->
                c.select(
                    BOX_VSPACE_ID,
                    PRIMARY,
                    ValueFactory.emptyArray(),
                    SPACE_MAX,
                    OFFSET,
                    ITERATOR));
  }

  private CompletableFuture<IProtoResponse> vindexSelect() {
    return balancer
        .getNext()
        .thenCompose(
            c ->
                c.select(
                    BOX_VINDEX_ID,
                    PRIMARY,
                    ValueFactory.emptyArray(),
                    INDEX_MAX,
                    OFFSET,
                    ITERATOR));
  }

  public Space getSpace(String spaceName) {
    Space space = spaceHolderByName.get(spaceName);
    if (space == null) {
      this.fetchSchema(); // may schema has been updated
      space = spaceHolderByName.get(spaceName);
      if (space == null) {
        throw new NoSchemaException("No schema for space: " + spaceName);
      }
    }

    return space;
  }

  public Space getSpace(Integer id) {
    Space space = spaceHolderById.get(id);
    if (space == null) {
      this.fetchSchema(); // may schema has been updated
      space = spaceHolderById.get(id);
      if (space == null) {
        throw new NoSchemaException("No schema for space: " + id);
      }
    }

    return space;
  }

  public Long getSchemaVersion() {
    return schemaVersion;
  }
}
