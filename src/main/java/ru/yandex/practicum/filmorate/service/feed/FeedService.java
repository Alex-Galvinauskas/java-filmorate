package ru.yandex.practicum.filmorate.service.feed;

import ru.yandex.practicum.filmorate.dto.FeedEventDto;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

import java.util.List;

public interface FeedService {

    void recordEvent(Long userId, Long actorId, EventType eventType,
                     Operation operation, Long entityId);

    List<FeedEventDto> getUserFeed(Long userId, Integer from, Integer size);
}