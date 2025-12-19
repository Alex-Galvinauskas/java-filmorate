package ru.yandex.practicum.filmorate.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.service.film.FilmService;
import ru.yandex.practicum.filmorate.service.film.FilmSearchService;

import java.util.List;

@RestController
@RequestMapping("/films")
public class FilmController extends AbstractController<FilmDTO, FilmService> {

    private final FilmSearchService filmSearchService;

    public FilmController(FilmService filmService, FilmSearchService filmSearchService) {
        super(filmService, "фильм");
        this.filmSearchService = filmSearchService;
    }

    @Override
    protected FilmDTO createEntity(FilmDTO filmDTO) {
        return service.createFilm(filmDTO);
    }

    @Override
    protected List<FilmDTO> getAllEntities() {
        return service.getAllFilms();
    }

    @Override
    protected FilmDTO getEntityById(Long id) {
        return service.getFilmById(id);
    }

    @Override
    protected FilmDTO updateEntity(FilmDTO filmDTO) {
        return service.updateFilm(filmDTO);
    }

    @Override
    protected Long getEntityId(FilmDTO filmDTO) {
        return filmDTO.getId();
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable Long id, @PathVariable Long userId) {
        service.addLike(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/common")
    public ResponseEntity<List<FilmDTO>> getCommonFilms(
            @RequestParam Long userId,
            @RequestParam Long friendId) {

        List<FilmDTO> commonFilms = service.getCommonFilms(userId, friendId);

        return ResponseEntity.ok(commonFilms);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<FilmDTO>> getPopularFilms(
            @RequestParam(required = false, defaultValue = "10") Integer count,
            @RequestParam(required = false) Integer genreId,
            @RequestParam(required = false) Integer year) {
        List<FilmDTO> popularFilms = service.getPopularFilms(count, genreId, year);
        return ResponseEntity.ok(popularFilms);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> deleteLike(@PathVariable Long id, @PathVariable Long userId) {
        service.removeLike(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{filmId}")
    public ResponseEntity<Void> deleteFilm(@PathVariable Long filmId) {
        service.deleteFilm(filmId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/director/{directorId}")
    public ResponseEntity<List<FilmDTO>> getFilmsByDirector(
            @PathVariable Long directorId,
            @RequestParam(defaultValue = "likes") String sortBy) {

        if (!sortBy.equals("year") && !sortBy.equals("likes")) {
            throw new IllegalArgumentException("Параметр sortBy может быть только 'year' или 'likes'");
        }

        List<FilmDTO> films = service.getFilmsByDirector(directorId, sortBy);
        return ResponseEntity.ok(films);
    }

    @GetMapping("/search")
    public ResponseEntity<List<FilmDTO>> getFilmsViaSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String by
    ) {
        List<FilmDTO> films = filmSearchService.searchFilms(query, by);
        return ResponseEntity.ok(films);
    }
}