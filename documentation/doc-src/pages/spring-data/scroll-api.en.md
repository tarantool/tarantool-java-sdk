---
title: Pagination (Scroll API)
---

## Scroll API Support for tarantool-spring-data

### Scroll API in Spring Data

For information about the interface, refer to
[Spring Data Documentation](https://docs.spring.io/spring-data/jpa/reference/data-commons/repositories/scrolling.html).

### Features of Working with Scroll API in tarantool-spring-data

#### Creating ScrollPosition

To create a `ScrollPosition`, use the static methods of the
`TarantoolScrollPosition` interface:

1. `static TarantoolScrollPosition forward(Pair<String, ?> indexKey);`:

    ???+ warning "Important"
    
        This method creates a `ScrollPosition` based on the passed pair, where the first argument is 
        the **INDEX NAME**, on which the scrolling must be performed. 
        The second argument is the value of the indexed field from which scrolling must begin.
        This method configures scrolling in the direction of increasing index.

    - Example of using the method:
        ```java
        // Listing 1.
        
        public List<Person> getPersons() {
            ScrollPosition scrollPosition = TarantoolScrollPosition.forward(Pair.of("pk", 5)); //(1)!
            Window<Person> personResult = repository.findFirst5ByName("Petya", scrollPosition);
            return personResult.getContent();
        }
        ```

        Let's assume a primary key ("pk") (schema below) was created, on which you want to perform
        scrolling. Also assume that all records have `name == "Petya"`.

        In (1) (see listing 1), the `TarantoolScrollPosition` interface creates a `ScrollPosition` based
        on the index named "pk" and the value 5 with forward scrolling.

        This means that scrolling will start with the record `"pk" >= 5` (in addition to other conditions from the derived method name), and records will be scrolled in ascending index order (schema below).
        Since a limit of 5 records was specified (`find**First5**ByName`), the returned window will
        contain elements with "pk" in the range [5,9].


        ```plaintext
        index ("pk" - unsigned long)
        Increasing index ->
        -----------------------------------------
        0-1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-... - records
        -----------------------------------------
                 |-------->|
                 |  window   |
                 |  [5;9]  |
        ```

    * If you need to start scrolling from the **beginning of the index** in the direction of increasing index
      (assuming we have the same database settings as in the previous example), then pass `Collections.emptyList()`
      as the value of the pair in `TarantoolScrollPosition.forward(Pair<String, ?>)`:

        ```java
        // Listing 2.
        
        public List<Person> getPersons() {
            ScrollPosition scrollPosition = TarantoolScrollPosition.forward(Pair.of("pk", Collections.emptyList())); //(1)
            Window<Person> personResult = repository.findFirst5ByName("Petya", scrollPosition);
            return personResult.getContent();
        }
        ```

        With `TarantoolScrollPosition` settings with parameters (1) (see listing 2), scrolling will behave as follows:

        ```plaintext
        index ("pk" - unsigned long)
        Increasing index ->
        -----------------------------------------
        0-1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-... - records
        -----------------------------------------
        |------->|
        |  window  |
        |  [0;4] |
        ```

2. `static TarantoolScrollPosition backward(Pair<String, ?> indexKey);`:

    ???+ warning "Important"

        This method creates scrolling similar to `static TarantoolScrollPosition forward(Pairstring, indexKey);`
        but in the direction of decreasing index.

    * Example of using the method:

        ```java
        // Listing 3.
        
        public List<Person> getPersons() {
            ScrollPosition scrollPosition = TarantoolScrollPosition.backward(Pair.of("pk", 9)); //(1)
            Window<Person> personResult = repository.findFirst5ByName("Petya", scrollPosition);
            return personResult.getContent();
        }
        ```

        `(1)` - configures scrolling in the direction of decreasing index with the name "pk", starting from the record
        where `"pk" <= 9` (see listing 3).

        This means that scrolling will start with the record `"pk" <= 9` (in addition to other conditions from the derived method name)
        and records will be scrolled in descending index order (schema below). Since
        a limit of 5 records was specified (`find**First5**ByName`), the returned window will contain
        elements with "pk" in the range [9,5].

        ```plaintext
        index ("pk" - unsigned long)
        Increasing index ->
        -----------------------------------------
        0-1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-... - records
        -----------------------------------------
                 |<--------|
                 |   window  |
                 |   [5;9] |
        ```
    * If you need to start scrolling from the **end of the index** in the direction of decreasing index
      (assuming we have the same database settings as in the previous examples), then pass `Collections.emptyList()`
      as the value of the pair in `TarantoolScrollPosition.backward(Pair<String, ?>)`:

        ```java
        // Listing 4.
        
        public List<Person> getPersons() {
            ScrollPosition scrollPosition = TarantoolScrollPosition.backward(Pair.of("pk", Collections.emptyList()));
            Window<Person> personResult = repository.findFirst5ByName("Petya", scrollPosition);
            return personResult.getContent();
        }
        ```

        ```plaintext
        index ("pk" - unsigned long)
        Increasing index ->
        -------------------
        0-1-2-3-4-5-6-7-8-9 - records (suppose there are only 10 records in the db)
        -------------------
                 |<--------|
                 |  window   |
                 |  [5;9]  |
        ```

3. If you need to change the scrolling direction (for example, a client went to page 2,
   then wants to return to page 1), use the `TarantoolScrollPosition reverse();` method:

    ???+ warning "Important"

        It should be noted that after changing the direction, scrolling is not limited by the condition
        specified in the above-mentioned methods (pair value). This means that after changing the direction, 
        scrolling will proceed to the end (or beginning) of the index.

    * Example of using the method:

        ```java
        // Listing 5.
         
        public List<Person> getPersons() {
            List<Person> result = new ArrayList<>();
           
            ScrollPosition scrollPosition = TarantoolScrollPosition.forward(Pair.of("pk", 3); 
            Window<Person> forwardResult = repository.findFirst10ByName("Petya", scrollPosition);
            forwardResult.forEach(result::add);
 
            ScrollPosition reversedPosition = ((TarantoolScrollPosition) forwardResult.positionAt(forwardResult.size() / 2 - 1)).reverse();
            Window<Person> backwardResult = repository.findFirst5ByName("Petya", reversedPosition);
            backwardResult.forEach(result::add);
 
            return result;
            }
        ```

        Assume there are more than 10 records in the database and all of them have `name == "Petya"`. Then the code in
        listing 5 works as follows (similarly for switching in the direction of increasing index):

        ```plaintext
        index ("pk" - unsigned long)
        Increasing index ->
        --------------------------------------
        0-1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-... - records
        --------------------------------------
             |--------------------->|
             |       window [3;12]    |
 
                        |
                        | take ScrollPosition from element at index 4 (counting within the window) in forwardResult
                        V
        index ("pk" - unsigned long)
        Increasing index ->
        --------------------------------------
        0-1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-... - records
        --------------------------------------
           |<--------|
           |   window  |
           |  [2;6]  |
        ```

#### TarantoolWindowIterator

This class is the equivalent of
[WindowIterator](https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/support/WindowIterator.html),
whose operation has been adapted for use with Tarantool (the class interface is similar to
`WindowIterator`).
Example of use:

```java
    TarantoolScrollPosition initialScrollPosition =
    TarantoolScrollPosition.forward(Pair.of("pk", Collections.emptyList()));

TarantoolWindowIterator<Person> personIterator =
    TarantoolWindowIterator.of(scrollPosition -> repository.findFirst10ByAge(10, scrollPosition))
        .startingAt(initialScrollPosition);

List<Person> result = new ArrayList<>();

    while(personIterator.hasNext()) {
    result.add(personIterator.next());
    }
```
