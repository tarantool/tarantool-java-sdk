## Документация

### Локальная сборка

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

### Схемы

Документация поддерживает схемы формата `.drawio`. Поместите свою схему в каталог `assets`. В тексте
markdown сошлитесь на схему как на обычную markdown-картинку. Путь к изображению должен быть
относительным:

```markdown
![](../../../../assets/<some-paths>/schema.drawio)
```
