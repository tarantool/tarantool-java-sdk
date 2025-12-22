---
title: Маппинг данных
---

## Маппинг полей Tarantool <-> Java POJO

Работа с Plain Old Java Objects (POJO) в Tarantool происходит с помощью библиотеки   
Jackson.

С помощью Jackson можно преобразовывать Java объекты в JSON (сериализация) и наоборот
(десериализация). Также можно использовать расширения Jackson для других форматов сериализация
данных.

В `tarantool-java-sdk` используется библиотека Jackson с расширением для работы с `Msgpack`, которая
позволяет работать с Java сущностями используя API библиотеки Jackson.

Отличие же заключается в том, что выходной тип данных будет не JSON, а Msgpack. Msgpack - формат
сериализации, используемый в Tarantool для передачи данных между клиентом и Tarantool. В
tarantool-java-sdk SDK есть модуль, который будет сериализовывать POJO с помощью Jackson в Msgpack и
передавать их по протоколу Tarantool.

Далее описываются практики, которые позволят понять как взаимодействовать с методами Tarantool c
помощью библиотеки Jackson.

Рассмотрим следующие примеры работы с Tarantool и Jackson. Эти примеры не являются подробным
руководством, но помогут вам понять принципы взаимодействия. Освоив их, вы сможете эффективно
работать с базой данных в любых сценариях, будь то кластер или одиночный экземпляр, и с любыми POJO
объектами.

## Эффективный Маппинг (Flatten input, Flatten output)

По умолчанию маппинг полей в любом из клиентов(CrudClient, BoxClient), выполняется наиболее
эффективным путём — по порядковому номеру поля.

Это означает, что если порядок полей в Tarantool такой:

```lua
person = box.schema.create_space('person')
person:create_index('pri')
person:format({
  {name = 'id', type = 'integer'},
  {name = 'is_married', type = 'boolean'},
  {name = 'name', type = 'string'},
})
```

Поля в Java POJO должны располагаться в таком же порядке:

```java

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonIgnoreProperties(ignoreUnknown = true) // for example bucket_id
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Person {

  public Integer id;
  public Boolean isMarried;
  public String name;
}
```

???+ note "Заметка"

    Приведенное создание таблицы можно использовать как на одиночном экземпляре, так и на кластере.
    Но для корректного поведения vshard в кластере необходимо добавить поле `bucket_id` с типом 
    `unsigned` и индекс на это поле.

???+ warning "Важно"

    `JsonFormat.Shape.ARRAY` обеспечивает конвертацию POJO в массив и наоборот при 
    сериализации/десериализации jackson.

Такой подход эффективен, поскольку отсутствуют накладные расходы на работу со схемой
`ключ-значение`, и именно так Tarantool передает данные кортежей по умолчанию (без
ключей) [по протоколу](https://www.tarantool.io/en/doc/latest/reference/internals/iproto/keys/#iproto-tuple).

```java

public class TestClass {

  @Test
  public void test() {
    var space = client.space("person");
    Person insertedTuple = space.insert(new Person(1, true, "Artyom"), Person.class).join().get();
    // ... [Tuple(..., data = Person{id=1, isMarried=true, name='Artyom'})] ...
    space.select(CONDITION, Person.class).join();
  }
}
```

???+ warning "Важно"

    CONDITION может отличаться у CrudClient и BoxClient, поэтому его инициализация скрыта.

### Уточнение про bucket_id

В этом формате можно опускать `bucket_id`, если он находится в конце кортежа.

```lua
person:

format( {
  {
    name = 'id', type = 'integer'
  },
  {
    name = 'is_married', type = 'boolean'
  },
  {
    name = 'name', type = 'string'
  },
  {
    name = 'bucket_id', type = 'unsigned', is_nullable = true
  },
})
```

Получим кортеж с `bucket_id` как список или как POJO:

```java
public class TestClass {

  @Test
  public void test() {
    // ... [Tuple(..., data = [1, true, Artyom, 123])] ...
    space.select(CONDITION).join();

    // ... [Tuple(..., data = Person{id=1, isMar ... e='Artyom'})] ...
    space.select(CONDITION, Person.class).join();
  }
}
```

В противном случае(когда `bucket_id` не находится в конце формата).

```lua
person:drop()
person = box.schema.create_space('person')
person:create_index('pri')
person:format({
  {name = 'id', type = 'integer'},
  {name = 'is_married', type = 'boolean'},
  {name = 'bucket_id', type = 'unsigned', is_nullable = true},
  {name = 'name', type = 'string'},
})
```

`bucket_id` необходимо указать, как поле в POJO:

```java

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonIgnoreProperties(ignoreUnknown = true) // for example bucket_id
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Person {

  public Integer id;
  public Boolean isMarried;
  public Integer bucketId;
  public String name;
}
```

Получим кортеж с `bucket_id` как POJO, где в ответе будет `bucket_id`:

```java
public class TestClass {

  @Test
  public void test() {
    var space = client.space("person");

    // ...[
    //   Tuple(...,
    //     data = Person{
    //                    id=1,
    //                    isMarried=true,
    //                    bucketId=123,
    //                    name='Artyom'
    //                  }
    //   )
    // ] ...
    space.select(CONDITION, Person.class).join().get();
  }
}
```

???+ note "Заметка"

    На запись кортежа в Tarantool действует аналогичная чтению кортежа логика.

## Гибкий маппинг с использованием ключей

Вы также можете настроить гибкий маппинг и работать с ключами несколькими способами. В этих способах
мы будем использовать ту же схему данных на стороне Tarantool, что использовали и для эффективного
маппинга:

```lua
person = box.schema.create_space('person')
person:create_index('pri')
person:format({
  {name = 'id', type = 'integer'},
  {name = 'name', type = 'string'},
  {name = 'is_married', type = 'boolean'},
})
```

Но поля в POJO у нас будут отличаться по порядку:

```java

@JsonIgnoreProperties(ignoreUnknown = true) // for example bucket_id
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder(toBuilder = true)
public class UnorderedPerson {

  public String name;
  @JsonProperty("is_married")
  public Boolean isMarried;
  public Integer id;

  public Person(
      @JsonProperty("is_married") Boolean isMarried,
      @JsonProperty("id") Integer id,
      @JsonProperty("name") String name) {
    this.id = id;
    this.isMarried = isMarried;
    this.name = name;
  }
}
```

Это необходимо, чтобы показать что работа с POJO происходит, как с объектом[^1].

???+ warning "Важно"

    `JsonFormat.Shape.ARRAY` отсутствует в декларации POJO. Поэтому данный POJO будет 
    интерпретироваться как объект[^1] в сериализации/десериализации jackson.

### Запись POJO в Tarantool

Сериализация возможна несколькими способами:

1. ```
    Unflatten input -- Сериализация POJO в Msgpack Map с помощью Jackson -> передача Map в Tarantool -> использование метода записи, который принимает Map как input
   ```
2. ```
    Flatten input method -- Конвертация POJO в Java List -> Сериализация Java List в Msgpack Array -> передача массива в Tarantool -> использование стандартного метода записи Tarantool
   ```

#### Unflatten input (POJO -> Msgpack Map)

???+ warning "Важно"

    Этот способ записи актуален только для Crud API. Box API не позволяет записывать объекты по 
    протоколу IPROTO. Поэтому при работе с экземпляром хранилища (storage) напрямую применяйте 
    следующий `Flatten input method`.

Для сериализации POJO в Msgpack Map с помощью Jackson используйте следующий код:

```java

public class TestClass {

  @Test
  public void test() {
    client.space("person").insertObject(new UnorderedPerson(1, true, "artyom")).join();
  }
}
```

В наборе библиотек `tarantool-java-sdk` до версии `1.1.3` включительно нельзя использовать методы
`crud.[method_name]_object` с помощью нативного CrudClient.
Чтобы записать UnorderedPerson, необходимо воспользоваться обходными путями.

Можно воспользоваться call/eval методами клиента и вызвать `crud.[method_name]_object` методы
напрямую.

```java
public class TestClass {

  @Test
  public void test() {
    client.call("crud.insert_object",
        Arrays.asList("person", new UnorderedPerson(1, true, "artyom"))).join();
  }
}
```

Это нам позволит передать POJO как объект [^1].

[^1]: Слово объект используется в терминах [Json](https://datatracker.ietf.org/doc/html/rfc8259)

#### Flatten input method (POJO -> Java List -> Msgpack Array)

Альтернативно можно добавить дополнительный метод в POJO UnorderedPerson,
который будет возвращать плоский кортеж в правильном порядке с учетом bucket_id.
Например, если bucket_id будет присутствовать в формате таблицы так:

```lua
person:format({
    { name = 'id', type = 'integer' },
    { name = 'name', type = 'string' },
    { name = 'bucket_id', type = 'unsigned', is_nullable = true },
    { name = 'is_married', type = 'boolean' },
})
```

То метод в POJO должен выглядит так:

```java
public class UnorderedPerson {
  // fields... 

  public List<Object> asList() {
    return new ArrayList<>(Arrays.asList(id, name, null, isMarried));
  }
}
```

Тогда при вызове метода insert можно будет использовать этот способ:

```java
public class TestClass {

  @Test
  public void test() {
    space.insert(new UnorderedPerson(2, true, "nikolay").asList()).join();
  }
}
```

Что позволит передавать POJO как кортеж, при этом, продолжая, работать с объектом UnorderedPerson.

### Чтения POJO из Tarantool

Десериалиация возможна несколькими способами:

1. ```
   Flatten output method -- Использование стандартных методов чтения Tarantool -> получение Msgpack массива \
                                                                                                               Конвертация массива с использования формата данных в POJO формат
                                              Получение карты {ключ поля -> номер поля} каким либо способом /
   ``` 
2. ```
   Unflatten output -- Получение Msgpack Map -> конвертация Msgpack Map в POJO с использованием Jackson
   ``` 

#### Unflatten output (Msgpack Map -> POJO)

???+ warning "Важно"

    Данный способ записи актуален только для Crud API. Box API не позволяет получать несжатые 
    кортежи по протоколу IPROTO. Поэтому при работе с экземпляром хранилища (storage) напрямую 
    применяйте метод `Flatten output method`.

В наборе библиотек `tarantool-java-sdk` до версии `1.1.3` включительно нельзя использовать метод
`crud.unflatten_rows` с помощью нативного CrudClient. Следовательно, чтобы прочитать
UnorderedPerson, воспользуемся вызовом lua напрямую через eval метод.

```java
List<UnorderedPerson> persons = routerClient.eval("""
          res, err = crud.select(...)
          return crud.unflatten_rows(res.rows, res.metadata)
        """,
    Arrays.asList(
        "person",
        Arrays.asList(Arrays.asList("==", "pk", 1))
    ),
    new TypeReference<List<List<PersonWithDifferentFieldsOrder>>>() {}
).thenApply(
    tarantoolResponse -> tarantoolResponse.get()  // unwrap TarantoolResponse
        .get(0) // get first object from multi return 
).join();
```

#### Flatten output method (Msgpack Array -> use format -> POJO)

Tarantool предоставляет несколько возможностей для получения формата:

##### 1. Используя SchemaFetcher

Этот вариант предпочтителен, если вы подключаетесь к инстансу Tarantool,  
который хранит данные (storage) и не является роутером (router).  
Если вы подключаетесь к хранилищу, то вы используете Box API клиент (TarantoolBoxClient).

```java
TarantoolBoxClient client = TarantoolFactory.box().build();
```

По умолчанию Box API клиент использует модуль `tarantool-schema` и его основной класс SchemaFetcher,
для получения метаданных из служебного спейса. Клиент проверяет версию схемы на каждом ответе от
Tarantool, и если версия схемы обновилась, то он обновляет свой локальный кеш со схемой с помощью
SchemaFetcher. Таким образом вы можете надежно использовать этот маппинг, не опасаясь, что java
клиент не обновит схему, если схема данных обновилась на стороне Tarantool.

???+ note "Заметка"

    При желании можно отключить SchemaFetcher. Например если вы используете эффективный маппинг по 
    номеру поля. Тогда вам необходимо указать данную опцию при создании клиента
    ```java
    var clientWithoutFetcher = TarantoolFactory.box()
           .withFetchSchema(false)
           .build();
    ```

Для получения формата таблицы, необходимо получить метаданные space с помощью `SchemaFetcher`:

```java
TarantoolSchemaFetcher fetcher = client.getFetcher();
Space spaceInfo = fetcher.getSpace("person");
List<Field> tupleFormat = spaceInfo.getFormat();
```

С помощью полученного формата можно осуществить маппинг по ключу,
используя полученные от Tarantool Java List:

```java
public class TestClass {

  @Test
  public void test() {
    space.select(Arrays.asList(1)).thenApply(
        list -> {
          var result = new ArrayList<>();

          for (var t : list.get()) {        // unwrap tuple struct from select response struct
            List<?> dataList = t.get();     // unwrap data from tuple struct
            Map<String, ?> map = IntStream  // create map {key -> value}
                .range(0, dataList.size())
                .boxed()
                .collect(
                    Collectors.toMap(
                        (i) -> tupleFormat.get(i).getName(),
                        dataList::get
                    )
                );
            // use jackson mapper to map from Map to Person POJO
            // import static io.tarantool.mapping.BaseTarantoolJacksonMapping.objectMapper; 
            UnorderedPerson person = objectMapper.convertValue(map, UnorderedPerson.class);

            result.add(person);
          }

          return result;
        }
    ).join();
// [UnorderedPerson{name='artyom', isMarried=true, id=1}]
  }
}
```

##### 2. Используя метаданные ответа tarantool/crud

Подробнее про структуру ответа tarantool/crud можно найти
здесь [github.com/tarantool/crud](https://github.com/tarantool/crud?tab=readme-ov-file#api).  
Cоздадим клиент TarantoolCrud, который является прокси к API модуля tarantool/crud.

```java
var client = TarantoolFactory.crud().build();
```

Для большей наглядности добавим bucket_id не в конец формата спейса.

```lua
person:drop()
person = box.schema.create_space('person')
person:create_index('pri')
person:format({
    { name = 'id', type = 'integer' },
    { name = 'name', type = 'string' },
    { name = 'bucket_id', type = 'unsigned', is_nullable = true },
    { name = 'is_married', type = 'boolean' },
})
```

```java
var space = client.space("person");
```

Если у вас версия коннектора, которая не выдает метаданные ответа tarantool/crud,  
то можно вызывать методы tarantool/crud напрямую:

```java
public class TestClass {

  @Test
  public void test() {
    client.call(
        "crud.select",
        Arrays.asList(
            "person",
            Arrays.asList(Arrays.asList("==", "pk", 1))
        ),
        new TypeReference<CrudResponse<List<List<?>>>>() {}
    ).thenApply(
        tarantoolResponse -> {
          var result = new ArrayList<>();

          CrudResponse<List<List<?>>> crudResponse = tarantoolResponse.get(); // unwrap tuple struct from select response struct

          List<io.tarantool.mapping.Field> metadata = crudResponse.getMetadata(); // get metadata from crud response
          List<List<?>> tuples = crudResponse.getRows();                         // get flatten tuples from crud response

          if (tuples == null) {
            return result;
          }

          for (List<?> tuple : tuples) {
            Map<Object, ?> map = IntStream // create map {key -> value}
                .range(0, tuple.size())
                .boxed()
                .collect(
                    Collectors.toMap(
                        (i) -> metadata.get(i).getName(),
                        tuple::get
                    )
                );

            // use jackson mapper to map from Map to Person POJO
            // import static io.tarantool.mapping.BaseTarantoolJacksonMapping.objectMapper; 
            UnorderedPerson person = objectMapper.convertValue(map, UnorderedPerson.class);

            result.add(person);
          }

          return result;
        }
    ).join();
  }
}
```

##### 3. Из TarantoolResponse (Tarantool 3.x feature)

В ответе, где возвращается кортеж, Tarantool позволяет получать и формат ответа в отдельном поле
IPROTO пакета. Отличие от предыдущего варианта в том, что формат передается из другого источника.

???+ warning "Важно"

    В `crud` версии `1.7.1` функционал возврата формата ответа в отдельном поле IPROTO пакета не 
    поддерживается.

Воспользуемся Box API клиентом:

```java
public class TestClass {
  
  @Test
  public void test() {
    
    // other code 
    
    space.select(Arrays.asList(1))
        .thenApply(
            selectResponse -> {
              var result = new ArrayList<>();

              List<Tuple<List<?>>> tuples = selectResponse.get();

              if (tuples == null) {
                return result;
              }

              for (Tuple<List<?>> tuple : tuples) {
                List<io.tarantool.mapping.Field> format = tuple.getFormat();
                List<?> data = tuple.get();
                Map<Object, ?> map = IntStream
                    .range(0, data.size())
                    .boxed()
                    .collect(
                        Collectors.toMap(
                            (i) -> format.get(i).getName(),
                            data::get
                        )
                    );
                result.add(
                    objectMapper.convertValue(map, PersonWithDifferentFieldsOrder.class)
                );
              }

              return result;
            }
        ).join();
  }
}
```
