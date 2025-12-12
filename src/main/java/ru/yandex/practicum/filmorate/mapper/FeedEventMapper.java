package ru.yandex.practicum.filmorate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.filmorate.dto.FeedEventDTO;
import ru.yandex.practicum.filmorate.model.FeedEvent;

@Mapper(componentModel = "spring")
public interface FeedEventMapper {

    @Mapping(target = "timestamp", expression = "java(feedEvent.getTimestamp().toEpochMilli())")
    @Mapping(target = "userId", source = "actorId")
    FeedEventDTO toDTO(FeedEvent feedEvent);

    @Mapping(target = "timestamp", expression = "java(java.time.Instant.ofEpochMilli(dto.getTimestamp()))")
    @Mapping(target = "actorId", source = "userId")
    FeedEvent toEntity(FeedEventDTO dto);
}