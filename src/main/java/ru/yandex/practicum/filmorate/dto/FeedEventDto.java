package ru.yandex.practicum.filmorate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedEventDto {
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    private Instant timestamp;

    @NotNull
    @JsonProperty("userId")
    private Long userId;

    @NotNull
    @JsonProperty("eventType")
    private EventType eventType;

    @NotNull
    @JsonProperty("operation")
    private Operation operation;

    @NotNull
    @JsonProperty("eventId")
    private Long eventId;

    @NotNull
    @JsonProperty("entityId")
    private Long entityId;
}