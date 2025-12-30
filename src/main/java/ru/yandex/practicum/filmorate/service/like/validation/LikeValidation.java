package ru.yandex.practicum.filmorate.service.like.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.service.user.UserService;

@Service
@Slf4j
@RequiredArgsConstructor
public class LikeValidation {

    private final FilmDbStorage filmDbStorage;
    private final UserService userService;

    public void validateFilmAndUserExist(Long filmId, Long userId) {
        filmDbStorage.getFilmById(filmId);
        userService.getUserById(userId);
        log.debug("Фильм {} и пользователь {} существуют", filmId, userId);
    }
}