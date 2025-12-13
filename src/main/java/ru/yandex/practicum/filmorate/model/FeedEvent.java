package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedEvent {
    private Long eventId;
    private Long userId;
    private Long actorId;
    private EventType eventType;
    private Operation operation;
    private Long entityId;
    private Instant timestamp;
}