package ru.yandex.practicum.filmorate.service;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface FilmService {
    Film createFilm(Film film);
    List<Film> getAllFilms();
    Film getFilmById(Long id);
    Film updateFilm(Film film);
}
