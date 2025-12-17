## Документация

Для того чтобы развернуть сайт локально на текущей ветке/теге:

1.  ```bash
    git checkout <branch/tag>
    ```
2. ```bash
    cd documentation
    ```
3. ```bash
    python3 -m venv venv
    ```
4. ```bash
    source venv/bin/activate
    ```
5. ```bash
    pip install -r requirements.txt
    ```
6. ```bash
    mkdocs serve 
    ```