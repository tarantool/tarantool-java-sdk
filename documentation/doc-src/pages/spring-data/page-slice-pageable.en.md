---
title: Pagination
---

## Pagination in tarantool-spring-data

Pagination in tarantool-spring-data supports the `Slice<T>`, `Page<T>`, and `Pageable` interfaces.
Instead of offset-based pagination, which is not supported by tarantool/crud, the interfaces implement
cursor-based pagination.

### Pageable

The `Pageable` interface represents page parameters: number, size.

To use pagination in tarantool-spring-data, the `Pageable` interface has been extended with the
`TarantoolPageable` interface. Now the page settings additionally contain a tuple cursor
(parameter [after](https://github.com/tarantool/crud?tab=readme-ov-file#select)), relative to
which the page is read, as well as a `PaginationDirection` object.

To create a `Pageable` instance, the constructors of the `TarantoolPageRequest` class must be used.
There are two ways to create `TarantoolPageable`:

- `new TarantoolPageRequest<>(int pageSize)` - paginated selection from the beginning of the data in `space`.
  The `pageSize` argument is the page size.
- `new TarantoolPageRequest<T>(int pageNumber, int pageSize, T cursor)` - paginated selection
  of data starting from some page. Here you need to consider the correspondence between the page number
  (starting from 0) and the passed cursor.

As an example, consider a `space` (table) that contains 100 records satisfying your pagination query.
You want to get 10 records per page, starting from the second page (`pageNumber = 1`). To do this, you should pass the following parameters:

```java
TarantoolPageable<Person> pageable = new TarantoolPageRequest<>(1, 10, domainClassCursor);
```

where `domainClassCursor` is an instance of the domain class (in the example, the `Person` class), which is
a tuple cursor
(see the parameter [after](https://github.com/tarantool/crud?tab=readme-ov-file#select)).

Example of a data model class:

```java

@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonIgnoreProperties(ignoreUnknown = true) // for example bucket_id
@Data
@KeySpace("person")
public class Person {

  @Id
  @JsonProperty("id")
  private Integer id;

  @Field("is_married")
  @JsonProperty("isMarried")
  private Boolean isMarried;

  @JsonProperty("name")
  private String name;
}
```

### Page

When calling methods with `Page`, two requests are executed:

1. A request for data selection. If there is no data, `Page<T>` with `Unpaged` pageable is returned.
2. If data exists, a request is made to calculate the total number of records in `space`
   that satisfy the conditions (the `count()` method). Based on the total number of records and the page size
   (passed in `TarantoolPageRequest` when creating) the total number of pages is determined
   (maximum page number).

**Important:** when working in parallel with paginated pagination and data manipulation (for example,
deleting or adding records), the number of pages may change.

Thus:

1. When moving **forward**, a page exists (not empty, and `Pageable` is not `Unpaged`),
   if **data is received** and the page number **does not exceed the maximum number**.
2. When moving **backward**, a page exists if **data is received** and `pageNumber >= 0`.

### Slice

When calling methods with `Slice`, `n + 1` records are requested, where `n` is the
slice size. If `n + 1` records are received, the next slice exists.

Thus:

1. When moving **forward**, a slice exists (not empty, and `Pageable` is not `Unpaged`) if
   **data is received**.
2. When moving **backward**, a slice exists if **data is received** and `pageNumber >= 0`.

### Specifics When the Tuple Cursor and Page Number Don't Match

Consider an example where the tuple cursor corresponds to the tuple after which the page begins
(see the parameter [after](https://github.com/tarantool/crud?tab=readme-ov-file#select)) with number 0
(`pageNumber = 0` (i.e. `null` was passed)), but in the `TarantoolPageRequest` constructor, a number
for another page is specified, for example, the second (`pageNumber = 1`):

```plaintext
                                                         (0)    (1)    (2)    (3)
Actual data splitting in space:                     |-----||-----||-----||-----|
 
                                                    [0]    [1]    [2]    [3]
Splitting based on passed parameters:     |-----||-----||-----||-----|
  
                                                               (forward)
                                                       |------------------->
                                                    [0]    [1]    [2]    [3]
Page:                                           |--X--||-----||-----||-----|
                                                <--------------------------|
                                                               (backward)

                                                               (forward)
                                                       |------------------------->
                                                  [0]    [1]    [2]    [3]   [4](3)
Slice:                                          |--X--||-----||-----||-----||-----|
                                                <---------------------------------|
                                                               (backward)
```

In this example, the following happens:

Maximum page number - `[3]` (your numbering). It corresponds to page number `(2)`
in the actual data splitting.

- `Page`:
    - When moving **forward**, the last existing page (not empty, and `Pageable` is not
      `Unpaged`) will have number `[3]` (`(2)` - in the actual page splitting).
    - When moving **backward**, the last existing page will have number `[1]`, but the method `hasPrevious()` will return `true` for this page. When continuing to move, you will get one empty page with `Unpaged` pageable.
- `Slice`:
    - When moving **forward**, the last existing slice (not empty, and `Pageable` is not
      `Unpaged`) will have number `[4]` (`(3)` - in the actual page splitting).
    - When moving **backward**, the last existing slice will have number `[1]`, but the method `hasPrevious()` will return `true` for this slice. When continuing to move, you will get one empty slice with `Unpaged` pageable.

Consider an example where the tuple cursor corresponds to the tuple after which the page begins
(see the parameter [after](https://github.com/tarantool/crud?tab=readme-ov-file#select)) with number 1
(`pageNumber = 1`), but in the `TarantoolPageRequest` constructor, a number for another page is specified,
for example 1 (`pageNumber = 0`):

```plaintext
                                                  (0)    (1)    (2)    (3)
Actual data splitting in DB:                 |-----||-----||-----||-----|

                                                         [0]    [1]    [2]    [3]
Splitting based on passed parameters:            |-----||-----||-----||-----|

                                                               (forward)
                                                       |-------------------------->
                                                         [0]    [1]    [2]    [3]
Page:                                                  |-----||-----||-----||--X--|
                                                       <-------------------|
                                                                 (backward)
  
                                                                 (forward)
                                                       |------------------->
                                                         [0]    [1]    [2]
Slice:                                                 |-----||-----||-----|
                                                       <-------------------|
                                                              (backward)
```

In this example, the following happens:

Maximum page number - `[3]` (your numbering). It corresponds to a non-existent
page in the actual data splitting:

- `Slice`:
    - When moving **forward**, the last existing slice will have number `[2]`.
    - When moving **backward**, the last existing slice will have number
      `[0]` (`(1)` - in the actual page splitting).

- `Page`:
    - When moving **forward**, the last existing page (not empty, and `Pageable` is not
      `Unpaged`) will have number `[2]`, but the method `hasNext()` will return `true` for this page. When continuing to move, you will get one empty page with
      `Unpaged` pageable.
    - When moving **backward**, the last existing page will have number `[0]` (`(1)` - in
      the actual page splitting).

### Working with Derived Methods

When working with derived methods, there is a peculiarity in using paginated queries.

- If the predicate field is a field for which an index is built, then the search for elements will
  be performed on this index. This means that when moving forward, the records returned on the pages
  will be in ascending index order, when moving backward in descending index order
  (use Java sorting tools to sort the page elements).
- If the predicate field is not a field for which an index is built, then the search for elements will
  be performed on the primary index. The records returned on the pages will be in ascending order
  of the primary index regardless of the pagination direction.

???+ warning "Important"

    - Maintain correspondence between the page number and the passed cursor. Otherwise, empty
      pages with `unpaged` pageable may occur. If you are unsure about the correctness of passing the cursor together with 
      the page number, then use the constructor `new TarantoolPageRequest<>(int pageSize)`, 
      which allows you to traverse from the beginning of the data and does not have the drawbacks 
      inherent in pagination starting from an arbitrary page.
    - If you are working on Spring Data 3.1.x or higher, use 
      [Scroll API](scroll-api.md) to implement paginated queries instead of `Slice` and `Page`.
