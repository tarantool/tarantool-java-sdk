---
uid: spring-data
---

# Введение (Tarantool Spring Data)

В данном разделе представлена документация об инструментах, позволяющий использовать Tarantool в
программах построенных на базе экосистемы Spring посредством предоставления реализаций Spring Data.

Для использования клиента `Tarantool Spring Data` добавьте следующую зависимость в `pom.xml` вашего
проекта:

```xml

<dependency>
  <groupId>io.tarantool</groupId>
  <!-- Если используется Spring 2.7.x, 3.1.x, 3.2.x, 3.3.x то используйте одну из следующих зависимостей: -->
  <!--
  <artifactId>tarantool-spring-data-27</artifactId>
  <artifactId>tarantool-spring-data-31</artifactId>
  <artifactId>tarantool-spring-data-32</artifactId>
  <artifactId>tarantool-spring-data-33</artifactId>
  -->
  <artifactId>tarantool-spring-data-34</artifactId>
  <version>${tarantool-java-sdk.version}</version>
</dependency>
```
