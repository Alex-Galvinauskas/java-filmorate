package ru.yandex.practicum.filmorate.service.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.service.user.UserService;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class LikeService {

    private final FilmDbStorage filmDbStorage;
    private final UserService userService;
    private final LikeEventService likeEventService;
    private final FilmService filmService;

    /**
     * Добавляет лайк фильму от пользователя.
     * Выполняет проверку существования фильма и пользователя.
     * Записывает событие лайка в ленту пользователя.
     *
     * @param filmId ID фильма
     * @param userId ID пользователя
     *
     * @throws NotFoundException если фильм или пользователь не найдены
     */
    @Transactional
    public void addLike(Long filmId, Long userId) {
        log.debug("Добавление лайка фильму с ID: {} от пользователя {}", filmId, userId);

        validateFilmAndUserExist(filmId, userId);

        filmDbStorage.addLike(filmId, userId);

        likeEventService.recordLikeEvent(userId, filmId, Operation.ADD);

        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    /**
     * Удаляет лайк у фильма от пользователя.
     * Выполняет проверку существования фильма и пользователя.
     * Записывает событие удаления лайка в ленту пользователя.
     *
     * @param filmId ID фильма
     * @param userId ID пользователя
     *
     * @throws NotFoundException если фильм или пользователь не найдены
     */
    @Transactional
    public void removeLike(Long filmId, Long userId) {
        log.debug("Удаление лайка фильму с ID: {} от пользователя {}", filmId, userId);

        validateFilmAndUserExist(filmId, userId);

        filmDbStorage.removeLike(filmId, userId);

        likeEventService.recordLikeEvent(userId, filmId, Operation.REMOVE);

        log.info("Пользователь {} удалил лайк у фильма {}", userId, filmId);
    }



    /**
     * Получает список пользователей, которые лайкнули фильм.
     *
     * @param filmId ID фильма
     * @return множество ID пользователей
     */
    public Set<Long> getUsersWhoLikedFilm(Long filmId) {
        log.debug("Получение пользователей, лайкнувших фильм {}", filmId);

        try {
            filmService.getFilmById(filmId);
            Set<Long> likes = filmDbStorage.getLikesByFilmId(filmId);
            log.debug("Фильм {} лайкнули {} пользователей", filmId, likes.size());
            return likes;
        } catch (NotFoundException e) {
            log.debug("Фильм с ID {} не найден", filmId);
            return Set.of();
        }
    }

    /**
     * Проверяет существование фильма и пользователя.
     * Выбрасывает исключения если что-то не найдено.
     *
     * @param filmId ID фильма
     * @param userId ID пользователя
     */
    private void validateFilmAndUserExist(Long filmId, Long userId) {

        filmService.getFilmById(filmId);
        userService.getUserById(userId);

        log.debug("Фильм {} и пользователь {} существуют", filmId, userId);
    }
}