package ru.yandex.practicum.filmorate.service.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.service.feed.FeedService;

@Service
@Slf4j
@RequiredArgsConstructor
public class LikeEventService {

    private final FeedService feedService;

    /**
     * Записывает событие лайка в ленту пользователя
     *
     * @param userId ID пользователя, который поставил/убрал лайк
     * @param filmId ID фильма, которому поставлен/удален лайк
     * @param operation операция (ADD или REMOVE)
     */
    public void recordLikeEvent(Long userId, Long filmId, Operation operation) {
        try {
            feedService.recordEvent(userId, userId, EventType.LIKE, operation, filmId);
            log.debug("Событие лайка ({}) записано в ленту пользователя {} для фильма {}",
                    operation, userId, filmId);
        } catch (Exception e) {
            log.error("Ошибка при записи события лайка в ленту для пользователя {} и фильма {}: {}",
                    userId, filmId, e.getMessage(), e);
            throw e;
        }
    }
}