# Інструкція з підключення до PostgreSQL

## Крок 1: Встановлення PostgreSQL

1. Завантажте та встановіть PostgreSQL з офіційного сайту: https://www.postgresql.org/download/
2. Під час встановлення запам'ятайте пароль для користувача `postgres`

## Крок 2: Створення бази даних

1. Відкрийте pgAdmin 4 або psql
2. Створіть нову базу даних:
   ```sql
   CREATE DATABASE kursova_db;
   ```

## Крок 3: Налаштування підключення

Відредагуйте файл `src/main/resources/database.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/kursova_db
db.username=postgres
db.password=ВАШ_ПАРОЛЬ
db.driver=org.postgresql.Driver
```

**АБО** встановіть системні змінні:
- `db.url=jdbc:postgresql://localhost:5432/kursova_db`
- `db.username=postgres`
- `db.password=ВАШ_ПАРОЛЬ`

## Крок 4: Запуск проєкту

1. Зберіть проєкт:
   ```bash
   mvn clean compile
   ```

2. Запустіть сервер:
   ```bash
   mvn exec:java -Dexec.mainClass=Application
   ```

При першому запуску автоматично створяться всі таблиці з файлу `src/main/resources/schema.sql`.

## Структура таблиць

- `categories` - категорії товарів
- `users` - користувачі (email, password, role)
- `products` - товари
- `orders` - замовлення
- `order_products` - зв'язок між замовленнями та товарами (many-to-many)
- `reviews` - відгуки

## Перевірка підключення

Якщо виникають помилки підключення:
1. Перевірте, що PostgreSQL запущений
2. Перевірте правильність даних у `database.properties`
3. Перевірте, що база даних `kursova_db` існує
4. Перевірте права доступу користувача `postgres`

