package ru.yandex.practicum.filmorate.service.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.directors.DirectorService;
import ru.yandex.practicum.filmorate.service.film.filmValidation.FilmValidator;
import ru.yandex.practicum.filmorate.service.like.LikeService;
import ru.yandex.practicum.filmorate.service.popular_films.PopularFilmService;
import ru.yandex.practicum.filmorate.service.user.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmServiceImpl implements FilmService {

    private final FilmDbStorage filmDbStorage;
    private final FilmValidator filmValidator;
    private final UserService userService;
    private final FilmMapper filmMapper;
    private final DirectorService directorService;
    private final LikeService likeService;
    private final PopularFilmService popularFilmsService;

    @Override
    public FilmDTO createFilm(FilmDTO filmDTO) {
        log.debug("Создание нового фильма с releaseDate: {}", filmDTO.getReleaseDate());

        filmValidator.validateMpa(filmDTO.getMpa());

        Film film = filmMapper.toEntity(filmDTO);
        filmValidator.validateAndPrepareGenres(film);
        filmValidator.validateAndPrepareDirectors(filmDTO);
        Film createdFilm = filmDbStorage.createFilm(film);
        FilmDTO result = filmMapper.toDTO(createdFilm);

        log.debug("Создан фильм с ID: {}, releaseDate: {}", result.getId(), result.getReleaseDate());
        return result;
    }

    @Override
    @Transactional
    public void addLike(Long filmId, Long userId) {
        likeService.addLike(filmId, userId);
    }

    @Override
    public List<FilmDTO> getAllFilms() {
        log.debug("Получение списка всех фильмов");
        return filmDbStorage.getAllFilms().stream()
                .map(filmMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FilmDTO> getPopularFilms(Integer count, Integer genreId, Integer year) {
        log.debug("Делегирование запроса популярных фильмов");
        return popularFilmsService.getPopularFilms(count, genreId, year);
    }

    @Override
    public List<FilmDTO> getFilmsByDirector(Long directorId, String sortBy) {
        log.debug("Получение фильмов режиссера {} с сортировкой: {}", directorId, sortBy);

        directorService.getById(directorId);

        List<Film> films = filmDbStorage.getFilmsByDirector(directorId, sortBy);
        return films.stream()
                .map(filmMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public FilmDTO getFilmById(Long id) {
        log.debug("Получение фильма по ID: {}", id);
        Film film = filmDbStorage.getFilmById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + id + " не найден"));
        return filmMapper.toDTO(film);
    }

    @Override
    public FilmDTO updateFilm(FilmDTO filmDTO) {
        log.debug("Обновление фильма с ID: {}", filmDTO.getId());

        FilmDTO existingFilm = getFilmById(filmDTO.getId());
        filmValidator.validateMpa(filmDTO.getMpa());
        Film film = filmMapper.toEntity(filmDTO);
        filmValidator.validateAndPrepareGenres(film);
        filmValidator.validateAndPrepareDirectors(filmDTO);
        filmValidator.validateFilmUniquenessForUpdate(filmMapper.toEntity(existingFilm), film);

        Film updatedFilm = filmDbStorage.updateFilm(film);
        return filmMapper.toDTO(updatedFilm);
    }

    @Override
    @Transactional
    public void removeLike(Long filmId, Long userId) {
        likeService.removeLike(filmId, userId);
    }

    @Override
    public void deleteFilm(Long filmId) {
        log.debug("Начало удаления фильма с ID: {}", filmId);

        FilmDTO film = getFilmById(filmId);
        log.debug("Фильм найден: '{}' (ID: {})", film.getName(), filmId);

        try {
            filmDbStorage.deleteFilm(filmId);
            log.info("Фильм '{}' (ID: {}) успешно удален", film.getName(), filmId);
        } catch (Exception e) {
            log.error("Ошибка при удалении фильма с ID {}: {}", filmId, e.getMessage(), e);
            throw new RuntimeException("Не удалось удалить фильм", e);
        }
    }

    @Override
    public List<FilmDTO> getCommonFilms(Long userId, Long friendId) {
        log.debug("Получение общих фильмов пользователя {} и друга {}", userId, friendId);

        filmValidator.validateCommonFilmsParams(userId, friendId);
        userService.getUserById(userId);
        userService.getUserById(friendId);

        List<Film> commonFilms = filmDbStorage.getCommonFilms(userId, friendId);

        log.debug("Найдено {} общих фильмов для пользователей {} и {}",
                commonFilms.size(), userId, friendId);

        return commonFilms.stream()
                .map(filmMapper::toDTO)
                .collect(Collectors.toList());
    }
}