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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Primary
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final GenreDbStorage genreDbStorage;
    private final MpaDbStorage mpaDbStorage;
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
        String sql = """
    SELECT
        f.*,
        m.name as mpa_name,
        m.description as mpa_description,
        GROUP_CONCAT(DISTINCT g.id ORDER BY g.id) as genre_ids,
        GROUP_CONCAT(DISTINCT g.name ORDER BY g.id) as genre_names,
        GROUP_CONCAT(DISTINCT d.id) as director_ids,
        GROUP_CONCAT(DISTINCT d.name) as director_names
    FROM films f
    LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id
    LEFT JOIN film_genres fg ON f.id = fg.film_id
    LEFT JOIN genres g ON fg.genre_id = g.id
    LEFT JOIN film_directors fd ON f.id = fd.film_id
    LEFT JOIN directors d ON fd.director_id = d.id
    GROUP BY f.id, m.name, m.description
    ORDER BY f.id
    """;

        return jdbcTemplate.query(sql, new FilmRowMapper());
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
        film.setGenres(genreDbStorage.getGenresByFilmId(film.getId()));
        film.setDirectors(directorDbStorage.getDirectorsByFilmId(film.getId()));
        film.setLikes(getLikesByFilmId(film.getId()));

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

    public void addLike(Long filmId, Long userId) {
        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
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

    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.*, m.name as mpa_name, m.description as mpa_description, " +
                "COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "GROUP BY f.id, m.name, m.description " +
                "ORDER BY likes_count DESC " +
                "LIMIT ?";

        log.debug("Получение {} популярных фильмов", count);
        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), count);

        return films.stream()
                .peek(film -> {
                    film.setGenres(genreDbStorage.getGenresByFilmId(film.getId()));
                    film.setDirectors(directorDbStorage.getDirectorsByFilmId(film.getId()));
                })
                .collect(Collectors.toList());
    }

    private void saveFilmGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            log.debug("Жанры для фильма {} не указаны или пусты", film.getId());
            return;
        }

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        List<Object[]> batchArgs = film.getGenres().stream()
                .map(genre -> new Object[]{film.getId(), genre.getId()})
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(sql, batchArgs);
        log.debug("Сохранены {} жанров для фильма {}: {}",
                batchArgs.size(), film.getId(),
                film.getGenres().stream().map(Genre::getId).collect(Collectors.toList()));
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
              WHERE fd1.film_id = f.id
                AND fd1.director_id IN (
                    SELECT fd2.director_id
                    FROM film_directors fd2
                    WHERE fd2.film_id = ?
                )
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

    /**
     * Получает лайки пользователей с ограничением по количеству
     */
    public Map<Long, Set<Long>> getRecentLikesByUsers(int limit) {
        String sql = "SELECT user_id, film_id FROM likes " +
                "ORDER BY created_at DESC LIMIT ?";

        Map<Long, Set<Long>> likesByUser = new HashMap<>();

        jdbcTemplate.query(sql, (rs) -> {
            Long userId = rs.getLong("user_id");
            Long filmId = rs.getLong("film_id");
            likesByUser.computeIfAbsent(userId, k -> new HashSet<>()).add(filmId);
        }, limit);

        log.debug("Получены последние {} лайков для {} пользователей", limit, likesByUser.size());
        return likesByUser;
    }

    public Map<Long, Set<Long>> getLikesByUsersSince(LocalDateTime since) {
        String sql = "SELECT user_id, film_id FROM likes " +
                "WHERE created_at >= ? " +
                "ORDER BY created_at DESC";

        Map<Long, Set<Long>> likesByUser = new HashMap<>();

        jdbcTemplate.query(sql, (rs) -> {
            Long userId = rs.getLong("user_id");
            Long filmId = rs.getLong("film_id");
            likesByUser.computeIfAbsent(userId, k -> new HashSet<>()).add(filmId);
        }, Timestamp.valueOf(since));

        log.debug("Получены лайки с {} для {} пользователей", since, likesByUser.size());
        return likesByUser;
    }

    /**
     * Получает фильмы по списку ID
     */
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
        for (Film film : films) {
            film.setGenres(genreDbStorage.getGenresByFilmId(film.getId()));
            film.setDirectors(directorDbStorage.getDirectorsByFilmId(film.getId()));
            film.setLikes(getLikesByFilmId(film.getId()));
        }

        log.debug("Получено {} фильмов по ID", films.size());
        return films;
    }

    /**
     * Получает рекомендации для пользователя одним запросом
     */
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
        for (Film film : films) {
            film.setGenres(genreDbStorage.getGenresByFilmId(film.getId()));
            film.setDirectors(directorDbStorage.getDirectorsByFilmId(film.getId()));
            film.setLikes(getLikesByFilmId(film.getId()));
        }

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

        return films.stream()
                .peek(film -> {
                    film.setGenres(genreDbStorage.getGenresByFilmId(film.getId()));
                    film.setDirectors(directorDbStorage.getDirectorsByFilmId(film.getId()));
                    film.setLikes(getLikesByFilmId(film.getId()));
                })
                .collect(Collectors.toList());
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
        SELECT f.*, m.name as mpa_name, m.description as mpa_description,
               COALESCE(l.likes_count, 0) as likes_count
        FROM films f
        LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id
        JOIN film_directors fd ON f.id = fd.film_id
        LEFT JOIN (
            SELECT film_id, COUNT(user_id) as likes_count
            FROM likes
            GROUP BY film_id
        ) l ON f.id = l.film_id
        WHERE fd.director_id = ?
        ORDER BY COALESCE(l.likes_count, 0) DESC, f.id
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

        sql.append("GROUP BY f.id, m.name, m.description ");
        sql.append("ORDER BY likes_count DESC ");
        sql.append("LIMIT ?");
        params.add(count);

        log.debug("Выполнение SQL запроса для популярных фильмов: {}", sql);
        log.debug("Параметры: genreId={}, year={}, count={}", genreId, year, count);

        List<Film> films = jdbcTemplate.query(sql.toString(), new FilmRowMapper(), params.toArray());

        return films.stream()
                .peek(film -> {
                    film.setGenres(genreDbStorage.getGenresByFilmId(film.getId()));
                    film.setDirectors(directorDbStorage.getDirectorsByFilmId(film.getId()));
                })
                .collect(Collectors.toList());
    }
}