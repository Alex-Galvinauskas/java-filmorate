package ru.yandex.practicum.filmorate.service.film;

import ru.yandex.practicum.filmorate.dto.FilmDTO;

import java.util.List;

public interface FilmService {
    FilmDTO createFilm(FilmDTO filmDTO);

    FilmDTO updateFilm(FilmDTO filmDTO);

    FilmDTO getFilmById(Long id);

    List<FilmDTO> getAllFilms();

    List<FilmDTO> getPopularFilms(Integer count);

    List<FilmDTO> getFilmsByDirector(Long directorId, String sortBy);

    void addLike(Long filmId, Long userId);

    void removeLike(Long filmId, Long userId);

    void deleteFilm(Long filmId);

    List<FilmDTO> getCommonFilms(Long userId, Long friendId);
}
