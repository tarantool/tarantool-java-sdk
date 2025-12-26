---
title: Repository API
---

## Working with Spring Data Repository API

To define a repository interface, you need to configure the client using the
`@EnableTarantoolRepositories` annotation. An example configuration can be found
here: [`client configuration`](configuration.md). The repository interface must be
typed with the data model class and the ID type.

### Creating a Data Model Class

```java

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonIgnoreProperties(ignoreUnknown = true) // for example bucket_id
@KeySpace("person")
public class Person {

  @Id
  @JsonProperty("id")
  public Integer id;

  @Field("is_married")
  @JsonProperty("isMarried")
  public Boolean isMarried;

  @JsonProperty("name")
  public String name;

  // constructors, getters, setters 
}
```

???+ warning "Important"

    Data model classes must **mandatory** be annotated with 
    `@JsonFormat(shape = JsonFormat.Shape.ARRAY)` for proper serialization and deserialization.

???+ warning "Important"

    The `@KeySpace("person")` annotation allows specifying which `space` in Tarantool is associated with this 
    data model class. If the annotation is omitted, the class name is interpreted as the 
    `space` name. (For the example above: with annotation: "person"; without annotation: result of 
    the `Person.class.getName()` function)

???+ warning "Important"

    The `@Field` annotation allows specifying the field name that will be used for working with 
    Tarantool. Without the annotation, the class field name is interpreted as the `space` field 
    name (For the example above: with annotation: "is_married"; without annotation: "isMarried")

???+ warning "Important"

    The `@JsonProperty` annotation allows specifying the field name that will be used for working 
    with Jackson. Without the annotation, there can be situations where jackson creates an incorrect field order. 
    This can especially happen if the fields are private and the Lombok library is used to 
    generate field setters. Jackson may not see the setter if the generated setter does not contain 
    a case-sensitive occurrence of the field name.

### Creating a Custom Repository

```java
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PersonRepository extends PagingAndSortingRepository<Person, Integer> {}
```

Thus, the custom `Repository` will inherit the functionality of `CrudRepository`.
For example, we can save a `Person` instance and then retrieve it by ID:

```java
import org.springframework.stereotype.Service;

@Service
public class PersonService {

  private PersonRepository repository;

  public PersonService(PersonRepository repository) {
    this.repository = repository;
  }

  public Person savePerson(Person person) {
    return this.repository.save(person);
  }

  public Optional<Person> selectPersonById(int id) {
    return this.repository.findById(id);
  }
}
```
