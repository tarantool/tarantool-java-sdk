---
title: Обработка ошибок
---

## Обработка исключительных ситуаций

### Общие сведения

При работе с Java-клиентом можно встретиться со следующими категориями ошибок:

<table>
    <caption>Типы ошибок</caption>
    <thead>
        <tr>
            <th>Ошибка на стороне Tarantool</th>
            <th>Представление ошибки в Java-клиенте</th>
            <th>Описание</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><a href="#box-error-section">Исключительные ситуации</a>, возникающие 
при выполнении Lua-кода на сервере приложений Tarantool. Исключения выбрасываются через 
<a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/box_error/error/">
box.error(...)</a> или <a href="https://www.lua.org/pil/8.4.html">error(...)</a></td>
            <td>Ошибки передаются в Java-клиент на уровне протокола IProto с 
использованием специального 
<a href="https://www.tarantool.io/ru/doc/latest/reference/internals/iproto/format/#error-responses">
пакета</a>. В Java-клиенте такие ошибки оборачиваются в исключение типа <code>BoxError</code></td>
            <td>
                <p>Ошибку данного типа в Java-клиенте можно получить в следующих случаях:</p>
                <ul>
                    <li><a href="#box-error-section-box-api">Ошибки</a> во время использования 
<code>TarantoolBoxSpace API</code></li>
                    <li><a href="#call-error">Явный вызов</a> <code>box.error(...)</code> или 
<code>error(...)</code> при выполнении кода хранимой процедуры через <code>TarantoolClient#call(...)
</code></li>
                    <li><a href="#eval-error">Явный вызов</a> <code>box.error(...)</code> или 
<code>error(...)</code> при выполнении кода через <code>TarantoolClient#eval(...)</code></li>
                    <li>Иные ошибки, возникающие при работе с <code>TarantoolBoxClient API</code>, 
<code>TarantoolClient#eval(...)</code>, <code>TarantoolClient#call(...)</code>, описанные в 
<a href="https://github.com/tarantool/tarantool/blob/master/src/box/errcode.h">файле</a></li>
                </ul>
            </td>
        </tr>
        <tr>
            <td><a href="#go-like-error">Ошибки</a>, возвращаемые в виде структур в составе 
multivalue кортежа без выброса исключений (по аналогии с Go) на стороне Tarantool</td>
            <td>Ответ от Tarantool преобразуется по общим правилам десериализации</td>
            <td>Такая ситуация не рассматривается Java-клиентом как исключительная. 
Ответ с объектом-ошибкой преобразуется в Java-объекты по общим правилам десериализации. При работе 
с <code>TarantoolCrudClient</code> кортеж-ответ преобразуется в объект типа 
<code>CrudResponse</code>
            </td>
        </tr>
        <tr>
            <td>-</td>
            <td>Исключения формируются на стороне Java-клиента</td>
            <td>Данные исключения генерируются самим Java-клиентом в зависимости от 
различных ситуаций. Смотрите полный <a href="#java-exceptions-table">список исключений</a>
            </td>
        </tr>
    </tbody>
</table>

<h3 id="box-error-section">Исключительные ситуации на стороне Tarantool</h3>

<h4 id="box-error-section-box-api">TarantoolBoxClient API</h4>

При использовании `TarantoolBoxClient` можно получить исключение типа `BoxError` в случае
ошибочного использования API.

<h5 id="box-error-ex-1">Пример 1: повторная вставка записи с тем же идентификатором</h5>

Создадим space с именем `person`:

```lua
person_space = box.schema.space.create('person', {
    if_not_exists = true,
    format = {
        { 'id', type = 'uuid' },
        { 'value', type = 'string', is_nullable = true }
    }
})
person_space:create_index('pk', { parts = { 'id' } })
```

Создадим экземпляр `TarantoolBoxClient` и вставим запись (запись должна появиться в `Tarantool`):

```java
final TarantoolBoxClient boxClient = TarantoolFactory.box()
    .host("paste-host-name")
    .port(3302)
    .user("paste-username")
    .password("paste-username-password")
    .build();

final TarantoolBoxSpace space = boxClient.space("person");
final List<?> tuple = Arrays.asList(UUID.randomUUID(), "some_value");

final Tuple<List<?>> insertedTuple = space.insert(tuple);
```

Повторная вставка записи приведет к выбросу исключения `CompletionException` со ссылкой (caused by)
на `BoxError`:

```java
final List<?> tupleWithNewValue = Arrays.asList(tuple.get(0), "some_value_2");

// stacktrace: 
//    java.util.concurrent.CompletionException: io.tarantool.core.exceptions.BoxError:
//    BoxError{code=3, message='Duplicate key exists in unique index "pk" in space "tt" with 
//    old tuple - [bdf657fc-5779-46d6-aea6-402e4a5eee38, "some_value"] and new tuple -
//    [bdf657fc-5779-46d6-aea6-402e4a5eee38, "some_value"]', 
//    stack=[BoxErrorStackItem{type='ClientError', line=1133, file='./src/box/memtx_tree.cc',
//    message='Duplicate key exists in unique index "pk" in space "tt" with old tuple - 
//    [bdf657fc-5779-46d6-aea6-402e4a5eee38, "some_value"] and new tuple -
//    [bdf657fc-5779-46d6-aea6-402e4a5eee38, "some_value"]', errno=0, code=3, details=null}]}
final Tuple<List<?>> insertedTuple = space.insert(tuple).join();
```

<h5 id="box-error-ex-2">Пример 2: при удалении записи передан ключ с неверным типом</h5>

Будем использовать созданный в <a href="#box-error-ex-1">Примере 1</a> space `person`. Удалим
запись, передав неверный ключ (представлен в виде `string`):

```java
final TarantoolBoxSpace space = boxClient.space("person");

final String uuidAsString = tuple.get(0).toString();

// stacktrace: 
//    io.tarantool.core.exceptions.BoxError: BoxError{code=18, message='Supplied key
//    type of part 0 does not match index part type: expected uuid', 
//    stack=[BoxErrorStackItem{type='ClientError', line=850, file='./src/box/key_def.h',
//    message='Supplied key type of part 0 does not match index part type: expected uuid', 
//    errno=0, code=18, details=null}]}
final Tuple<List<?>> deletedTuple = space.delete(uuidAsString).join();
```

???+ warning "Важно"

    Чтобы узнать больше про модуль box, обратитесь к 
    [документации](https://www.tarantool.io/ru/doc/latest/reference/reference_lua/box/)

<h4 id="call-error">Исключения на стороне Tarantool при вызове <code>TarantoolClient#call(...)
</code></h4>

Создадим хранимые процедуры, в которых проверяется тип переданного параметра:

```lua
function check_parameter_with_error(param)
    if type(param) ~= "string" then
        error("Parameter must be a string")
    end
    return "Parameter is valid"
end

box.schema.func.create('check_parameter_with_error')
```

```lua
function check_parameter_with_box_error(param)
    if type(param) ~= "string" then
        box.error(box.error.PROC_LUA, "Parameter must be a string")
    end
    return "Parameter is valid"
end

box.schema.func.create('check_parameter_with_box_error')
```

Вызовем хранимые процедуры из Java:

```java
import java.util.Collections;
import java.util.List;
import java.util.UUID;

final List<String> stringParam = Collections.singletonList("some_value");

// OK 
// TarantoolResponse(data = [Parameter is valid], formats = {})
final TarantoolResponse<List<?>> result =
    boxClient.call("check_parameter_with_error", stringParam).join();

final List<UUID> param = Collections.singletonList(UUID.randomUUID());

// Stacktrace:
//    java.util.concurrent.CompletionException: io.tarantool.core.exceptions.BoxError: 
//    BoxError{code=32, message='Parameter must be a string', 
//    stack=[BoxErrorStackItem{type='ClientError', line=3, 
//    file='[string "function check_parameter_with_box_error(param..."]', 
//    message='Parameter must be a string', errno=0, code=32, details=null}]}
final TarantoolResponse<List<?>> exceptionallyResult =
    boxClient.call("check_parameter_with_error", param).join();
```

```java
import java.util.Collections;
import java.util.List;
import java.util.UUID;

final List<String> stringParam = Collections.singletonList("some_value");

// OK
// TarantoolResponse(data = [Parameter is valid], formats = {})
final TarantoolResponse<List<?>> result =
    boxClient.call("check_parameter_with_box_error", stringParam).join();

final List<UUID> param = Collections.singletonList(UUID.randomUUID());

// Stacktrace: 
//    io.tarantool.core.exceptions.BoxError: BoxError{code=32, message='Parameter must be a 
//    string', stack=[BoxErrorStackItem{type='ClientError', line=3, 
//    file='[string "function check_parameter_with_box_error(param..."]', 
//    message='Parameter must be a string', errno=0, code=32, details=null}]}
final TarantoolResponse<List<?>> exceptionallyResult =
    boxClient.call("check_parameter_with_box_error", param).join();
```

<h4 id="eval-error">Исключения на стороне Tarantool при вызове <code>TarantoolClient#eval(...)
</code></h4>

Выполним следующий Lua код через Java-клиент:

```lua
if not box.space[space_name] then 
    box.error(box.error.NO_SUCH_SPACE, string.format("Space does not exist: %s", space_name)) 
end
return string.format("Space '%s' exists", space_name)
```

```java
final String luaCode = "if not box.space[space_name] then "
    + "box.error(box.error.NO_SUCH_SPACE, string.format(\"Space does not exist: %s\", space_name)) "
    + "end return string.format(\"Space '%s' exists\", space_name)";

// OK
// TarantoolResponse(data = [Space 'person' exists], formats = {})
final TarantoolResponse<List<?>> result = client.eval(luaCode, "person").join();

// Stacktrace:
//    java.util.concurrent.CompletionException: io.tarantool.core.exceptions.BoxError: 
//    BoxError{code=36, message='Space 'Space does not exist: unknown_space' does not exist', 
//    stack=[BoxErrorStackItem{type='ClientError', line=1, file='eval', message='Space 'Space does 
//    not exist: unknown_space' does not exist', errno=0, code=36, details=null}]}
final TarantoolResponse<List<?>> exceptionallyResult = client.eval(luaCode, "person").join();
```

<h3 id="go-like-error">Go-like ошибки</h3>
<h4 id="go-like-error-crud">TarantoolCrudClient</h4>

`TarantoolCrudClient` является оберткой над API модуля [crud](https://github.com/tarantool/crud).
Большинство методов API возвращают результат в виде кортежа (res, err), где err - объект
[ошибки](https://www.tarantool.io/en/doc/latest/reference/reference_capi/error/). В Java-клиенте
результат вызова метода crud API оборачивается в объект типа `CrudResponse`. С точки
зрения протокола IProto, возврат кортежа с объектом-ошибкой не является исключительной ситуацией,
но в случае ошибки (err != nil), `TarantoolCrudClient` самостоятельно выбрасывает исключение типа
`CrudError` (по аналогии с`TarantoolBoxClient`, который выбрасывает `BoxError`) для сигнализации,
что был возвращен объект ошибки.

В качестве примера создадим на vshard шардированном кластере space `person` со следующим форматом:

```lua
format = {
    { 'id', type = 'uuid' },
    { 'value', type = 'string', is_nullable = true },
    { 'bucket_id', 'unsigned' },
}
```

Добавим записи в кластер:

```java
import java.util.Arrays;
import java.util.UUID;

final TarantoolCrudClient crudClient = TarantoolFactory.crud()
    .host("router-hostname")
    .port(3301)
    .user("username")
    .password("username-password")
    .build();

final TarantoolCrudSpace space = crudClient.space("person");

final List<?> tuple = Arrays.asList(UUID.randomUUID(), "some_value");

// OK
//    Tuple(
//      formatId = 55, 
//      data = [ca95b6c7-134d-476a-8545-1ce038cb3b18, some_value, 1208], 
//      format = [
//        Field{
//          name='id', 
//          type='uuid', 
//          isNullable=null, 
//          collation='null', 
//          constraint=null, 
//          foreignKey=null
//        }, 
//        Field{
//          name='value', 
//          type='string', 
//          isNullable=null, 
//          collation='null', 
//          constraint=null, 
//          foreignKey=null
//        }, 
//      Field{
//        name='bucket_id', 
//        type='unsigned', 
//        isNullable=null, 
//        collation='null', 
//        constraint=null, 
//        foreignKey=null
//      }
//    ]
//   )
final Tuple<List<?>> insertedTuple = space.insert(tuple).join();

final List<?> tupleWithSameId = Arrays.asList(tuple.get(0), "second_value");

// Stacktrace:
//    java.util.concurrent.CompletionException: io.tarantool.mapping.crud.CrudException: 
//    InsertError: Failed to insert: Duplicate key exists in unique index "pk" in space "person" 
//    with old tuple - [ca95b6c7-134d-476a-8545-1ce038cb3b18, "some_value", 1208] and 
//    new tuple - [ca95b6c7-134d-476a-8545-1ce038cb3b18, "second_value", 1208]
final Tuple<List<?>> exceptionallyTuple = space.insert(tupleWithSameId).join();
```

<h4 id="go-like-error-call">Возврат Go-like ошибок через хранимые процедуры</h4>

Tarantool допускает возврат Go-like кортежей в возвращаемом значении хранимых процедур. В этом
случае пользователь должен позаботиться о правильности преобразования lua и Java типов, основываясь
на правилах [преобразования](tuple_pojo_mapping.md).

Рассмотрим следующую процедуру:

```lua
function multiply_by_100(input)
    if type(input) ~= "number" then
        return nil, "Input is not a number"
    end
    local result = input * 100
    return result, nil
end

box.schema.func.create('multiply_by_100')
```

```java
final TarantoolClient client = TarantoolFactory.box()
    .host("hostname")
    .port(3301)
    .user("username")
    .password("username-password")
    .build();

// OK
// TarantoolResponse(data = [10000, null], formats = {})
final TarantoolResponse<List<?>> result = client.call("multiply_by_100", Arrays.asList(100)).join();

// OK
// TarantoolResponse(data = [null, Input is not a number], formats = {})
final TarantoolResponse<List<?>> resultWithError =
    client.call("multiply_by_100", Arrays.asList("100")).join();
```

В качестве возвращаемых значений могут выступать различные lua-типы, например, массивы:

```lua
function prime_factors(input)
    if type(input) ~= "number" or input < 2 then
        return nil, "Input is not a valid number (must be an integer >= 2)"
    end

    local factors = {}
    local divisor = 2

    while input >= divisor do
        while (input % divisor) == 0 do
            table.insert(factors, divisor)
            input = input / divisor
        end
        divisor = divisor + 1
    end

    return factors, nil
end

box.schema.func.create('prime_factors')
```

```java
import java.util.Arrays;

// OK
// TarantoolResponse(data = [[2, 2, 5, 5], null], formats = {})
final TarantoolResponse<List<?>> result = client.call("prime_factors", Arrays.asList(100)).join();

    // OK
// TarantoolResponse(data = [null, Input is not a valid number (must be an integer >= 2)], 
// formats = {})
    final TarantoolResponse<List<?>> result =
        client.call("prime_factors", Arrays.asList(100)).join();
```

<h3 id="java-exceptions">Исключения, генерируемые Java-клиентом</h3>

<table id="java-exceptions-table">
    <caption>Типы исключений</caption>
    <thead>
        <tr>
            <th>Тип исключения</th>
            <th>Описание</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>BadGreetingException</td>
            <td>Выбрасывается в случае неудачного приветствия с узлом Tarantool</td>
        </tr>
        <tr>
            <td>BalancerException</td>
            <td>Базовое исключение, выбрасываемое при неправильной работе 
балансировщика</td>
        </tr>
        <tr>
            <td>NoAvailableClientsException</td>
            <td>Наследник BalancerException. Выбрасывается в случае когда балансировщик 
не смог найти ни одного "живого" соединения в пуле соединений</td>
        </tr>
        <tr>
            <td>SchemaFetchingException</td>
            <td>Наследник ClientException. Выбрасывается, когда во время загрузки схем 
спейсов произошла какая-либо ошибка</td>
        </tr>
        <tr>
            <td>ConnectionException</td>
            <td>Базовое исключение для исключений, связанных с низкоуровневым 
соединением (интерфейс Connection)</td>
        </tr>
        <tr>
            <td>ConnectionClosedException</td>
            <td>Наследник ConnectionException. Выбрасывается при аварийном закрытии 
соединения со стороны узла Tarantool или со стороны Java-клиента, в момент, когда соединение к узлу 
инициировано, но было прервано до момента фактического подключения</td>
        </tr>
        <tr>
            <td>CrudException</td>
            <td>Базовый класс исключений при работе с TarantoolCrudSpace API. 
Выбрасывается при ошибочной работе с TarantoolCrudSpace API (например, вставка записи с одинаковым 
ключом)</td>
        </tr>
        <tr>
            <td>ServerException</td>
            <td>Базовый класс исключений для ошибок сервера, таких как устаревшая 
версия протокола</td>
        </tr>
        <tr>
            <td>JacksonMappingException</td>
            <td>Базовый класс исключений для ошибок сериализации и десериализации 
типов</td>
        </tr>
        <tr>
            <td>NoSchemaException</td>
            <td>Наследник ClientException. Выбрасывается, когда при вызове 
<code>TarantoolBoxClient#space(...)</code> space с переданным идентификатором или именем не 
существует</td>
        </tr>
        <tr>
            <td>ShutdownException</td>
            <td>Наследник ClientException. Класс исключений клиентов, таких как ошибки 
изящного завершения</td>
        </tr>
        <tr>
            <td>PoolException</td>
            <td>Базовый класс исключений, возникающих при работе пула соединений</td>
        </tr>
        <tr>
            <td>PoolClosedException</td>
            <td>Наследник PoolException. Выбрасывается, при попытке выполнить запросы 
на прежде закрытом Java-клиенте</td>
        </tr>
        <tr>
            <td>TimeoutException</td>
            <td>Выбрасывается, когда действие клиента превышает заданное время 
(подключение, приветствие, тайм-аут запроса)</td>
        </tr>
    </tbody>
</table>
