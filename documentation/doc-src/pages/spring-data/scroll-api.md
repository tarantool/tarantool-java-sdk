---
title: Пагинация (Scroll API)
---

## Поддержка Scroll API для tarantool-spring-data

### Scroll API в Spring Data

Для ознакомления с интерфейсом обратитесь
к [документации Spring Data](https://docs.spring.io/spring-data/jpa/reference/data-commons/repositories/scrolling.html).

### Особенности работы с Scroll API в tarantool-spring-data

#### Создание ScrollPosition

Для создания `ScrollPosition` воспользуйтесь статическими методами интерфейса
`TarantoolScrollPosition`:

1. `static TarantoolScrollPosition forward(Pair<String, ?> indexKey);`:

    ???+ warning "Важно"
    
        Метод создает `ScrollPosition`, на основе переданной пары, где первым аргументом является 
        **ИМЯ ИНДЕКСА**, на основе которого необходимо сделать прокрутку. 
        Второй аргумент - значение индексированного поля, от которого необходимо начать прокрутку.
        Данный метод настраивает прокрутку в сторону увеличения индекса.

    - Пример использования метода:
        ```java
        // Листинг 1.
        
        public List<Person> getPersons() {
            ScrollPosition scrollPosition = TarantoolScrollPosition.forward(Pair.of("pk", 5)); //(1)!
            Window<Person> personResult = repository.findFirst5ByName("Petya", scrollPosition);
            return personResult.getContent();
        }
        ```

        Представим, что создан первичный ключ ("pk") (схема ниже), по которому вы хотите производить
        прокрутку. Представим также, что все записи имеют `name == "Petya"`.

        В (1) (см. листинг 1) интерфейс `TarantoolScrollPosition` создает `ScrollPosition` на основе
        индекса с названием "pk" и значения 5 с прокруткой вперед.

        Это означает, что прокрутка начнется с записи `"pk" >= 5` (помимо прочих условий из названия
        производного метода), и записи будут прокручиваться по возрастанию индекса (схема ниже).
        Так как указан лимит в 5 записей (`find**First5**ByName`), возвращенное окно будет
        содержать элементы с "pk" в отрезке [5,9].


        ```plaintext
        index ("pk" - unsigned long)
        Возрастание индекса ->
        -----------------------------------------
        0-1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-... - записи
        -----------------------------------------
                 |-------->|
                 |  окно   |
                 |  [5;9]  |
        ```

    * Если необходимо начать прокрутку с **начала индекса** в сторону увеличения индекса
      (предположим, что имеем все те же настроки БД как и в прошлом примере), то передайте в
      `TarantoolScrollPosition.forward(Pair<String, ?>)` в качестве значения пары
      `Collections.emptyList()`:

        ```java
        // Листинг 2.
        
        public List<Person> getPersons() {
            ScrollPosition scrollPosition = TarantoolScrollPosition.forward(Pair.of("pk", Collections.emptyList())); //(1)
            Window<Person> personResult = repository.findFirst5ByName("Petya", scrollPosition);
            return personResult.getContent();
        }
        ```

        При настройках `TarantoolScrollPosition` с параметрами `(1)` (см. листинг 2) прокрутка будет
        вести себя следующим образом:

        ```plaintext
        index ("pk" - unsigned long)
        Возрастание индекса ->
        -----------------------------------------
        0-1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-... - записи
        -----------------------------------------
        |------->|
        |  окно  |
        |  [0;4] |
        ```

2. `static TarantoolScrollPosition backward(Pair<String, ?> indexKey);`:

    ???+ warning "Важно"

        Метод создает аналогично `static TarantoolScrollPosition forward(Pairstring, indexKey);`
        прокрутку, но уже в сторону убывания индекса.

    * Пример использования метода:

        ```java
        // Листинг 3.
        
        public List<Person> getPersons() {
            ScrollPosition scrollPosition = TarantoolScrollPosition.backward(Pair.of("pk", 9)); //(1)
            Window<Person> personResult = repository.findFirst5ByName("Petya", scrollPosition);
            return personResult.getContent();
        }
        ```

        `(1)` - настраивает прокрутку в сторону убывания индекса с названием "pk", начиная с записи,
        которой `"pk" <= 9` (см. листинг 3).

        Это означает, что прокрутка начнется с записи `"pk" <= 9` (помимо прочих условий из названия
        производного метода) и записи будут прокручиваться по убыванию индекса (схема ниже). Так как
        указан лимит в 5 записей (`find**First5**ByName`), возвращенное окно будет содержать
        элементы с "pk" в отрезке [9,5].

        ```plaintext
        index ("pk" - unsigned long)
        Возрастание индекса ->
        -----------------------------------------
        0-1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-... - записи
        -----------------------------------------
                 |<--------|
                 |   окно  |
                 |   [5;9] |
        ```
    * Если необходимо начать прокрутку с **конца индекса** в сторону уменьшения индекса
      (предположим, что имеем все те же настроки БД как и в прошлых примерах), то передайте в
      `TarantoolScrollPosition.backward(Pair<String, ?>)` в качестве значения пары
      `Collections.emptyList()`:

        ```java
        // Листинг 4.
        
        public List<Person> getPersons() {
            ScrollPosition scrollPosition = TarantoolScrollPosition.backward(Pair.of("pk", Collections.emptyList()));
            Window<Person> personResult = repository.findFirst5ByName("Petya", scrollPosition);
            return personResult.getContent();
        }
        ```

        ```plaintext
        index ("pk" - unsigned long)
        Возрастание индекса ->
        -------------------
        0-1-2-3-4-5-6-7-8-9 - записи (пусть в бд всего 10 записей)
        -------------------
                 |<--------|
                 |  окно   |
                 |  [5;9]  |
        ```

3. Если необходимо сделать смену направления прокрутки (например, клиента зашел на 2 страницу,
   потом хочет вернуться на 1), то используйте метод `TarantoolScrollPosition reverse();`:

    ???+ warning "Важно"

        Стоит заметить, что после смены направления, прокрутка не ограничивается условием, заданным 
        в выше упомянутых методах (значение пары). Это означает, что после смены направления 
        прокрутка будет идти до конца (или начала индекса).

    * Пример использования метода:

        ```java
        // Листинг 5.
         
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

        Предположим, что в базе более 10 записей и все они имеют `name == "Petya"`. Тогда код на
        листинге 5 работает следующим образом (аналогично и для случая переключения в сторону
        возрастания индекса):

        ```plaintext
        index ("pk" - unsigned long)
        Возрастание индекса ->
        --------------------------------------
        0-1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-... - записи
        --------------------------------------
             |--------------------->|
             |       окно [3;12]    |
 
                        |
                        | берем ScrollPosition из элемента под индексом 4 (отсчет в самом окне) в окне forwardResult
                        V
        index ("pk" - unsigned long)
        Возрастание индекса ->
        --------------------------------------
        0-1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-... - записи
        --------------------------------------
           |<--------|
           |   окно  |
           |  [2;6]  |
        ```

#### TarantoolWindowIterator

Данный класс является аналогом
[WindowIterator](https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/support/WindowIterator.html),
работа которого адаптирована под использование с Tarantool (интерфейс класса аналогичен
`WindowIterator`).
Пример использования:

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
