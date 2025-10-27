package ru.yandex.practicum.filmorate.service.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface FilmService {
    Film createFilm(Film film);

    void addLike(Long filmId, Long userId);

    List<Film> getAllFilms();

    List<Film> getPopularFilms(Integer count);

    Film getFilmById(Long id);

    Film updateFilm(Film film);

    void removeLike(Long filmId, Long userId);
}
