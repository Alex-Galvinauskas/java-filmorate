package ru.yandex.practicum.filmorate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class FeedEventDTO {
    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("actorId")
    private Long actorId;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("operation")
    private String operation;

    @JsonProperty("eventId")
    private Long eventId;

    @JsonProperty("entityId")
    private Long entityId;
}