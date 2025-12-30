package ru.yandex.practicum.filmorate.service.film;

import ru.yandex.practicum.filmorate.dto.GenreDTO;

import java.util.List;

public interface GenreService {
    List<GenreDTO> getAllGenres();

    GenreDTO getGenreById(Long id);

    void deleteAllGenresByFilmId(Long filmId);
}