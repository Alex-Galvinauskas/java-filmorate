package ru.yandex.practicum.filmorate.managment;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

public interface FilmStorage {
    Film createFilm(Film film);

    List<Film> getAllFilms();

    Optional<Film> getFilmById(Long id);

    Film updateFilm(Film film);

    boolean existsFilmById(Long id);

    boolean existsFilmByNameAndReleaseYear(String name, Integer releaseYear);

    boolean existsFilmByNameAndReleaseYearAndIdNot(String name, Integer releaseYear, Long excludeId);
}