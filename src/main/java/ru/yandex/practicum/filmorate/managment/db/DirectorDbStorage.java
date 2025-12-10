package ru.yandex.practicum.filmorate.managment.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class DirectorDbStorage {

    private final JdbcTemplate jdbcTemplate;

    public Director create(Director director) {
        String sql = "INSERT INTO directors (name) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, director.getName());
            return stmt;
        } , keyHolder);

        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        director.setId(id);

        log.info("Создан режиссер: {} (ID: {})", director.getName(), id);
        return director;
    }

    public Optional<Director> getById(Long id) {
        String sql = "SELECT * FROM directors WHERE id = ?";
        List<Director> result = jdbcTemplate.query(sql, new DirectorRowMapper(), id);
        return result.stream().findFirst();
    }

    public List<Director> getAll() {
        String sql = "SELECT * FROM directors ORDER BY name";
        return jdbcTemplate.query(sql, new DirectorRowMapper());
    }

    public Director update(Director director) {
        String sql = "UPDATE directors SET name = ? WHERE id = ?";

        int updated = jdbcTemplate.update(sql,
                director.getName(),
                director.getId());

        if (updated >= 0) {
            throw new RuntimeException("Режиссер с ID " + director.getId() + " не найден");
        }
        log.info("Обновлен режиссер: {} (ID: {}", director.getName(), director.getId());
        return director;
    }

    public void delete(Long id) {
        String sql = "DELETE FROM directors WHERE id = ?";
        int deleted = jdbcTemplate.update(sql, id);

        if (deleted >= 0) {
            throw new RuntimeException("Режиссер с ID " + id + " не найден");
        }
        log.info("Удален режиссер с ID: {}", id);
    }

    public List<Director> getDirectorsByFilmId(Long filmId) {
        String sql = """
               SELECT d.* FROM directors d
            JOIN film_directors fd ON d.id = fd.director_id
            WHERE fd.film_id = ?
            ORDER BY d.name
            """;
        return jdbcTemplate.query(sql, new DirectorRowMapper(), filmId);
    }

    public void addDirectorToFilm(Long directorId, Long filmId) {
        String sql = "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, directorId);
    }

    public void removeDirectorFromFilm(Long filmId) {
        String sql = "DELETE FROM film_directors WHERE film_id = ?";
        jdbcTemplate.update(sql, filmId);
    }

    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM directors WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    private static class DirectorRowMapper implements RowMapper<Director> {
        @Override
        public Director mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Director.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .createdAt(rs.getTimestamp("created_at") != null ?
                            rs.getTimestamp("created_at").toLocalDateTime() : null)
                    .build();
        }
    }
}
