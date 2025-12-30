package ru.yandex.practicum.filmorate.service.like;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.service.like.validation.LikeValidation;

@Service
@Slf4j
@RequiredArgsConstructor
public class LikeService {

    private final FilmDbStorage filmDbStorage;
    private final LikeEventService likeEventService;
    private final LikeValidation likeValidation;

    @Transactional
    public void addLike(Long filmId, Long userId) {
        log.debug("Добавление лайка фильму с ID: {} от пользователя {}", filmId, userId);

        likeValidation.validateFilmAndUserExist(filmId, userId);
        filmDbStorage.addLike(filmId, userId);
        likeEventService.recordLikeEvent(userId, filmId, Operation.ADD);

        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    @Transactional
    public void removeLike(Long filmId, Long userId) {
        log.debug("Удаление лайка фильму с ID: {} от пользователя {}", filmId, userId);

        likeValidation.validateFilmAndUserExist(filmId, userId);
        filmDbStorage.removeLike(filmId, userId);
        likeEventService.recordLikeEvent(userId, filmId, Operation.REMOVE);

        log.info("Пользователь {} удалил лайк у фильма {}", userId, filmId);
    }
}