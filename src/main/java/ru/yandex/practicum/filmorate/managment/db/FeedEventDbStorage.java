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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Repository
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FeedEventDbStorage {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<FeedEvent> feedEventRowMapper = (rs, rowNum) -> FeedEvent.builder()
            .eventId(rs.getLong("event_id"))
            .userId(rs.getLong("user_id"))
            .actorId(rs.getLong("actor_id"))
            .eventType(EventType.valueOf(rs.getString("event_type")))
            .operation(Operation.valueOf(rs.getString("operation")))
            .entityId(rs.getLong("entity_id"))
            .timestamp(rs.getTimestamp("timestamp").toInstant())
            .build();

    public List<FeedEvent> findFeedEventsByUser(Long userId, int from, int size) {
        String sql = """
    SELECT ufe.* FROM user_feed_events ufe
    WHERE ufe.actor_id = ?  -- Показываем действия, совершенные этим пользователем
       OR ufe.user_id = ?   -- Или действия, предназначенные этому пользователю (от друзей)
    ORDER BY ufe.event_id ASC, ufe.timestamp ASC
    LIMIT ? OFFSET ?
    """;
        return jdbcTemplate.query(sql, feedEventRowMapper, userId, userId, size, from);
    }

    public void save(FeedEvent event) {
        create(event);
    }

    public void create(FeedEvent feedEvent) {
        String sql = "INSERT INTO user_feed_events (user_id, actor_id, event_type, operation, entity_id, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        Instant timestamp = feedEvent.getTimestamp() != null ? feedEvent.getTimestamp() : Instant.now();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"event_id"});
            ps.setLong(1, feedEvent.getUserId());
            ps.setLong(2, feedEvent.getActorId());
            ps.setString(3, feedEvent.getEventType().name());
            ps.setString(4, feedEvent.getOperation().name());
            ps.setLong(5, feedEvent.getEntityId());
            ps.setTimestamp(6, Timestamp.from(timestamp));
            return ps;
        }, keyHolder);

        Long generatedId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        feedEvent.setEventId(generatedId);
        feedEvent.setTimestamp(timestamp);

        log.debug("Создано событие в ленте: {} ({}) для пользователя {}, entityId={}, time={}",
                feedEvent.getEventType(), feedEvent.getOperation(),
                feedEvent.getUserId(), feedEvent.getEntityId(), timestamp);
    }
}