package ru.yandex.practicum.filmorate.service.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.GenreDbStorage;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GenreServiceImpl implements GenreService {

    private final GenreDbStorage genreDbStorage;

    @Override
    public List<Genre> getAllGenres() {
        log.info("Получение всех жанров");
        return genreDbStorage.getAllGenres();
    }

    @Override
    public Genre getGenreById(Integer id) {
        log.info("Получение жанра по ID: {}", id);
        return genreDbStorage.getGenreById(id)
                .orElseThrow(() -> new NotFoundException("Жанр с ID " + id + " не найден"));
    }
}