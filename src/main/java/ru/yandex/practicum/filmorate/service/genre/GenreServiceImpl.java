package ru.yandex.practicum.filmorate.service.genre;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.GenreDTO;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.GenreDbStorage;
import ru.yandex.practicum.filmorate.mapper.GenreMapper;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GenreServiceImpl implements GenreService {

    private final GenreDbStorage genreDbStorage;
    private final GenreMapper genreMapper;

    @Override
    public List<GenreDTO> getAllGenres() {
        log.debug("Получение всех жанров");
        return genreDbStorage.getAllGenres().stream()
                .map(genreMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public GenreDTO getGenreById(Long id) {
        log.debug("Получение жанра по ID: {}", id);
        Genre genre = genreDbStorage.getGenreById(id)
                .orElseThrow(() -> new NotFoundException("Жанр с ID " + id + " не найден"));
        return genreMapper.toDTO(genre);
    }

    @Override
    public void deleteAllGenresByFilmId(Long filmId) {
        String sql = "DELETE FROM film_genres WHERE film_id = ?";
        int rowsDeleted = genreDbStorage.deleteGenresByFilmId(filmId);
        log.debug("Удалено {} связей с жанрами для фильма с ID: {}", rowsDeleted, filmId);
    }
}