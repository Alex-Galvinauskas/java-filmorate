package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.service.film.GenreService;

import java.util.List;

@RestController
@RequestMapping("/genres")
@Slf4j
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    /**
     * Получает список всех жанров
     * @return список всех жанров
     */
    @GetMapping
    public List<Genre> getAllGenres() {
        log.info("Получен запрос на получение всех жанров");

        List<Genre> genres = genreService.getAllGenres();

        log.info("Возвращено {} жанров", genres.size());
        return genres;
    }

    /**
     * Получает жанр по идентификатору
     * @param id идентификатор жанра
     * @return найденный жанр
     */
    @GetMapping("/{id}")
    public Genre getGenreById(@PathVariable Integer id) {
        log.info("Получен запрос на получение жанра с id={}", id);

        Genre genre = genreService.getGenreById(id);

        log.info("Жанр с id {} найден: {}", id, genre.getName());
        return genre;
    }
}