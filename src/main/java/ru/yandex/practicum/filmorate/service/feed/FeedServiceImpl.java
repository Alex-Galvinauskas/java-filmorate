package ru.yandex.practicum.filmorate.service.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dto.FeedEventDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.FeedEventDbStorage;
import ru.yandex.practicum.filmorate.managment.db.UserDbStorage;
import ru.yandex.practicum.filmorate.mapper.FeedEventMapper;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.FeedEvent;
import ru.yandex.practicum.filmorate.model.Operation;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final FeedEventDbStorage feedEventDbStorage;
    private final FeedEventMapper feedEventMapper;
    private final UserDbStorage userDbStorage;

    @Override
    public List<FeedEventDto> getUserFeed(Long userId, Integer from, Integer size) {
        log.debug("Получение ленты событий для пользователя {}", userId);

        userDbStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        List<FeedEvent> events = feedEventDbStorage.findFeedEventsByUser(userId, from, size);

        log.debug("=== ДЕТАЛЬНАЯ ИНФОРМАЦИЯ О ЛЕНТЕ ПОЛЬЗОВАТЕЛЯ {} ===", userId);
        for (FeedEvent event : events) {
            log.debug("EventId: {}, Type: {}, Operation: {}, EntityId: {}, Timestamp: {}",
                    event.getEventId(), event.getEventType(), event.getOperation(),
                    event.getEntityId(), event.getTimestamp());
        }
        log.debug("=== КОНЕЦ ЛЕНТЫ ===");

        return events.stream()
                .map(feedEventMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void recordEvent(Long userId, Long actorId, EventType eventType,
                            Operation operation, Long entityId) {
        Instant now = Instant.now();

        FeedEvent event = FeedEvent.builder()
                .userId(userId)
                .actorId(actorId)
                .eventType(eventType)
                .operation(operation)
                .entityId(entityId)
                .timestamp(now)
                .build();

        feedEventDbStorage.save(event);

        log.debug("Записано событие для пользователя {}: {} {} entityId={}",
                userId, eventType, operation, entityId);
    }
}