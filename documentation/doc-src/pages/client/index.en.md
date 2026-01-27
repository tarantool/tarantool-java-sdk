---
title: High-Level Client
hide:
  - toc
---

This section describes the components of the high-level client and examples of its usage.

## Quick Start

* Start a blank `Tarantool`:

```bash
docker run -p 3301:3301 -it tarantool/tarantool:3.2
```

Connect to Tarantool from Java:

```java
public class TestClass {

  @Test
  public void test() {
    final TarantoolBoxClient client = TarantoolFactory.box().build();
    // check that we connected successfully and return the Tarantool version we connected to
    // should return "TarantoolResponse(data = [3.2.1-0-g219c4de1a88], formats = {})"
    client.eval("return _TARANTOOL").join();
  }
}
```

Create a [space](https://www.tarantool.io/en/doc/latest/platform/ddl_dml/value_store/#spaces)
`person` to which we will write data. This space is created without a format, and a primary key (pk) 
index is created by default, where the first field has the `unsigned` type. Such a space can store
any data, provided that the first field has the unsigned type, i.e. schemaless storage with a 
requirement for the primary key:

```java
public class TestClass {

  @Test
  public void test() {
    final TarantoolBoxClient client = TarantoolFactory.box().build();
    // check that we connected successfully and return the Tarantool version we connected to
    // should return "TarantoolResponse(data = [3.2.1-0-g219c4de1a88], formats = {})"
    client.eval("return _TARANTOOL").join();

    client.eval("return box.schema.create_space('person'):create_index('pk')").join();
  }
}
```

???+ note "Note"

    You can set a stricter data storage schema. To do this, execute the following code instead of 
    the previous one:
    ```java
    client.eval("return box.space.person:format({ {'key', 'integer'}, {'value', 'string'} })").join();
    ```

In the previous calls, we used the `eval` method, which is available for both Box API client and 
Crud API client. Now, after creating the space, we can use Box API methods to work with data. Box API 
is used to work with a single `Tarantool` instance. Even in the case of a cluster, Box API can be 
used to connect to individual nodes.

```java
public class TestClass {

  @Test
  public void test() {
    final TarantoolBoxClient client = TarantoolFactory.box().build();
    // check that we connected successfully and return the Tarantool version we connected to
    // should return "TarantoolResponse(data = [3.2.1-0-g219c4de1a88], formats = {})"
    client.eval("return _TARANTOOL").join();

    client.eval("return box.schema.create_space('person'):create_index('pk')").join();

    var space = client.space("person"); // get the person space
    space.insert(Arrays.asList(1, "Hello World")).join(); // write one tuple
    
    var result = space.select(1).join(); // get the written tuple
  }
}
```
