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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}