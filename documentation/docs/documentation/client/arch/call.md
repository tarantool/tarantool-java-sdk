---
title: Хранимые процедуры
---

## Использование хранимых процедур

### Создание процедуры на узле Tarantool

```lua
function insert_person(id)
    return crud.insert('person', {id, true, 'hello'})
end
box.schema.func.create('insert_person')

function echo(in_value)
    return in_value
end
box.schema.func.create('echo')
```

Space `person` имеет следующий формат данных:

```lua
format = {
    { 'id', type = 'number' },
    { 'is_married', type = 'boolean', is_nullable = true },
    { 'name', type = 'string' },
    { 'bucket_id', 'unsigned' },
}
```

### Вызов хранимых процедур

Для того чтобы вызвать хранимую процедуру, воспользуйтесь `TarantoolClient` API:

```java
// ... создание клиента TarantoolCrudClient

final List<?> data = crudClient.call("insert_person", Arrays.asList(1)).join().get();
```

Хранимая процедура `insert_person` принимает один аргумент `id`. В процедуре выполняется вставка
записи с идентификатором `id`. Метод `crud.insert(...)` возвращает Go-like кортеж `result, err`,
где

```plaintext
// структура
result : {  
    rows: array<array<object>>,
    metadata: map<string, object>
}

// структура
err: {
    line: string,
    class_name: string,
    err: string,
    file: string,
    str: string
}
```

При первом вызове хранимой функции Java-клиент вернет следующий результат запроса:

```plaintext
data = [
    {
        rows=[
            Tuple(formatId = 55, data = [1, false, hello, 477], format = [])
        ], 
        metadata=[
            {name=id, type=number}, 
            {type=boolean, name=is_married, is_nullable=true}, 
            {name=name, type=string}, 
            {name=bucket_id, type=unsigned}
        ]
    }, // result
    null // error
]
```

При повторном вызове из-за дублирования идентификатора метод `crud.insert(...)` выдаст ошибку:

```plaintext
data = [
    null, // result
    { // error
        line=120, 
        class_name=InsertError, 
        
        err=Failed to insert: Duplicate key exists in unique index "pk" in space "person" with old \
        tuple - [1, false, "hello", 477] and new tuple - [1, false, "hello", 477],
        
        file=/app/.rocks/share/tarantool/crud/insert.lua,
        
        str=InsertError: Failed to insert: Duplicate key exists in unique index "pk" in space \
        "person" with old tuple - [1, false, "hello", 477] and new tuple - [1, false, "hello", 477]
    }
]
```

```java
// ... создание клиента TarantoolBoxClient
// Вызов хранимой процедуры с входным аргументом типа int
final List<?> data = crudClient.call("echo", Arrays.asList(1)).join().get();

// Вызов хранимой процедуры с входным аргументом типа String 
final List<?> data = crudClient.call("echo", Arrays.asList("hello")).join().get();

// Вызов хранимой процедуры с входным аргументом типа boolean
final List<?> data = crudClient.call("echo", Arrays.asList(true)).join().get();
```

Хранимая процедура `echo` принимает один аргумент `in_value`. В процедуре выполняется возврат
входного аргумента. При вызове хранимой функции Java-клиент вернет следующий результат запроса:

```plaintext
// Результат crudClient.call("echo", Arrays.asList(1)).join().get();
data = [1] // массив из одного int

// Результат crudClient.call("echo", Arrays.asList("hello")).join().get();
data = ["hello"] // массив из одного элемента типа String

// Результат crudClient.call("echo", Arrays.asList(true)).join().get();
data = [true] // массив из одного жлемента типа boolean
```

Хранимые процедуры могут принимать 0 или более входных и выходных аргументов. Преобразование
Java-типов в типы Tarantool производится с помощью Jackson сериализаторов/десериализаторов
([подробнее](./tuple_pojo_mapping.md)). Для того чтобы задать пользовательские преобразования типов,
обратитесь к [документации Jackson](https://github.com/FasterXML/jackson-databind/wiki). 
