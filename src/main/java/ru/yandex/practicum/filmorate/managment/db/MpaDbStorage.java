package ru.yandex.practicum.filmorate.managment.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class MpaDbStorage {

    private final JdbcTemplate jdbcTemplate;

    public List<Mpa> getAllMpa() {
        String sql = "SELECT * FROM mpa_ratings ORDER BY id";
        log.debug("Получение всех рейтингов MPA");
        return jdbcTemplate.query(sql, new MpaRowMapper());
    }

    public Optional<Mpa> getMpaById(Integer id) {
        String sql = "SELECT * FROM mpa_ratings WHERE id = ?";
        log.debug("Поиск рейтинга MPA по ID: {}", id);
        List<Mpa> result = jdbcTemplate.query(sql, new MpaRowMapper(), id);
        return result.stream().findFirst();
    }

    private static class MpaRowMapper implements RowMapper<Mpa> {
        @Override
        public Mpa mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Mpa.builder()
                    .id(rs.getInt("id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .build();
        }
    }
}