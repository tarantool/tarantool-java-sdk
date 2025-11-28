---
uid: customer-maven
---

# Загрузка артефактов из клиентской зоны с помощью maven

#### Генерация settings.xml скриптом и загрузка артефактов через maven

1. Воспользоваться скриптом [
   `java_make_settings.sh`](https://www.tarantool.io/en/accounts/customer_zone/packages/maven/download_from_customer_zone.sh)
   (нужен доступ в клиентскую зону) и сгенерировать `settings.xml` вместе с `session id`:
   ```bash
   USER_NAME=email PASSWORD=password ./java_make_settings.sh
   ```
   После успешного выполнения команды, появится файл настроек `settings.xml`.
   > [!ВАЖНО]
   > Файл действует ограниченное время - около 5 минут
2. Используя ранее сгенерированный `setting.xml`, загрузить зависимости проекта:
   ```bash
   mvn -s settings.xml dependency:resolve
   ```
3. Удалить информацию об артефактах `tarantool`, чтобы `maven` не искал информацию заново:
   ```bash
   find ~/.m2/repository/io/tarantool -name "_remote.repositories" -type f -delete
   ```

#### Загрузка артефактов из клиентской зоны c помощью maven

:::
Необходимо обладать логином и паролем к `download.tarantool.io` с доступом к `maven` директории.
:::

:::
Для того чтобы использовать **download.tarantool.io**, как Maven repository, нужно добавить
следующий блок в файл с настройками Maven (обычно он хранится в директории `~/.m2/settings.xml`).
Нужно поменять конфигурацию maven как показано ниже, не забыв заменить {user_name} и {password}:

```xml

<settings>
  <servers>
    <server>
      <id>tarantool-io</id>
      <username>{user_name}</username>
      <password>{password}</password>
    </server>
  </servers>

  <activeProfiles>
    <activeProfile>tarantool-io</activeProfile>
  </activeProfiles>

  <profiles>
    <profile>
      <id>tarantool-io</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2</url>
        </repository>
        <repository>
          <id>tarantool-io</id>
          <url>https://download.tarantool.io/maven</url>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
</settings>
```

После необходимо добавить зависимости в `pom.xml` проекта.
:::
