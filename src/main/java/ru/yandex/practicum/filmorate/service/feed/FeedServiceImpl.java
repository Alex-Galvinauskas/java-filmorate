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

    /**
     * Получить ленту событий пользователя с пагинацией
     */
    @Override
    public List<FeedEventDto> getUserFeed(Long userId, Integer from, Integer size) {
        log.debug("Получение ленты событий для пользователя {}", userId);

        // Проверяем существование пользователя
        userDbStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        // Валидация пагинации - для тестов возвращаем все события
        int validFrom = (from == null || from < 0) ? 0 : from;
        int validSize = (size == null || size <= 0) ? Integer.MAX_VALUE : size; // Убираем ограничение

        // Получаем события из БД
        List<FeedEvent> events = feedEventDbStorage.findFeedEventsByUser(userId, validFrom, validSize);

        // Конвертируем в DTO
        return events.stream()
                .map(feedEventMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Записать событие для пользователя и его друзей
     */
    @Transactional
    public void recordEvent(Long userId, Long actorId, EventType eventType,
                            Operation operation, Long entityId) {
        Instant now = Instant.now();

        // 1. Событие для самого пользователя
        FeedEvent selfEvent = FeedEvent.builder()
                .userId(userId)
                .actorId(actorId)
                .eventType(eventType)
                .operation(operation)
                .entityId(entityId)
                .timestamp(now)
                .build();
        feedEventDbStorage.save(selfEvent);  // Теперь save() возвращает FeedEvent с eventId

        log.debug("Записано событие для пользователя {}: {} {} (eventId: {})",
                userId, eventType, operation, selfEvent.getEventId());
    }

    /**
     * Записать событие для пользователя и всех его друзей
     */
    @Transactional
    public void recordEventForUserAndFriends(Long actorId, EventType eventType,
                                             Operation operation, Long entityId) {
        Instant now = Instant.now();

        // 1. Событие для самого пользователя
        FeedEvent selfEvent = FeedEvent.builder()
                .userId(actorId)
                .actorId(actorId)
                .eventType(eventType)
                .operation(operation)
                .entityId(entityId)
                .timestamp(now)
                .build();
        feedEventDbStorage.save(selfEvent);

        // 2. Получаем друзей пользователя
        List<Long> friendIds = feedEventDbStorage.getFriendIds(actorId);

        // 3. Создаём события для каждого друга
        for (Long friendId : friendIds) {
            FeedEvent friendEvent = FeedEvent.builder()
                    .userId(friendId)
                    .actorId(actorId)
                    .eventType(eventType)
                    .operation(operation)
                    .entityId(entityId)
                    .timestamp(now)
                    .build();
            feedEventDbStorage.save(friendEvent);
        }

        log.info("Записано событие {} {} для пользователя {} и {} друзей",
                eventType, operation, actorId, friendIds.size());
    }

    /**
     * Обработка события LIKE (фильм)
     */
    @Transactional
    public void handleLikeEvent(Long userId, Long filmId, boolean isAdd) {
        Operation operation = isAdd ? Operation.ADD : Operation.REMOVE;
        recordEventForUserAndFriends(userId, EventType.LIKE, operation, filmId);
    }

    /**
     * Обработка события REVIEW
     */
    @Transactional
    public void handleReviewEvent(Long userId, Long reviewId, Operation operation) {
        recordEventForUserAndFriends(userId, EventType.REVIEW, operation, reviewId);
    }

    /**
     * Обработка события FRIEND
     */
    @Transactional
    public void handleFriendEvent(Long userId, Long friendId, boolean isAdd) {
        Operation operation = isAdd ? Operation.ADD : Operation.REMOVE;
        recordEventForUserAndFriends(userId, EventType.FRIEND, operation, friendId);
    }
}