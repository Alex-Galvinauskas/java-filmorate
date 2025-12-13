package ru.yandex.practicum.filmorate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class FeedEventDTO {
    private Long timestamp;
    private Long userId;
    private EventType eventType;
    private Operation operation;
    private Long eventId;
    private Long entityId;
}