---
uid: customer-script
---

# Загрузка артефактов из клиентской зоны при помощи скрипта

Необходимые компоненты:

- [Maven](https://maven.apache.org)
- Скрипт, загруженный из
  [клиентской зоны](https://www.tarantool.io/en/accounts/customer_zone/packages/maven/download_from_customer_zone.sh)

> [!ВАЖНО]
> 
> Скачанный скрипт необходимо сделать исполняемым:
>
> ```shell
> chmod +x ./download_from_customer_zone.sh
> ```

#### Использование скрипта для генерации settings.xml

Общий формат работы со скриптом:

```
./download_from_customer_zone.sh <email_from_customer_zone> <password_from_customer_zone> <version>
```

> `email_from_customer_zone` - email от клиентской зоны
>
> `password_from_customer_zone` - пароль от клиентской зоны
>
> `version` - версия артефактов

Пример вызова:

```bash
./download_from_customer_zone.sh email@mail.ru qwerty 1.4.0
```
