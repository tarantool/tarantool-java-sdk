---
uid: high-client-get-started
---

# Быстрый старт

Запустите пустой `Tarantool`:

```bash
docker run -p 3301:3301 -it tarantool/tarantool:3.2
```

Подключимся к Tarantool из java:

```java
// imports

public class TestClass {

  @Test
  void test() {
    final var client = TarantoolFactory.box().build();
    // проверим, что мы подключились успешно и вернем версию Tarantool, к которому подключились
    // должно вернуться "TarantoolResponse(data = [3.2.1-0-g219c4de1a88], formats = {})"
    client.eval("return _TARANTOOL").join();
  }
}
```

:::
Создадим [space](https://www.tarantool.io/en/doc/latest/platform/ddl_dml/value_store/#spaces)
`person`, в который будем писать данные. Этот `space` создается без формата, и на него создается
первичный ключ (pk) по умолчанию, где первое поле имеет тип `unsigned`. Такой `space` может хранить
любые данные, при условии, что первое поле имеет тип `unsigned`, то есть это
бессхемное хранение с требованием к первичному ключу:

```java
// imports

public class TestClass {

  @Test
  void test() {
    final var client = TarantoolFactory.box().build();
    final var result = client.eval("return box.schema.create_space('person'):create_index('pk')")
        .join();
  }
}
```

> [!ЗАМЕТКА]
>
> Чтобы задать более строгую схему хранения данных, можно задать формат:
>
>   ```java
>   // imports
>
>   public class TestClass {
>     
>     @Test
>     void test() {
>       final var client = TarantoolFactory.box().build();
>       final var result = client.eval("return box.space.person:format({ {'key', 'integer'}, {'value', 'string'} })").join();
>     }
>   }
>   ```
:::

:::
В предыдущих вызовах мы использовали метод `eval`, который доступен как для `Box API` клиента, так и
для `Crud API` клиента. Теперь, после создания `space`, мы можем воспользоваться методами `Box API`
для работы с данными. `Box API` используется для работы с отдельным экземпляром `Tarantool`. Даже в
случае кластера, `Box API` может быть применен для подключения к отдельным узлам.
:::

```java
// imports

public class TestClass {

  @Test
  void test() {
    final var client = TarantoolFactory.box().build();

    // получим спейс person
    final var space = client.space("person");

    // запишем один кортеж
    final var insertedPerson = space.insert(Arrays.asList(1, "Hello World")).join();

    // получим записанный кортеж
    final var selectedPerson = space.select(1).join();
  }
}
```
