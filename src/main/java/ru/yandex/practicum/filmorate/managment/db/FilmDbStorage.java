package ru.yandex.practicum.filmorate.managment.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.managment.inMemory.FilmStorage;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Primary
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final GenreDbStorage genreDbStorage;
    private final DirectorDbStorage directorDbStorage;

    @Override
    public Film createFilm(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            LocalDate releaseDate = film.getReleaseDate() != null ? film.getReleaseDate() : LocalDate.now();
            stmt.setDate(3, Date.valueOf(releaseDate));
            stmt.setInt(4, film.getDuration());
            long mpaId = (film.getMpa() != null && film.getMpa().getId() != null) ? film.getMpa().getId() : 1L;
            stmt.setLong(5, mpaId);

            return stmt;
        }, keyHolder);

        Long filmId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        film.setId(filmId);

        saveFilmGenres(film);
        saveFilmDirectors(film);

        log.info("Создан новый фильм в БД: '{}' (ID: {})", film.getName(), filmId);
        return getFilmById(filmId).orElse(film);
    }

    @Override
    public List<Film> getAllFilms() {
        String sql = "SELECT f.*, m.name as mpa_name, m.description as mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                "ORDER BY f.id";

        log.debug("Получение всех фильмов из БД");
        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper());

        // Загружаем все связанные данные одним запросом
        enrichFilmsWithRelatedData(films);

        log.debug("Загружено {} фильмов с жанрами и режиссерами", films.size());
        return films;
    }

    @Override
    public Optional<Film> getFilmById(Long id) {
        String sql = "SELECT f.*, m.name as mpa_name, m.description as mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                "WHERE f.id = ?";

        log.debug("Поиск фильма по ID: {}", id);
        List<Film> result = jdbcTemplate.query(sql, new FilmRowMapper(), id);

        if (result.isEmpty()) {
            return Optional.empty();
        }

        Film film = result.getFirst();
        List<Film> singleFilmList = Collections.singletonList(film);
        enrichFilmsWithRelatedData(singleFilmList);

        log.debug("Найден фильм: '{}' (ID: {}) с {} жанрами",
                film.getName(), film.getId(), film.getGenres().size());
        return Optional.of(film);
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, " +
                "duration = ?, mpa_rating_id = ? WHERE id = ?";

        LocalDate releaseDate = film.getReleaseDate() != null ? film.getReleaseDate() : LocalDate.now();

        int updated = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(releaseDate),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        if (updated == 0) {
            throw new RuntimeException("Фильм с ID " + film.getId() + " не найден");
        }

        updateFilmGenres(film);
        updateFilmDirectors(film);

        log.info("Обновлен фильм в БД: '{}' (ID: {})", film.getName(), film.getId());
        return getFilmById(film.getId()).orElse(film);
    }

    @Override
    public boolean existsFilmById(Long id) {
        String sql = "SELECT COUNT(*) FROM films WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public boolean existsFilmByNameAndReleaseYear(String name, Integer releaseYear) {
        String sql = "SELECT COUNT(*) FROM films WHERE LOWER(name) = LOWER(?) AND YEAR(release_date) = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                name.toLowerCase(), releaseYear);
        return count != null && count > 0;
    }

    public void addLike(long filmId, long userId) {
        String sql = "MERGE INTO likes (film_id, user_id) KEY(film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
        log.debug("Добавлен лайк фильму {} от пользователя {}", filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
        log.debug("Удален лайк фильму {} от пользователя {}", filmId, userId);
    }

    public Set<Long> getLikesByFilmId(Long filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        List<Long> likes = jdbcTemplate.queryForList(sql, Long.class, filmId);
        return new HashSet<>(likes);
    }

    public List<Film> getFilmsViaSearchByName(String query) {
        String sql = "SELECT f.*, m.name as mpa_name, m.description as mpa_description, " +
                "COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "WHERE lower(f.name) LIKE ? " +
                "GROUP BY f.id, m.id, m.name, m.description " +
                "ORDER BY COUNT(l.user_id) DESC";

        log.debug("Получение популярных фильмов по части названия: {}", query);
        String searchPattern = "%" + query.toLowerCase() + "%";
        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), searchPattern);

        enrichFilmsWithRelatedData(films);

        return films;
    }

    public List<Film> getFilmsViaSearchByDirector(String query) {
        String sql = "SELECT f.*, m.name as mpa_name, m.description as mpa_description, " +
                "COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "LEFT JOIN film_directors fd ON f.id = fd.film_id " +
                "JOIN directors d ON fd.director_id = d.id " +
                "WHERE lower(d.name) LIKE ? " +
                "GROUP BY f.id, m.id, m.name, m.description " +
                "ORDER BY COUNT(l.user_id) DESC";

        log.debug("Получение популярных фильмов по части имени режиссера: {}", query);
        String searchPattern = "%" + query.toLowerCase() + "%";
        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), searchPattern);

        enrichFilmsWithRelatedData(films);

        return films;
    }

    private void saveFilmGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            log.debug("Жанры для фильма {} не указаны или пусты", film.getId());
            return;
        }

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        List<Object[]> batchArgs = film.getGenres().stream()
                .distinct()
                .map(genre -> new Object[]{film.getId(), genre.getId()})
                .collect(Collectors.toList());

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
            log.debug("Сохранены {} жанров для фильма {}: {}",
                    batchArgs.size(), film.getId(),
                    film.getGenres().stream().map(Genre::getId).collect(Collectors.toList()));
        }
    }

    private void updateFilmGenres(Film film) {
        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId());

        saveFilmGenres(film);
        log.debug("Обновлены жанры для фильма {}", film.getId());
    }

    private static class FilmRowMapper implements RowMapper<Film> {
        @Override
        public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
            Mpa mpa = Mpa.builder()
                    .id(rs.getLong("mpa_rating_id"))
                    .name(rs.getString("mpa_name"))
                    .description(rs.getString("mpa_description"))
                    .build();

            LocalDate releaseDate = rs.getDate("release_date").toLocalDate();

            return Film.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .releaseDate(releaseDate)
                    .duration(rs.getInt("duration"))
                    .mpa(mpa)
                    .likes(new HashSet<>())
                    .genres(new ArrayList<>())
                    .directors(new ArrayList<>())
                    .build();
        }
    }

    @Override
    public boolean existsFilmByNameAndReleaseYearExcludingId(String name, Integer releaseYear,
                                                             Long excludedId) {
        String sql = """
            SELECT COUNT(*)
            FROM films f
            WHERE LOWER(f.name) = LOWER(?)
              AND YEAR(f.release_date) = ?
              AND f.id != ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM film_directors fd1
                  JOIN film_directors fd2 ON fd1.director_id = fd2.director_id
                  WHERE fd1.film_id = f.id
                    AND fd2.film_id = ?
              )
            """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                name.toLowerCase(), releaseYear, excludedId, excludedId);
        return count != null && count > 0;
    }

    @Override
    public void deleteFilm(Long filmId) {
        log.debug("Удаление фильма с ID: {} из БД", filmId);

        if (!existsFilmById(filmId)) {
            log.warn("Попытка удаления несуществующего фильма с ID: {}", filmId);
            throw new RuntimeException("Фильм с ID " + filmId + " не найден");
        }

        removeAllLikesByFilmId(filmId);

        String deleteGenresSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteGenresSql, filmId);

        String deleteFilmSql = "DELETE FROM films WHERE id = ?";
        int rowsDeleted = jdbcTemplate.update(deleteFilmSql, filmId);

        if (rowsDeleted > 0) {
            log.info("Фильм с ID {} успешно удален", filmId);
        } else {
            log.warn("Фильм с ID {} не был удален", filmId);
        }
    }

    public void removeAllLikesByFilmId(Long filmId) {
        String sql = "DELETE FROM likes WHERE film_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, filmId);
        log.debug("Удалено {} лайков для фильма с ID: {}", rowsDeleted, filmId);
    }

    /**
     * Получает лайки всех пользователей
     */
    public Map<Long, Set<Long>> getLikesByUsers() {
        String sql = "SELECT user_id, film_id FROM likes";
        Map<Long, Set<Long>> likesByUser = new HashMap<>();

        jdbcTemplate.query(sql, (rs) -> {
            Long userId = rs.getLong("user_id");
            Long filmId = rs.getLong("film_id");
            likesByUser.computeIfAbsent(userId, k -> new HashSet<>()).add(filmId);
        });

        log.debug("Получены лайки для {} пользователей", likesByUser.size());
        return likesByUser;
    }

    public List<Film> getFilmsByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        String sql = "SELECT f.*, m.name as mpa_name, m.description as mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                "WHERE f.id IN (" +
                ids.stream().map(id -> "?").collect(Collectors.joining(",")) +
                ")";

        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), ids.toArray());
        enrichFilmsWithRelatedData(films);

        log.debug("Получено {} фильмов по ID", films.size());
        return films;
    }

    public List<Film> getRecommendationsForUser(Long userId, int limit) {
        String sql =
                "WITH user_likes AS (" +
                        "    SELECT film_id FROM likes WHERE user_id = ?" +
                        "), " +
                        "similar_users AS (" +
                        "    SELECT l2.user_id, COUNT(DISTINCT l2.film_id) AS common_likes " +
                        "    FROM likes l1 " +
                        "    JOIN likes l2 ON l1.film_id = l2.film_id AND l1.user_id != l2.user_id " +
                        "    WHERE l1.user_id = ? " +
                        "    GROUP BY l2.user_id " +
                        "    ORDER BY common_likes DESC " +
                        "    LIMIT 5" +
                        "), " +
                        "recommended_films AS (" +
                        "    SELECT DISTINCT l.film_id " +
                        "    FROM similar_users su " +
                        "    JOIN likes l ON su.user_id = l.user_id " +
                        "    WHERE l.film_id NOT IN (SELECT film_id FROM user_likes) " +
                        ") " +
                        "SELECT f.*, m.name as mpa_name, m.description as mpa_description " +
                        "FROM films f " +
                        "JOIN recommended_films rf ON f.id = rf.film_id " +
                        "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                        "ORDER BY (" +
                        "    SELECT COUNT(*) FROM likes l WHERE l.film_id = f.id" +
                        ") DESC " +
                        "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), userId, userId, limit);
        enrichFilmsWithRelatedData(films);

        log.debug("Получено {} рекомендаций для пользователя {}", films.size(), userId);
        return films;
    }

    private void saveFilmDirectors(Film film) {
        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            String sql = "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";
            Set<Long> uniqueDirectorIds = film.getDirectors().stream()
                    .filter(Objects::nonNull)
                    .map(Director::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<Object[]> batchArgs = uniqueDirectorIds.stream()
                    .map(directorId -> new Object[]{film.getId(), directorId})
                    .collect(Collectors.toList());

            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                log.debug("Сохранено {} режиссеров для фильма {}: {}",
                        batchArgs.size(), film.getId(), uniqueDirectorIds);
            }
        }
    }

    private void updateFilmDirectors(Film film) {
        String deleteSql = "DELETE FROM film_directors WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId());

        saveFilmDirectors(film);
    }

    public List<Film> getFilmsByDirector(Long directorId, String sortBy) {
        String sql = buildDirectorFilmsQuery(sortBy);

        log.debug("SQL запрос для режиссера {}: {}", directorId, sql);

        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), directorId);
        log.debug("Найдено фильмов для режиссера {}: {}", directorId, films.size());

        enrichFilmsWithRelatedData(films);

        return films;
    }

    private String buildDirectorFilmsQuery(String sortBy) {
        if ("year".equalsIgnoreCase(sortBy)) {
            return """
                    SELECT f.*, m.name as mpa_name, m.description as mpa_description
                    FROM films f
                    LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id
                    JOIN film_directors fd ON f.id = fd.film_id
                    WHERE fd.director_id = ?
                    ORDER BY f.release_date
                    """;
        } else if ("likes".equalsIgnoreCase(sortBy)) {
            return """
        SELECT f.*, m.name as mpa_name, m.description as mpa_description
        FROM films f
        LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id
        JOIN film_directors fd ON f.id = fd.film_id
        WHERE fd.director_id = ?
        ORDER BY (
            SELECT COUNT(*) FROM likes l WHERE l.film_id = f.id
        ) DESC, f.id
        """;
        } else {
            return """
                    SELECT f.*, m.name as mpa_name, m.description as mpa_description
                    FROM films f
                    LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id
                    JOIN film_directors fd ON f.id = fd.film_id
                    WHERE fd.director_id = ?
                    ORDER BY f.id
                    """;
        }
    }

    public List<Film> getPopularFilms(int count, Integer genreId, Integer year) {
        StringBuilder sql = new StringBuilder(
                "SELECT f.*, m.name as mpa_name, m.description as mpa_description, " +
                        "COUNT(l.user_id) as likes_count " +
                        "FROM films f " +
                        "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                        "LEFT JOIN likes l ON f.id = l.film_id "
        );

        List<Object> params = new ArrayList<>();

        if (genreId != null) {
            sql.append("JOIN film_genres fg ON f.id = fg.film_id ");
        }

        sql.append("WHERE 1=1 ");

        if (genreId != null) {
            sql.append("AND fg.genre_id = ? ");
            params.add(genreId);
        }

        if (year != null) {
            sql.append("AND YEAR(f.release_date) = ? ");
            params.add(year);
        }

        sql.append("GROUP BY f.id, m.id, m.name, m.description ");
        sql.append("ORDER BY COUNT(l.user_id) DESC ");
        sql.append("LIMIT ?");
        params.add(count);

        log.debug("Выполнение SQL запроса для популярных фильмов: {}", sql);
        log.debug("Параметры: genreId={}, year={}, count={}", genreId, year, count);

        List<Film> films = jdbcTemplate.query(sql.toString(), new FilmRowMapper(),
                params.toArray());

        enrichFilmsWithRelatedData(films);

        return films;
    }

    public List<Film> getCommonFilms(Long userId, Long friendId) {
        String sql = "WITH user_likes AS ( " +
                "    SELECT film_id FROM likes WHERE user_id = ? " +
                "), friend_likes AS ( " +
                "    SELECT film_id FROM likes WHERE user_id = ? " +
                "), common_films AS ( " +
                "    SELECT ul.film_id FROM user_likes ul " +
                "    INNER JOIN friend_likes fl ON ul.film_id = fl.film_id " +
                ") " +
                "SELECT f.*, m.name as mpa_name, m.description as mpa_description, " +
                "       COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "WHERE f.id IN (SELECT film_id FROM common_films) " +
                "GROUP BY f.id, m.id, m.name, m.description " +  // Добавлен m.id
                "ORDER BY COUNT(l.user_id) DESC";

        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), userId, friendId);
        enrichFilmsWithRelatedData(films);

        return films;
    }

    /**
     * Метод для загрузки связанных данных (жанры, режиссеры, лайки) для списка фильмов
     * одним запросом, что решает проблему N+1
     */
    private void enrichFilmsWithRelatedData(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        Set<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toSet());

        // Загружаем жанры для всех фильмов одним запросом
        Map<Long, List<Genre>> genresByFilmId = loadGenresForFilms(filmIds);

        // Загружаем режиссеров для всех фильмов одним запросом
        Map<Long, List<Director>> directorsByFilmId = loadDirectorsForFilms(filmIds);

        // Загружаем лайки для всех фильмов одним запросом
        Map<Long, Set<Long>> likesByFilmId = loadLikesForFilms(filmIds);

        // Наполняем фильмы данными
        for (Film film : films) {
            Long filmId = film.getId();
            film.setGenres(genresByFilmId.getOrDefault(filmId, new ArrayList<>()));
            film.setDirectors(directorsByFilmId.getOrDefault(filmId, new ArrayList<>()));
            film.setLikes(likesByFilmId.getOrDefault(filmId, new HashSet<>()));
        }
    }

    /**
     * Загружает жанры для указанных фильмов одним запросом
     */
    private Map<Long, List<Genre>> loadGenresForFilms(Set<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return new HashMap<>();
        }

        String placeholders = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT fg.film_id, g.id, g.name " +
                "FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.id " +
                "WHERE fg.film_id IN (" + placeholders + ") " +
                "ORDER BY g.id";

        Map<Long, List<Genre>> result = new HashMap<>();

        jdbcTemplate.query(sql, filmIds.toArray(), rs -> {
            Long filmId = rs.getLong("film_id");
            Genre genre = Genre.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .build();

            result.computeIfAbsent(filmId, k -> new ArrayList<>()).add(genre);
        });

        return result;
    }

    /**
     * Загружает режиссеров для указанных фильмов одним запросом
     */
    private Map<Long, List<Director>> loadDirectorsForFilms(Set<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return new HashMap<>();
        }

        String placeholders = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT fd.film_id, d.id, d.name " +
                "FROM film_directors fd " +
                "JOIN directors d ON fd.director_id = d.id " +
                "WHERE fd.film_id IN (" + placeholders + ") " +
                "ORDER BY d.id";

        Map<Long, List<Director>> result = new HashMap<>();

        jdbcTemplate.query(sql, filmIds.toArray(), rs -> {
            Long filmId = rs.getLong("film_id");
            Director director = Director.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .build();

            result.computeIfAbsent(filmId, k -> new ArrayList<>()).add(director);
        });

        return result;
    }

    /**
     * Загружает лайки для указанных фильмов одним запросом
     */
    private Map<Long, Set<Long>> loadLikesForFilms(Set<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return new HashMap<>();
        }

        String placeholders = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT film_id, user_id FROM likes " +
                "WHERE film_id IN (" + placeholders + ")";

        Map<Long, Set<Long>> result = new HashMap<>();

        jdbcTemplate.query(sql, filmIds.toArray(), rs -> {
            Long filmId = rs.getLong("film_id");
            Long userId = rs.getLong("user_id");

            result.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
        });

        return result;
    }
}