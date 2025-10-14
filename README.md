# Filmorate - Система управления фильмами и пользователями

![Java](https://img.shields.io/badge/Java-21%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0%2B-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)

**Filmorate** — это RESTful веб-приложение для управления информацией о фильмах и пользователях, разработанное на Spring Boot с использованием современных практик Java-разработки.

---

## 🚀 Основные возможности

### Управление фильмами:
- Создание, обновление и получение информации о фильмах
- Валидация данных фильмов (название, описание, дата релиза, продолжительность)
- Проверка уникальности фильмов по названию и году выпуска
- Автоматическая проверка минимальной даты релиза (28 декабря 1895 года)

### Управление пользователями:
- Регистрация и обновление профилей пользователей
- Валидация email, логина и даты рождения
- Автоматическая нормализация данных (приведение email к нижнему регистру)
- Установка имени из логина, если имя не указано

### Безопасность и валидация:
- Комплексная валидация входных данных
- Обработка исключений с понятными сообщениями об ошибках
- Проверка на дубликаты (email, логин, фильмы)
- Глобальная обработка исключений с соответствующими HTTP-статусами

---

## 🛠 Технологический стек

- **Java 21**
- **Spring Boot 3.0+**
- **Spring Validation**
- **Lombok**
- **Maven**

---

## 📋 API Endpoints

### Фильмы

| Метод  | Endpoint      | Описание                        |
|--------|---------------|---------------------------------|
| `POST` | `/films`      | Создание нового фильма          |
| `GET`  | `/films`      | Получение списка всех фильмов   |
| `GET`  | `/films/{id}` | Получение фильма по ID          |
| `PUT`  | `/films`      | Обновление существующего фильма |

### Пользователи

| Метод  | Endpoint      | Описание                              |
|--------|---------------|---------------------------------------|
| `POST` | `/users`      | Создание нового пользователя          |
| `GET`  | `/users`      | Получение списка всех пользователей   |
| `GET`  | `/users/{id}` | Получение пользователя по ID          |
| `PUT`  | `/users`      | Обновление существующего пользователя |

---

## 🎯 Примеры использования

### Создание фильма

**Запрос:**
```http
POST /films
Content-Type: application/json

{
  "name": "Интерстеллар",
  "description": "Фантастический эпос о путешествии через червоточину",
  "releaseDate": "2014-10-26",
  "duration": 169
}
```

**Ответ:**
```json
{
  "id": 1,
  "name": "Интерстеллар",
  "description": "Фантастический эпос о путешествии через червоточину",
  "releaseDate": "2014-10-26",
  "duration": 169
}
```

### Создание пользователя

**Запрос:**
```http
POST /users
Content-Type: application/json

{
"email": "user@example.com",
"login": "user123",
"name": "Иван Иванов",
"birthday": "1990-01-01"
}
```

**Ответ**
```json
{
"id": 1,
"email": "user@example.com",
"login": "user123",
"name": "Иван Иванов",
"birthday": "1990-01-01"
}
```

## 🏗 Архитектура проекта

```text
filmorate/

src/
├── main/java/ru/yandex/practicum/filmorate/
│   ├── annotation/              # Кастомные аннотации валидации
│   │   ├── MinReleaseDate.java           → Аннотация проверки даты релиза фильма
│   │   └── MinReleaseDateValidator.java  → Валидатор для аннотации MinReleaseDate
│   ├── controller/              # REST контроллеры
│   │   ├── FilmController.java           → Обработка CRUD операций для фильмов
│   │   └── UserController.java           → Обработка CRUD операций для пользователей
│   ├── exception/               # Кастомные исключения
│   │   ├── DuplicateException.java       → Ошибка дублирования данных
│   │   ├── NotFoundException.java        → Ошибка "не найдено"
│   │   ├── ValidationException.java      → Ошибка валидации
│   │   └── GlobalExceptionHandler.java   → Глобальный обработчик исключений
│   ├── management/              # Слой хранения данных
│   │   ├── FilmStorage.java              → Интерфейс хранилища фильмов
│   │   ├── InMemoryFilmStorage.java      → In-memory реализация для фильмов
│   │   ├── UserStorage.java              → Интерфейс хранилища пользователей
│   │   └── InMemoryUserStorage.java      → In-memory реализация для пользователей
│   ├── model/                   # Модели данных
│   │   ├── Film.java                     → Сущность фильма
│   │   └── User.java                     → Сущность пользователя
│   ├── service/                 # Сервисный слой (бизнес-логика)
│   │   ├── FilmService.java              → Интерфейс сервиса фильмов
│   │   ├── FilmServiceImpl.java          → Реализация сервиса фильмов
│   │   ├── UserService.java              → Интерфейс сервиса пользователей
│   │   └── UserServiceImpl.java          → Реализация сервиса пользователей
│   └── FilmorateApplication.java         → Главный класс Spring Boot приложения
│
├── test/java/ru/yandex/practicum/filmorate/
│   ├── controller/              # Тесты контроллеров
│   │   ├── FilmControllerTest.java       → Тесты FilmController
│   │   └── UserControllerTest.java       → Тесты UserController
│   ├── exception/               # Тесты исключений
│   │   └── GlobalExceptionHandler.java   → Тесты обработки исключений
│   ├── management/              # Тесты хранилища
│   │   ├── InMemoryFilmStorageTest.java  → Тесты хранилища фильмов
│   │   └── InMemoryUserStorageTest.java  → Тесты хранилища пользователей
│   ├── model/                   # Тесты моделей
│   │   ├── FilmValidationTest.java       → Тесты валидации фильмов
│   │   └── UserValidationTest.java       → Тесты валидации пользователей
│   ├── service/                 # Тесты сервисов
│   │   ├── FilmServiceImplTest.java      → Тесты сервиса фильмов
│   │   └── UserServiceImplTest.java      → Тесты сервиса пользователей
│   └── FilmorateApplicationTests.java    → Интеграционные тесты приложения
│
└── README.md                    → Документация проекта
```

## ⚙️ Установка и запуск
### Предварительные требования
- **Java 21 или выше**

- **Maven 3.6 или выше**

### Сборка и запуск
**1. Клонирование репозитория:**
```bash
git clone <repository-url>
cd filmorate
```

2. **Сборка проекта**
```bash
mvn clean package
```

3. **Запуск приложения**
```bash
mvn spring-boot:run
```

4. **Проверка работы приложения**

**Приложение будет доступно по адресу: http://localhost:8080**

### Запуск в IDE
- Импортируйте проект как Maven проект в вашу IDE

- Запустите класс FilmorateApplication

## 🧪 Тестирование
**Для тестирования API можно использовать:**

- Postman - коллекция запросов

- curl - командная строка

- Swagger UI - автоматическая документация API

### Пример тестирования с curl:
**Создание пользователя:**

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "login": "testuser",
    "name": "Test User",
    "birthday": "1995-05-15"
  }'
```

```bash
curl http://localhost:8080/users
```

## 🐛 Обработка ошибок
**Приложение возвращает стандартизированные ответы об ошибках:**

- 400 Bad Request - ошибки валидации

- 404 Not Found - ресурс не найден

- 409 Conflict - конфликт данных (дубликаты)

- 500 Internal Server Error - внутренние ошибки сервера

**Пример ответа с ошибкой:**
```json
{
  "error": "Ошибка валидации",
  "message": "email: Некорректный email"
}
```

## 🧩 Возможные улучшения
- Добавление системы лайков для фильмов

- Добавление функционала друзей для пользователей

- Интеграция с базой данных

- Добавление аутентификации и авторизации

- Добавление кэширования для улучшения производительности

## 📌 Лицензия

Проект лицензирован под [MIT License].

📞 **Контакты**  
Если у вас есть вопросы или предложения, вы можете связаться с автором через:
- **Github:** [Alex Galvinauskas](https://github.com/Alex-Galvinauskas)
- **Telegram:** [https://t.me/Alex_Galvinauskas](https://t.me/Alex_Galvinauskas)