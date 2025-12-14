package ru.yandex.practicum.filmorate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.yandex.practicum.filmorate.dto.FeedEventDTO;
import ru.yandex.practicum.filmorate.model.FeedEvent;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

@Mapper(componentModel = "spring")
public interface FeedEventMapper {

    @Mapping(target = "timestamp", expression = "java(feedEvent.getTimestamp().toEpochMilli())")
    @Mapping(target = "eventType", source = "eventType", qualifiedByName = "eventTypeToString")
    @Mapping(target = "operation", source = "operation", qualifiedByName = "operationToString")
    @Mapping(target = "actorId", source = "actorId")
    FeedEventDTO toDTO(FeedEvent feedEvent);

    @Mapping(target = "timestamp", expression = "java(java.time.Instant.ofEpochMilli(dto.getTimestamp()))")
    @Mapping(target = "eventType", source = "eventType", qualifiedByName = "stringToEventType")
    @Mapping(target = "operation", source = "operation", qualifiedByName = "stringToOperation")
    @Mapping(target = "actorId", source = "actorId")
    FeedEvent toEntity(FeedEventDTO dto);

    @Named("eventTypeToString")
    default String eventTypeToString(EventType eventType) {
        return eventType != null ? eventType.name() : null;
    }

    @Named("operationToString")
    default String operationToString(Operation operation) {
        return operation != null ? operation.name() : null;
    }

    @Named("stringToEventType")
    default EventType stringToEventType(String eventType) {
        return eventType != null ? EventType.valueOf(eventType) : null;
    }

    @Named("stringToOperation")
    default Operation stringToOperation(String operation) {
        return operation != null ? Operation.valueOf(operation) : null;
    }
}