package ru.yandex.practicum.filmorate.service.film;

import ru.yandex.practicum.filmorate.dto.FilmDTO;

import java.util.List;

public interface FilmService {
    FilmDTO createFilm(FilmDTO filmDTO);

    FilmDTO updateFilm(FilmDTO filmDTO);

    FilmDTO getFilmById(Long id);

    List<FilmDTO> getAllFilms();

    /**
     * добавляем параметры genreId и year
      */
    List<FilmDTO> getPopularFilms(Integer count, Integer genreId, Integer year);

    void addLike(Long filmId, Long userId);

    void removeLike(Long filmId, Long userId);
}
