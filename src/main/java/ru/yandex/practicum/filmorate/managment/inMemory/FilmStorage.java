package ru.yandex.practicum.filmorate.managment.inMemory;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface FilmStorage {
    Film createFilm(Film film);

    List<Film> getAllFilms();

    Optional<Film> getFilmById(Long id);

    Film updateFilm(Film film);

    Map<Long, Set<Long>> getLikesByUsers();

    List<Film> getFilmsByIds(Set<Long> ids);

    boolean existsFilmById(Long id);

    boolean existsFilmByNameAndReleaseYear(String name, Integer releaseYear);

    boolean existsFilmByNameAndReleaseYearExcludingId(String name, Integer releaseYear, Long excludedId);

    void deleteFilm(Long filmId);

    List<Film> getCommonFilms(Long userId, Long friendId);
}