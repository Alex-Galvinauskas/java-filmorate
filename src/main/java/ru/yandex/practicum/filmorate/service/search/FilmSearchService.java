package ru.yandex.practicum.filmorate.service.search;

import ru.yandex.practicum.filmorate.dto.FilmDTO;

import java.util.List;

public interface FilmSearchService {

    List<FilmDTO> searchFilms(String query, String searchBy);
}
