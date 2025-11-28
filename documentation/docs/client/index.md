---
uid: high-client
---

# Введение (Высокоуровневый клиент к Tarantool)

В данном разделе представлена документация о высокоуровневом клиенте к Tarantool, позволяющий
подключаться к Tarantool как к одному узлу, так и к кластеру через маршрутизаторы.

Для использования клиента `Tarantool` добавьте следующую зависимость в `pom.xml` вашего проекта:

```xml

<dependency>
  <groupId>io.tarantool</groupId>
  <artifactId>tarantool-client</artifactId>
  <version>${tarantool-java-sdk.version}</version>
</dependency>
```
