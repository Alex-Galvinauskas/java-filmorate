package ru.yandex.practicum.filmorate.managment.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.managment.inMemory.FilmStorage;
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
        String sql = "SELECT f.*, m.name as mpa_name, m.description as mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                "ORDER BY f.id";

        log.debug("Получение всех фильмов из БД");
        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper());

        return films.stream()
                .peek(film -> film.setGenres(genreDbStorage.getGenresByFilmId(film.getId())))
                .collect(Collectors.toList());
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
                .peek(film -> film.setGenres(genreDbStorage.getGenresByFilmId(film.getId())))
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
                    .build();
        }
    }

    @Override
    public boolean existsFilmByNameAndReleaseYearExcludingId(String name, Integer releaseYear,
                                                             Long excludedId) {
        String sql = "SELECT COUNT(*) FROM films WHERE LOWER(name) = LOWER(?) AND YEAR(release_date) = ? AND id != ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                name.toLowerCase(), releaseYear, excludedId);
        return count != null && count > 0;
    }

    private void saveFilmDirectors(Film film) {
        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            String sql = "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";
            List<Object[]> batchArgs = film.getDirectors().stream()
                    .map(director -> new Object[]{film.getId(), director.getId()})
                    .collect(Collectors.toList());
            jdbcTemplate.batchUpdate(sql, batchArgs);
            log.debug("Сохранено {} режиссеров для фильма {}", batchArgs.size(), film.getId());
        }
    }

    private void updateFilmDirectors(Film film) {
        directorDbStorage.removeDirectorFromFilm(film.getId());
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
        StringBuilder sql = new StringBuilder("""
            SELECT f.*, m.name as mpa_name, m.description as mpa_description
            FROM films f
            LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id
            JOIN film_directors fd ON f.id = fd.film_id
            WHERE fd.director_id = ?
            """);

        if ("year".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY f.release_date");
        } else if ("likes".equalsIgnoreCase(sortBy)) {
            sql.append("""
                ORDER BY (
                    SELECT COUNT(*) FROM likes l
                    WHERE l.film_id = f.id
                ) DESC
                """);
        } else {
            sql.append(" ORDER BY f.id");
        }

        return sql.toString();
    }
}