---
title: Data Mapping
---

## Tarantool ⟷ Java POJO Field Mapping

Working with Plain Old Java Objects (POJO) in Tarantool is done using the Jackson library.

With Jackson, you can convert Java objects to JSON (serialization) and vice versa
(deserialization). You can also use Jackson extensions for other data serialization formats.

In `tarantool-java-sdk`, the Jackson library with an extension for working with `Msgpack` is used,
which allows working with Java entities using the Jackson library API.

The difference is that the output data type will not be JSON, but Msgpack. Msgpack is a
serialization format used in Tarantool for transmitting data between the client and Tarantool. In
the tarantool-java-sdk SDK, there is a module that serializes POJO using Jackson to Msgpack and
transmits them via the Tarantool protocol.

The following practices are described that will help you understand how to interact with Tarantool methods
using the Jackson library.

Let's consider the following examples of working with Tarantool and Jackson. These examples are not a detailed
guide, but will help you understand the principles of interaction. Mastering them will allow you to work
efficiently with the database in any scenarios, whether it's a cluster or a single instance, and with any POJO
objects.

## Efficient Mapping (Flatten input, Flatten output)

By default, field mapping in any of the clients (CrudClient, BoxClient), is performed in the most
efficient way — by the field's ordinal number.

This means that if the field order in Tarantool is:

```lua
person = box.schema.create_space('person')
person:create_index('pri')
person:format({
  {name = 'id', type = 'integer'},
  {name = 'is_married', type = 'boolean'},
  {name = 'name', type = 'string'},
})
```

The fields in the Java POJO should be arranged in the same order:

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

???+ note "Note"

    The table creation shown can be used on a single instance as well as on a cluster.
    But for correct vshard behavior in a cluster, you must add a `bucket_id` field with type 
    `unsigned` and an index on this field.

???+ warning "Important"

    `JsonFormat.Shape.ARRAY` ensures the conversion of POJO to an array and vice versa during 
    Jackson serialization/deserialization.

This approach is efficient because there are no overhead costs for working with the
`key-value` schema, and this is exactly how Tarantool transmits tuple data by default (without
keys) [via the protocol](https://www.tarantool.io/en/doc/latest/reference/internals/iproto/keys/#iproto-tuple).

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

???+ warning "Important"

    CONDITION may differ between CrudClient and BoxClient, so its initialization is hidden.

### Clarification About bucket_id

In this format, you can omit `bucket_id` if it is at the end of the tuple.

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

We get a tuple with `bucket_id` as a list or as a POJO:

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

Otherwise (when `bucket_id` is not at the end of the format).

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

`bucket_id` must be specified as a field in the POJO:

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

We get a tuple with `bucket_id` as a POJO, where the response will contain `bucket_id`:

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

???+ note "Note"

    For writing tuples to Tarantool, the same reading logic applies.

## Flexible Mapping Using Keys

You can also configure flexible mapping and work with keys in several ways. In these ways
we will use the same data schema on the Tarantool side that we used for efficient
mapping:

```lua
person = box.schema.create_space('person')
person:create_index('pri')
person:format({
  {name = 'id', type = 'integer'},
  {name = 'name', type = 'string'},
  {name = 'is_married', type = 'boolean'},
})
```

But the fields in the POJO will be different in order:

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

This is necessary to show that working with POJO happens as with an object[^1].

???+ warning "Important"

    `JsonFormat.Shape.ARRAY` is absent in the POJO declaration. Therefore, this POJO will be 
    interpreted as an object[^1] during Jackson serialization/deserialization.

### Writing POJO to Tarantool

Serialization is possible in several ways:

1. ```
    Unflatten input -- Serialization of POJO to Msgpack Map using Jackson -> transmission of Map to Tarantool -> using a write method that accepts Map as input
   ```
2. ```
    Flatten input method -- Conversion of POJO to Java List -> Serialization of Java List to Msgpack Array -> transmission of array to Tarantool -> using standard Tarantool write method
   ```

#### Unflatten input (POJO -> Msgpack Map)

???+ warning "Important"

    This writing method is only applicable to Crud API. Box API does not allow writing objects via 
    the IPROTO protocol. Therefore, when working directly with a storage instance, use the 
    following `Flatten input method`.

To serialize a POJO to a Msgpack Map using Jackson, use the following code:

```java

public class TestClass {

  @Test
  public void test() {
    client.space("person").insertObject(new UnorderedPerson(1, true, "artyom")).join();
  }
}
```

In the `tarantool-java-sdk` library set up to version `1.1.3` inclusive, you cannot use the `crud.[method_name]_object` methods with the native CrudClient.
To write an UnorderedPerson, you need to use workarounds.

You can use the client's call/eval methods and call the `crud.[method_name]_object` methods
directly.

```java
public class TestClass {

  @Test
  public void test() {
    client.call("crud.insert_object",
        Arrays.asList("person", new UnorderedPerson(1, true, "artyom"))).join();
  }
}
```

This will allow us to pass the POJO as an object [^1].

[^1]: The word "object" is used in the terminology of [Json](https://datatracker.ietf.org/doc/html/rfc8259)

#### Flatten input method (POJO -> Java List -> Msgpack Array)

Alternatively, you can add an additional method to the UnorderedPerson POJO,
which will return a flat tuple in the correct order, taking into account bucket_id.
For example, if bucket_id will be present in the table format like this:

```lua
person:format({
    { name = 'id', type = 'integer' },
    { name = 'name', type = 'string' },
    { name = 'bucket_id', type = 'unsigned', is_nullable = true },
    { name = 'is_married', type = 'boolean' },
})
```

Then the method in the POJO should look like this:

```java
public class UnorderedPerson {
  // fields... 

  public List<Object> asList() {
    return new ArrayList<>(Arrays.asList(id, name, null, isMarried));
  }
}
```

Then when calling the insert method, you can use this approach:

```java
public class TestClass {

  @Test
  public void test() {
    space.insert(new UnorderedPerson(2, true, "nikolay").asList()).join();
  }
}
```

This will allow passing the POJO as a tuple, while continuing to work with the UnorderedPerson object.

### Reading POJO from Tarantool

Deserialization is possible in several ways:

1. ```
   Flatten output method -- Using standard Tarantool read methods -> receiving Msgpack array \
                                                                                                               Converting array using data format to POJO format
                                              Getting the {field key -> field number} map in any way /
   ``` 
2. ```
   Unflatten output -- Receiving Msgpack Map -> converting Msgpack Map to POJO using Jackson
   ``` 

#### Unflatten output (Msgpack Map -> POJO)

???+ warning "Important"

    This reading method is only applicable to Crud API. Box API does not allow retrieving uncompressed 
    tuples via the IPROTO protocol. Therefore, when working directly with a storage instance, 
    use the `Flatten output method`.

In the `tarantool-java-sdk` library set up to version `1.1.3` inclusive, you cannot use the
`crud.unflatten_rows` method with the native CrudClient. Therefore, to read
UnorderedPerson, we will use a direct lua call through the eval method.

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

Tarantool provides several options for obtaining the format:

##### 1. Using SchemaFetcher

This option is preferred if you are connecting to a Tarantool instance that stores data (storage) and is not a router (router).  
If you are connecting to a storage, you use the Box API client (TarantoolBoxClient).

```java
TarantoolBoxClient client = TarantoolFactory.box().build();
```

By default, the Box API client uses the `tarantool-schema` module and its main class SchemaFetcher,
to get metadata from the system space. The client checks the schema version on each response from
Tarantool, and if the schema version is updated, it updates its local schema cache using
SchemaFetcher. Thus, you can reliably use this mapping without worrying that the java
client won't update the schema if the data schema is updated on the Tarantool side.

???+ note "Note"

    Optionally, you can disable SchemaFetcher. For example, if you use efficient mapping by 
    field number. Then you need to specify this option when creating the client
    ```java
    var clientWithoutFetcher = TarantoolFactory.box()
           .withFetchSchema(false)
           .build();
    ```

To get the table format, you need to get the space metadata using `SchemaFetcher`:

```java
TarantoolSchemaFetcher fetcher = client.getFetcher();
Space spaceInfo = fetcher.getSpace("person");
List<Field> tupleFormat = spaceInfo.getFormat();
```

Using the obtained format, you can perform mapping by key,
using the received from Tarantool Java List:

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

##### 2. Using tarantool/crud Response Metadata

More information about the tarantool/crud response structure can be found
here [github.com/tarantool/crud](https://github.com/tarantool/crud?tab=readme-ov-file#api).  
Create a TarantoolCrud client that is a proxy to the tarantool/crud module API.

```java
var client = TarantoolFactory.crud().build();
```

For better clarity, let's add bucket_id not at the end of the space format.

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

If you have a connector version that does not return tarantool/crud response metadata,  
you can call the tarantool/crud methods directly:

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

##### 3. From TarantoolResponse (Tarantool 3.x feature)

In a response where a tuple is returned, Tarantool allows getting the response format in a separate field
of the IPROTO packet. The difference from the previous variant is that the format is passed from another source.

???+ warning "Important"

    In `crud` version `1.7.1`, the functionality of returning the response format in a separate field of the IPROTO packet is not 
    supported.

Let's use a Box API client:

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
