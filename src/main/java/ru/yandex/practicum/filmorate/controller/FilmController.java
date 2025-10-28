/**
 * Контроллер для управления операциями с фильмами.
 * Обрабатывает HTTP-запросы для создания, получения, обновления и управления фильмами.
 * Предоставляет REST API для работы с сущностью Film.
 *
 * @see ru.yandex.practicum.filmorate.model.Film
 * @see ru.yandex.practicum.filmorate.service.film.FilmService
 */
package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.film.FilmService;

import java.util.List;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController extends AbstractController<Film, FilmService> {


    public FilmController(FilmService filmService) {
        super(filmService, "фильма");
    }

    @Override
    protected Film createEntity(Film film) {
        return service.createFilm(film);
    }

    @Override
    protected List<Film> getAllEntities() {
        return service.getAllFilms();
    }

    @Override
    protected Film getEntityById(Long id) {
        return service.getFilmById(id);
    }

    @Override
    protected Film updateEntity(Film film) {
        return service.updateFilm(film);
    }

    /**
     * Добавляет лайк фильму от пользователя.
     * @param id - id фильма
     * @param userId - id пользователя
     */
    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Получен запрос на добавление лайка фильму с id {} от пользователя с id {}", id, userId);

        service.addLike(id, userId);

        log.info("Лайк фильму с id {} успешно добавлен", id);
    }

    /**
     * Получает список популярных фильмов.
     * @param count - количество фильмов (опционально)
     * @return - список популярных фильмов
     */
    @GetMapping("/popular")
    public List<Film> getPopularFilms(@RequestParam(defaultValue = "10") Integer count) {
        log.info("Получен запрос на получение популярных фильмов {}", count);

        List<Film> popularFilms = service.getPopularFilms(count);

        log.info("Получен список популярных фильмов {}", popularFilms.size());
        return popularFilms;
    }

    /**
     * Удаляет лайк фильму от пользователя.
     * @param id - id фильма
     * @param userId - id пользователя
     */
    @DeleteMapping("/{id}/like/{userId}")
    public void deleteLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Получен запрос на удаление лайка фильму с id {} от пользователя с id {}", id, userId);

        service.removeLike(id, userId);

        log.info("Лайк фильму с id {} от пользователя с id {} успешно удален", id, userId);
    }
}