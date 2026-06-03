# Приклади запитів для Postman

## 1. Реєстрація користувача (USER)
**POST** `http://localhost:8080/api/users`

**Headers:**
```
Content-Type: application/x-www-form-urlencoded
```

**Body (x-www-form-urlencoded):**
```
email: user@example.com
password: password123
role: USER
```

**Або для адміна:**
```
email: admin@example.com
password: admin123
role: ADMIN
```

---

## 2. Логін користувача
**POST** `http://localhost:8080/api/users/login`

**Headers:**
```
Content-Type: application/x-www-form-urlencoded
```

**Body (x-www-form-urlencoded):**
```
email: user@example.com
password: password123
```

**Відповідь:**
```json
{
  "token": "base64_encoded_token",
  "email": "user@example.com",
  "role": "USER"
}
```

---

## 3. Отримати всі товари
**GET** `http://localhost:8080/api/products`

**Headers:** (не потрібні)

**Відповідь:**
```json
[
  {
    "id": 1,
    "name": "Футболка Nike Basic",
    "price": 599.99,
    "categoryId": 0,
    "type": "футболка",
    "size": "M",
    "gender": "унісекс",
    "imageUrl": "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400"
  },
  {
    "id": 2,
    "name": "Джинси Levi's 501",
    "price": 1299.99,
    "categoryId": 0,
    "type": "джинси",
    "size": "L",
    "gender": "чоловічий",
    "imageUrl": "https://images.unsplash.com/photo-1542272604-787c3835535d?w=400"
  }
]
```

---

## 4. Отримати товар за ID
**GET** `http://localhost:8080/api/products/1`

**Headers:** (не потрібні)

**Відповідь:**
```json
{
  "id": 1,
  "name": "Футболка Nike Basic",
  "price": 599.99,
  "categoryId": 0,
  "type": "футболка",
  "size": "M",
  "gender": "унісекс",
  "imageUrl": "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400"
}
```

---

## 5. Створити товар (тільки для адміна)
**POST** `http://localhost:8080/api/products`

**Headers:**
```
Content-Type: application/x-www-form-urlencoded
Authorization: Bearer YOUR_TOKEN_HERE
```

**Body (x-www-form-urlencoded):**
```
name: Спортивні штани Nike
price: 1999.00
categoryId: 0
type: штани
size: L
gender: чоловічий
imageUrl: https://megasport.ua/api/s3/images/megasport-dev/products/3555570144/68ef96c06b6bd-6389105.png
```

**Відповідь (201 Created):**
```json
{
  "id": 10,
  "name": "Спортивні штани Nike",
  "price": 1999.00,
  "categoryId": 0,
  "type": "штани",
  "size": "L",
  "gender": "чоловічий",
  "imageUrl": "https://megasport.ua/api/s3/images/megasport-dev/products/3555570144/68ef96c06b6bd-6389105.png"
}
```

---

## 6. Оновити товар (тільки для адміна)
**PUT** `http://localhost:8080/api/products/1`

**Headers:**
```
Content-Type: application/x-www-form-urlencoded
Authorization: Bearer YOUR_TOKEN_HERE
```

**Body (x-www-form-urlencoded):**
```
name: Футболка Nike Basic (оновлена)
price: 699.99
categoryId: 0
type: футболка
size: XL
gender: унісекс
imageUrl: https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400
```

**Відповідь (200 OK):**
```json
{
  "id": 1,
  "name": "Футболка Nike Basic (оновлена)",
  "price": 699.99,
  "categoryId": 0,
  "type": "футболка",
  "size": "XL",
  "gender": "унісекс",
  "imageUrl": "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400"
}
```

---

## 7. Видалити товар (тільки для адміна)
**DELETE** `http://localhost:8080/api/products/1`

**Headers:**
```
Authorization: Bearer YOUR_TOKEN_HERE
```

**Відповідь (200 OK):**
```json
{
  "success": true,
  "message": "Товар видалено"
}
```

---

## 8. Створити замовлення (для авторизованого користувача)
**POST** `http://localhost:8080/api/orders`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer YOUR_TOKEN_HERE
```

**Body (raw JSON):**
```json
{
  "productIds": [1, 2, 3]
}
```

**Відповідь (201 Created):**
```json
{
  "id": 1,
  "userId": 1,
  "productIds": [1, 2, 3],
  "date": "2024-01-15 14:30:00",
  "status": "Нове"
}
```

---

## 9. Отримати всі замовлення (тільки для адміна)
**GET** `http://localhost:8080/api/orders`

**Headers:**
```
Authorization: Bearer YOUR_ADMIN_TOKEN_HERE
```

**Відповідь:**
```json
[
  {
    "id": 1,
    "userId": 1,
    "productIds": [1, 2, 3],
    "date": "2024-01-15 14:30:00",
    "status": "Нове"
  },
  {
    "id": 2,
    "userId": 2,
    "productIds": [4, 5],
    "date": "2024-01-15 15:00:00",
    "status": "Відправлено"
  }
]
```

---

## 10. Оновити статус замовлення (тільки для адміна)
**PUT** `http://localhost:8080/api/orders/1`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer YOUR_ADMIN_TOKEN_HERE
```

**Body (raw JSON):**
```json
{
  "status": "Відправлено"
}
```

**Відповідь (200 OK):**
```json
{
  "id": 1,
  "userId": 1,
  "productIds": [1, 2, 3],
  "date": "2024-01-15 14:30:00",
  "status": "Відправлено"
}
```

---

## Примітки:

1. **Токен отримується при логіні** - скопіюйте його з відповіді та використовуйте в заголовку `Authorization: Bearer YOUR_TOKEN`

2. **Тестові облікові записи:**
   - Адмін: `admin@gmail.com` / `admin123`
   - Користувач: `user@gmail.com` / `user123`

3. **Формат даних:**
   - Реєстрація/Логін/Товари: `application/x-www-form-urlencoded`
   - Замовлення: `application/json`

4. **CORS:** Сервер підтримує CORS, тому можна тестувати з браузера

5. **Помилки:**
   - 400 - Bad Request (некоректні дані)
   - 401 - Unauthorized (не авторизовано)
   - 403 - Forbidden (немає прав доступу)
   - 404 - Not Found (ресурс не знайдено)
   - 500 - Internal Server Error (помилка сервера)

