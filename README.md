# OTP Service

Backend-приложение для защиты операций с помощью одноразовых OTP-кодов.

---

## Стек технологий

- Java 21
- Maven
- PostgreSQL 17
- JDBC
- Flyway
- com.sun.net.httpserver (HTTP API)
- JWT (аутентификация)
- BCrypt (хеширование паролей)
- SLF4J + Logback (логирование)
- Angus Mail (Email)
- JSMPP (SMS)
- Telegram Bot API
- Docker / Docker Compose

---

## Возможности

### Аутентификация и пользователи
- Регистрация пользователей
- Роли: `ADMIN` и `USER`
- Запрет создания второго администратора
- Логин с получением JWT токена

### Разграничение доступа
- ADMIN API доступен только администратору
- USER API доступен только пользователю
- Проверка ролей и токенов

### OTP-функционал
- Генерация OTP-кода
- Привязка к операции
- Валидация OTP-кода
- Статусы:
    - `ACTIVE` - активен
    - `EXPIRED` - просрочен
    - `USED` - использован

### Каналы доставки
- FILE (в файл)
- SMS (эмулятор / SMPP)
- Email (эмулятор / SMTP)
- Telegram (эмулятор / Bot API)

### Scheduler
- Автоматически переводит просроченные OTP в `EXPIRED`

---

## Быстрый запуск через Docker

### 1. Клонировать проект

```bash
git clone <repository-url>
cd otp-service
```

### 2. Запустить приложение
```bash
docker compose up --build
```
или в фоне:

```bash
docker compose up -d --build
```

### 3. Проверить health-check

```bash
curl -i http://localhost:8082/health
```

Ожидаемый ответ:

```JSON
{"status":"OK"}
```

## Ручная проверка API

### 1. Регистрация администратора
```bash
curl -i -X POST http://localhost:8082/api/auth/register \
-H "Content-Type: application/json" \
-d '{
"login": "admin",
"password": "admin123",
"role": "ADMIN"
}'
```

### 2. Регистрация пользователя
```bash
curl -i -X POST http://localhost:8082/api/auth/register \
-H "Content-Type: application/json" \
-d '{
"login": "user1",
"password": "user123",
"role": "USER"
}'
```
### 3. Логин администратора

Приведен пример команды с сохранением токена в переменную окружения `ADMIN_TOKEN` для удобства дальнейшего использования:

```bash
   ADMIN_TOKEN=$(curl -s -X POST http://localhost:8082/api/auth/login \
   -H "Content-Type: application/json" \
   -d '{"login":"admin","password":"admin123"}' \
   | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
```

### 4. Логин пользователя

Приведен пример команды с сохранением токена в переменную окружения `USER_TOKEN` для удобства дальнейшего использования:

```bash
   USER_TOKEN=$(curl -s -X POST http://localhost:8082/api/auth/login \
   -H "Content-Type: application/json" \
   -d '{"login":"user1","password":"user123"}' \
   | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
```

## Admin API

### 1. Получить список пользователей

```bash
curl -i http://localhost:8082/api/admin/users \
-H "Authorization: Bearer $ADMIN_TOKEN"
```

### 2. Изменить OTP конфигурацию

```bash
curl -i -X PUT http://localhost:8082/api/admin/otp-config \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $ADMIN_TOKEN" \
-d '{
"codeLength": 6,
"ttlSeconds": 300
}'
```

## User API

### 1. Генерация OTP

```bash
curl -i -X POST http://localhost:8082/api/user/otp/generate \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $USER_TOKEN" \
-d '{
"operationId": "payment-final-check",
"description": "Manual check",
"channels": ["FILE"],
"destination": {}
}'
```

Получить OTP из файла (если в запросе на генерацию был указан тип канала `FILE`): 

```bash
tail -n 5 otp-codes.txt
```

### 2. Валидация OTP
```bash
curl -i -X POST http://localhost:8082/api/user/otp/validate \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $USER_TOKEN" \
-d '{
"operationId": "payment-final-check",
"code": "PASTE_CODE_HERE"
}'
```

Ожидаемый ответ:

```JSON
{
"operationId": "payment-final-check",
"status": "USED",
"valid": true
}
```

## Проверка ролей

### 1. USER не имеет доступ к ADMIN API
```bash
curl -i http://localhost:8082/api/admin/users \
-H "Authorization: Bearer $USER_TOKEN"
```

Ожидаемый ответ 403 (Forbidden):

```JSON
{
  "error":"FORBIDDEN",
  "message":"Access denied"
}
```

### 2. ADMIN не имеет доступ к USER API
```bash
curl -i -X POST http://localhost:8082/api/user/otp/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "operationId": "forbidden",
    "channels": ["FILE"]
  }' 
```

Ожидаемый ответ 403 (Forbidden):

```JSON
{
  "error":"FORBIDDEN",
  "message":"Access denied"
}
```

## Конфигурация каналов

Настройки каналов задаются в файлах:

* src/main/resources/email.properties
* src/main/resources/sms.properties
* src/main/resources/telegram.properties

**По умолчанию каналы выключены:**

* email.enabled=false
* sms.enabled=false
* telegram.enabled=false

В этом режиме каналы работают как эмуляторы (_логируются, но не отправляют реальные сообщения_).

## Фоновый процесс (Scheduler)

В приложении реализован фоновый процесс, который автоматически отслеживает просроченные OTP-коды:

- Запускается при старте приложения
- Работает с заданным интервалом
- Находит все OTP-коды со статусом `ACTIVE`, у которых истёк срок действия
- Переводит их в статус `EXPIRED`

### Конфигурация

Интервал работы задаётся в `application.properties`:

```properties
otp.expiration.scheduler.interval.seconds=60
```