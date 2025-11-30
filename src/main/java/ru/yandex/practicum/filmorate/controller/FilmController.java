package ru.yandex.practicum.filmorate.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.service.film.FilmService;

import java.util.List;

@RestController
@RequestMapping("/films")
public class FilmController extends AbstractController<FilmDTO, FilmService> {

    public FilmController(FilmService filmService) {
        super(filmService, "фильм");
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

    @GetMapping("/popular")
    public ResponseEntity<List<FilmDTO>> getPopularFilms(@RequestParam(defaultValue = "10") Integer count) {
        List<FilmDTO> popularFilms = service.getPopularFilms(count);
        return ResponseEntity.ok(popularFilms);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> deleteLike(@PathVariable Long id, @PathVariable Long userId) {
        service.removeLike(id, userId);
        return ResponseEntity.ok().build();
    }
}