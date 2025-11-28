---
_layout: landing
---

<a href="https://tarantool.org">
  <img src="https://avatars2.githubusercontent.com/u/2344919?v=2&s=250" align="right" alt="">
</a>

:::
**Tarantool Java SDK** - набор библиотек для взаимодействия с Tarantool из Java.
:::

:::
Для работы с Tarantool используется фреймворк [Netty](https://netty.io) для асинхронного сетевого
взаимодействия и библиотека [MessagePack](https://github.com/msgpack/msgpack-java) для сериализации
и десериализации данных. Библиотеки обеспечивают полную поддержку всего протокола Tarantool
(IProto). Они предоставляют Box API, для работы с отдельными серверами Tarantool. Crud API[^1] для
работы с кластерами (Tarantool DB, Tarantool EE), а также интерфейсы для интеграции со Spring
:::

`JavaDoc Tarantool Java SDK` доступен по [ссылке](javadoc/index.html)

[^1]: CRUD API — является прокси к API [tarantool/crud](https://github.com/tarantool/crud)
