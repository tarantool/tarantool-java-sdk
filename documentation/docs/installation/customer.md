---
uid: customer
---

# Ручная загрузка артефактов из клиентской зоны

Необходимые шаги:

1. Необходимо [зарегистрироваться](https://www.tarantool.io/en/accounts/customer_zone) с рабочей
   почты компании. Так же необходимо, чтобы для почты компании предоставили доступ в
   [клиентскую зону](https://www.tarantool.io/en/accounts/customer_zone).
2. Скачать артефакты из
   [репозитория](https://www.tarantool.io/en/accounts/customer_zone/packages/maven)
3. Добавить артефакты в локальный репозиторий <<maven>>/<<gradle>> (расположение по умолчанию:
   `~/.m2`) или в собственный закрытый репозиторий.
4. Убедиться, что репозиторий доступен с рабочей машины (случае собственного закрытого репозитория)
5. Импортировать необходимую зависимость в `pom.xml` проекта. Например:

```xml

<dependency>
  <groupId>io.tarantool</groupId>
  <artifactId>tarantool-client</artifactId>
  <version>${tarantool-java-sdk.version}</version>
</dependency>
```
