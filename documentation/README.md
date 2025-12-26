## Documentation

### Local Build

To deploy the site locally on the current branch/tag:

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

### Schemas

Documentation supports `.drawio` format schemas. Place your schema in the `assets` directory. In the markdown text,
refer to the schema as a regular markdown image. The path to the image must be relative:

```markdown
![](../../../../assets/<some-paths>/schema.drawio)
```
