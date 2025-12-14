package ru.yandex.practicum.filmorate.service.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dto.FeedEventDTO;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.FeedDbStorage;
import ru.yandex.practicum.filmorate.managment.db.UserDbStorage;
import ru.yandex.practicum.filmorate.mapper.FeedEventMapper;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.FeedEvent;
import ru.yandex.practicum.filmorate.model.Operation;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final FeedDbStorage feedDbStorage;
    private final FeedEventMapper feedEventMapper;
    private final UserDbStorage userDbStorage;

    @Override
    @Transactional
    public void recordEvent(Long userId, Long actorId, EventType eventType,
                            Operation operation, Long entityId) {
        FeedEvent event = FeedEvent.builder()
                .userId(userId)
                .actorId(actorId)
                .eventType(eventType)
                .operation(operation)
                .entityId(entityId)
                .timestamp(Instant.now())
                .build();

        feedDbStorage.create(event);

        log.debug("Записано событие: userId={}, actorId={}, eventType={}, operation={}, entityId={}",
                userId, actorId, eventType, operation, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedEventDTO> getUserFeed(Long userId) {
        log.debug("Получение ленты событий для пользователя {}", userId);

        if (!userDbStorage.userExists(userId)) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        log.debug("Получение событий для пользователя: {}", userId);
        List<FeedEvent> events = feedDbStorage.findByUserId(userId, 1000);

        List<FeedEventDTO> result = events.stream()
                .map(feedEventMapper::toDTO)
                .collect(Collectors.toList());

        log.debug("Найдено {} событий в ленте пользователя {}", result.size(), userId);
        return result;
    }
}