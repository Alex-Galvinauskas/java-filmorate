-- Очистка таблиц (в порядке удаления для соблюдения внешних ключей)
DELETE FROM film_genres;
DELETE FROM likes;
DELETE FROM films;
DELETE FROM friendships;
DELETE FROM users;

-- Заполнение пользователей
INSERT INTO users (id, email, login, name, birthday) VALUES
(1, 'user1@test.com', 'user1', 'User One', '1990-01-01'),
(2, 'user2@test.com', 'user2', 'User Two', '1995-01-01'),
(3, 'user3@test.com', 'user3', 'User Three', '2000-01-01');

-- Заполнение фильмов (важно: используем ID, соответствующие тестам)
INSERT INTO films (id, name, description, release_date, duration, mpa_rating_id) VALUES
(1, 'Film Updated', 'New film update decription', '1989-04-17', 190, 5),
(2, 'New film', 'New film about friends', '1999-04-30', 120, 3),
(3, 'New film with director', 'Film with director', '1999-12-31', 100, 3);

-- Добавление жанров к фильмам
INSERT INTO film_genres (film_id, genre_id) VALUES
(2, 1),  -- Фильм 2 (ID=2) - Комедия (genre_id=1)
(2, 2),  -- Фильм 2 - Драма (genre_id=2)
(3, 2);  -- Фильм 3 - Драма (genre_id=2)

-- Добавление лайков (для теста популярности)
INSERT INTO likes (film_id, user_id) VALUES
(1, 1), (1, 2), (1, 3),  -- Фильм 1: 3 лайка (самый популярный)
(2, 1), (2, 2),          -- Фильм 2: 2 лайка
(3, 1);                  -- Фильм 3: 1 лайк