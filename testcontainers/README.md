## Модуль Testcontainers для Tarantool

В данном проекте содержится реализация модуля `testcontainers` для `Tarantool`.

Поддерживаемые варианты использования:

| Название            | Версия продукта                                            |
|---------------------|------------------------------------------------------------|
| Один узел Tarantool | <ul><li>[x] 3.x </li><li>[ ] 2.11.x</li></ul>              |
| Кластер TDB         | <ul><li>[x] 3.x </li><li>[x] 2.x</li><li>[ ] 1.x</li></ul> |
| Кластер TQE         | <ul><li>[x] 2.x </li><li>[ ] 3.x</li></ul>                 |

### Архитектура

Информацию об архитектуре модуля, можно найти:

* [Архитектура решения "Один узел Tarantool"](doc/001-single-node-testcontainers-arch.md)
* [Архитектура решения "Кластер TDB"](./doc/004-tdb-cluster-testcontainers-arch.md)
* [Архитектура решения «Кластер TQE»](doc/007-tqe-cluster-arch.md)

### Примеры использования

Примеры использования модуля можно найти:

* [Пример использования testcontainers для одного узла Tarantool](./doc/003-single-node-testcontainers-standard-impl-example.md)
* [Пример использования testcontainers для кластера TDB](./doc/006-tdb2-cluster-testcontainers-standart-impl-example.md)
* [Пример использования testcontainers для кластера TQE](doc/008-tqe-cluster-example.md)
