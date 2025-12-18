package ru.yandex.practicum.filmorate.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dto.FeedEventDto;
import ru.yandex.practicum.filmorate.model.FeedEvent;

@Component
public class FeedEventMapper {

    public FeedEventDto toDto(FeedEvent event) {
        if (event == null) {
            return null;
        }

        return FeedEventDto.builder()
                .timestamp(event.getTimestamp())
                .userId(event.getUserId())
                .eventType(event.getEventType())
                .operation(event.getOperation())
                .eventId(event.getEventId())
                .entityId(event.getEntityId())
                .build();
    }

    public FeedEvent toEntity(FeedEventDto dto) {
        if (dto == null) {
            return null;
        }

        return FeedEvent.builder()
                .eventId(dto.getEventId())
                .userId(dto.getUserId())
                .timestamp(dto.getTimestamp())
                .eventType(dto.getEventType())
                .operation(dto.getOperation())
                .entityId(dto.getEntityId())
                .build();
    }
}