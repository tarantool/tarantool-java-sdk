---
title: Stored Procedures
---

## Using Stored Procedures

### Creating a Procedure on the Tarantool Node

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

The `person` space has the following data format:

```lua
format = {
    { 'id', type = 'number' },
    { 'is_married', type = 'boolean', is_nullable = true },
    { 'name', type = 'string' },
    { 'bucket_id', 'unsigned' },
}
```

### Calling Stored Procedures

To call a stored procedure, use the `TarantoolClient` API:

```java
// ... creating TarantoolCrudClient client

final List<?> data = crudClient.call("insert_person", Arrays.asList(1)).join().get();
```

The stored procedure `insert_person` accepts one argument `id`. In the procedure, a record with the identifier `id` is inserted. The `crud.insert(...)` method returns a Go-like tuple `result, err`, where

```plaintext
// structure
result : {  
    rows: array<array<object>>,
    metadata: map<string, object>
}

// structure
err: {
    line: string,
    class_name: string,
    err: string,
    file: string,
    str: string
}
```

On the first call to the stored function, the Java client will return the following query result:

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

On a repeated call due to identifier duplication, the `crud.insert(...)` method will return an error:

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
// ... creating TarantoolBoxClient
// Calling a stored procedure with an input argument of type int
final List<?> data = crudClient.call("echo", Arrays.asList(1)).join().get();

// Calling a stored procedure with an input argument of type String 
final List<?> data = crudClient.call("echo", Arrays.asList("hello")).join().get();

// Calling a stored procedure with an input argument of type boolean
final List<?> data = crudClient.call("echo", Arrays.asList(true)).join().get();
```

The stored procedure `echo` accepts one argument `in_value`. In the procedure, the input argument is returned. When calling the stored function, the Java client will return the following query result:

```plaintext
// Result of crudClient.call("echo", Arrays.asList(1)).join().get();
data = [1] // array with one int element

// Result of crudClient.call("echo", Arrays.asList("hello")).join().get();
data = ["hello"] // array with one String element

// Result of crudClient.call("echo", Arrays.asList(true)).join().get();
data = [true] // array with one boolean element
```

Stored procedures can accept 0 or more input and output arguments. Conversion
of Java types to Tarantool types is performed using Jackson serializers/deserializers
([more details](./tuple_pojo_mapping.md)). To specify custom type conversions,
refer to the [Jackson documentation](https://github.com/FasterXML/jackson-databind/wiki).
