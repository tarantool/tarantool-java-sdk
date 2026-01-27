---
title: Repository API
---

## Работа со Spring Data Repository API

Чтобы определить интерфейс репозитория, необходимо сконфигурировать клиент с помощью аннотации
`@EnableTarantoolRepositories`. Пример конфигурации можно найти
здесь: [`конфигурирование клиентов`](configuration.md). Интерфейс репозитория должен быть
типизирован классом модели данных и типом идентификатора (ID type).

### Создание класса модели данных

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

???+ warning "Важно"

    Классы моделей данных должны **обязательно** аннотироваться 
    `@JsonFormat(shape = JsonFormat.Shape.ARRAY)` для правильной сериализации и десериализации.

???+ warning "Важно"

    Аннотация `@KeySpace("person")` позволяет указать, какой `space` в Tarantool связан с данным 
    классом модели данных. Если аннотация будет опущена, то название класса интерпретируется как 
    название `space`. (Для примера выше: с аннотацией: "person"; без аннотации: результат работы 
    функции `Person.class.getName()`)

???+ warning "Важно"

    Аннотация `@Field` позволяет задать название поля, которое будет использовано для работы с 
    Tarantool. Без аннотации название поля класса будет интерпретироваться как название поля 
    `space` (Для примера выше: с аннотацией: "is_married"; без аннотации: "isMarried")

???+ warning "Важно"

    Аннотация `@JsonProperty` позволяет задать название поля, которое будет использовано для работы 
    с Jackson. Без аннотации возможны ситуации когда jackson создаст неправильный порядок полей. 
    Особенно это может произойти если поля приватные, и используется библиотека Lombok для 
    генерации сеттеров полей. Jackson может не увидеть сеттер, если в сгенерированном сеттере нет 
    регистрозависимого вхождения названия поля.

### Создание пользовательского Repository

```java
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PersonRepository extends PagingAndSortingRepository<Person, Integer> {}
```

Таким образом, пользовательский `Repository` будет наследовать функциональность `CrudRepository`.
К примеру, мы можем сохранить экземпляр `Person`, а после получить его по идентификатору:

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
