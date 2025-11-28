---
uid: maven-central
---

# Загрузка артефактов из maven-central

Необходимые шаги:

1. Убедиться, что публичные репозитории `maven-central` доступны с рабочей машины.
2. Импортировать необходимую зависимость в `pom.xml` проекта. Например:

```xml

<dependency>
  <groupId>io.tarantool</groupId>
  <artifactId>tarantool-client</artifactId>
  <version>${tarantool-java-sdk.version}</version>
</dependency>
```
