package ru.yandex.practicum.filmorate.managment.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.FeedEvent;
import ru.yandex.practicum.filmorate.model.Operation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Repository
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FeedDbStorage {

    private final JdbcTemplate jdbcTemplate;

    public FeedEvent create(FeedEvent feedEvent) {
        String sql = "INSERT INTO user_feed_events (user_id, actor_id, event_type, operation, entity_id, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        Instant timestamp = feedEvent.getTimestamp() != null ? feedEvent.getTimestamp() : Instant.now();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"event_id"});
            stmt.setLong(1, feedEvent.getUserId());
            stmt.setLong(2, feedEvent.getActorId());
            stmt.setString(3, feedEvent.getEventType().name());
            stmt.setString(4, feedEvent.getOperation().name());
            stmt.setLong(5, feedEvent.getEntityId());
            stmt.setTimestamp(6, Timestamp.from(timestamp));
            return stmt;
        }, keyHolder);

        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        feedEvent.setEventId(id);
        feedEvent.setTimestamp(timestamp);

        log.debug("Создано событие в ленте: {} ({}) для пользователя {}, entityId={}, time={}",
                feedEvent.getEventType(), feedEvent.getOperation(),
                feedEvent.getUserId(), feedEvent.getEntityId(), timestamp);
        return feedEvent;
    }

    public List<FeedEvent> findByUserId(Long userId, Integer limit) {
        String sql = "SELECT * FROM user_feed_events WHERE user_id = ? " +
                "ORDER BY event_id ASC LIMIT ?";

        log.debug("Поиск событий для пользователя {} с лимитом {}", userId, limit);

        List<FeedEvent> events = jdbcTemplate.query(sql, new FeedEventRowMapper(), userId, limit);

        log.debug("Найдено {} событий для пользователя {}:", events.size(), userId);
        for (int i = 0; i < events.size(); i++) {
            FeedEvent event = events.get(i);
            log.debug("  [{}/{}] eventType={}, operation={}, entityId={}, timestamp={}, eventId={}",
                    i + 1, events.size(),
                    event.getEventType(), event.getOperation(),
                    event.getEntityId(), event.getTimestamp(), event.getEventId());
        }

        return events;
    }

    private static class FeedEventRowMapper implements RowMapper<FeedEvent> {
        @Override
        public FeedEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return FeedEvent.builder()
                    .eventId(rs.getLong("event_id"))
                    .userId(rs.getLong("user_id"))
                    .actorId(rs.getLong("actor_id"))
                    .eventType(EventType.valueOf(rs.getString("event_type")))
                    .operation(Operation.valueOf(rs.getString("operation")))
                    .entityId(rs.getLong("entity_id"))
                    .timestamp(rs.getTimestamp("timestamp").toInstant())
                    .build();
        }
    }
}