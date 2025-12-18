package ru.yandex.practicum.filmorate.managment.db;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.FeedEvent;
import ru.yandex.practicum.filmorate.model.Operation;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
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
            SELECT * FROM user_feed_events
            WHERE user_id = ?
            ORDER BY event_id ASC
            LIMIT ? OFFSET ?
            """;
        return jdbcTemplate.query(sql, feedEventRowMapper, userId, size, from);
    }

    public Integer countFeedEventsByUser(Long userId) {
        String sql = "SELECT COUNT(*) FROM user_feed_events WHERE user_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId);
    }

    public FeedEvent save(FeedEvent event) {
        String sql = """
            INSERT INTO user_feed_events
            (user_id, actor_id, event_type, operation, entity_id, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"event_id"});
            ps.setLong(1, event.getUserId());
            ps.setLong(2, event.getActorId());
            ps.setString(3, event.getEventType().name());
            ps.setString(4, event.getOperation().name());
            ps.setLong(5, event.getEntityId());
            ps.setTimestamp(6, Timestamp.from(event.getTimestamp()));
            return ps;
        }, keyHolder);

        Long generatedId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        event.setEventId(generatedId);
        return event;
    }

    public List<Long> getFriendIds(Long userId) {
        String sql = """
            SELECT friend_id FROM friendships
            WHERE user_id = ? AND status = 'CONFIRMED'
            UNION
            SELECT user_id FROM friendships
            WHERE friend_id = ? AND status = 'CONFIRMED'
            """;
        return jdbcTemplate.queryForList(sql, Long.class, userId, userId);
    }
}