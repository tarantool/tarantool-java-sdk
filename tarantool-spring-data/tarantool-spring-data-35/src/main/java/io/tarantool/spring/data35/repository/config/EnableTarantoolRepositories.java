/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data35.repository.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.repository.config.QueryCreatorType;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;

import static io.tarantool.spring.data.TarantoolBeanNames.DEFAULT_TARANTOOL_KEY_VALUE_TEMPLATE_REF;
import io.tarantool.client.ClientType;
import io.tarantool.spring.data35.config.properties.TarantoolProperties;
import io.tarantool.spring.data35.query.TarantoolQueryCreator;
import io.tarantool.spring.data35.repository.support.TarantoolRepositoryFactoryBean;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(TarantoolRepositoriesRegistrar.class)
@EnableConfigurationProperties(TarantoolProperties.class)
@QueryCreatorType(TarantoolQueryCreator.class)
public @interface EnableTarantoolRepositories {

  /**
   * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
   * declarations e.g.: {@code @EnableJpaRepositories("org.my.pkg")} instead of
   * {@code @EnableJpaRepositories(basePackages="org.my.pkg")}.
   *
   * @return value
   */
  String[] value() default {};

  /**
   * Base packages to scan for annotated components. {@link #value()} is an alias for (and mutually
   * exclusive with) this attribute. Use {@link #basePackageClasses()} for a type-safe alternative
   * to String-based package names.
   *
   * @return base packages
   */
  String[] basePackages() default {};

  /**
   * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for
   * annotated components. The package of each class specified will be scanned. Consider creating a
   * special no-op marker class or interface in each package that serves no purpose other than being
   * referenced by this attribute.
   *
   * @return classes
   */
  Class<?>[] basePackageClasses() default {};

  /**
   * Specifies which types are not eligible for component scanning.
   *
   * @return filters
   */
  Filter[] excludeFilters() default {};

  /**
   * Specifies which types are eligible for component scanning. Further, narrows the set of
   * candidate components from everything in {@link #basePackages()} to everything in the base
   * packages that matches the given filter or filters.
   *
   * @return filters
   */
  Filter[] includeFilters() default {};

  /**
   * Returns the postfix to be used when looking up custom repository implementations. Defaults to
   * {@literal Impl}. So for a repository named {@code PersonRepository} the corresponding
   * implementation class will be looked up scanning for {@code PersonRepositoryImpl}.
   *
   * @return postfix
   */
  String repositoryImplementationPostfix() default "Impl";

  /**
   * Configures the location of where to find the Spring Data named queries properties file.
   *
   * @return location
   */
  String namedQueriesLocation() default "";

  /**
   * Returns the key of the {@link QueryLookupStrategy} to be used for lookup queries for query
   * methods. Defaults to {@link Key#CREATE_IF_NOT_FOUND}.
   *
   * @return strategy
   */
  Key queryLookupStrategy() default Key.CREATE_IF_NOT_FOUND;

  Class<?> repositoryFactoryBeanClass() default TarantoolRepositoryFactoryBean.class;

  /**
   * Allow custom base classes, for generic behavior shared amongst selected repositories.
   *
   * @return base class
   */
  Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;

  /**
   * Configures the name of the {@link KeyValueOperations} bean to be used with the repositories
   * detected.
   *
   * @return reference to {@link KeyValueOperations} bean.
   */
  String keyValueTemplateRef() default DEFAULT_TARANTOOL_KEY_VALUE_TEMPLATE_REF;

  /**
   * Configures whether nested repository-interfaces (e.g. defined as inner classes) should be
   * discovered by the repositories' infrastructure.
   *
   * @return result of consideration
   */
  boolean considerNestedRepositories() default false;

  /**
   * Configures the {@link ClientType} to be used as driver client type to connection to Tarantool.
   * Must match the referenced type in {@link #keyValueTemplateRef()}} if these values differ from
   * the default ones.
   *
   * @return the type of client through which communication with Tarantool will be made.
   */
  ClientType clientType() default ClientType.CRUD;
}
