/**
 * Контроллер для управления операциями с фильмами.
 * Обрабатывает HTTP-запросы для создания, получения, обновления и управления фильмами.
 * Предоставляет REST API для работы с сущностью Film.
 *
 * @see ru.yandex.practicum.filmorate.model.Film
 * @see ru.yandex.practicum.filmorate.service.FilmService
 */
package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;

@RestController
@RequestMapping("/films")
@RequiredArgsConstructor
@Slf4j
public class FilmController {

    private final FilmService filmService;

    /**
     * Создает новый фильм.
     *
     * @param film объект фильма для создания
     * @return созданный фильм с присвоенным идентификатором
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Film createFilm(@Valid @RequestBody Film film) {
        return filmService.createFilm(film);
    }

    /**
     * Возвращает список всех фильмов.
     *
     * @return список всех фильмов в системе
     */
    @GetMapping
    public List<Film> getAllFilms() {
        return filmService.getAllFilms();
    }

    /**
     * Возвращает фильм по его идентификатору.
     *
     * @param id идентификатор фильма
     * @return найденный фильм
     * @throws ru.yandex.practicum.filmorate.exception.NotFoundException если фильм с указанным ID не найден
     */
    @GetMapping("/{id}")
    public Film getFilmById(@PathVariable Long id) {
        return filmService.getFilmById(id);
    }

    /**
     * Обновляет существующий фильм.
     *
     * @param film объект фильма с обновленными данными
     * @return обновленный фильм
     * @throws ru.yandex.practicum.filmorate.exception.NotFoundException если фильм с указанным ID не найден
     */
    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film film) {
        return filmService.updateFilm(film);
    }
}