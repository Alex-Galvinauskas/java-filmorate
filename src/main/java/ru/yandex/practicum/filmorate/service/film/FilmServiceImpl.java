/**
 * Реализация сервисного слоя для работы с фильмами.
 * Содержит бизнес-логику приложения для операций с фильмами.
 * Обеспечивает проверку уникальности фильмов и обработку исключительных ситуаций.
 * Делегирует операции хранения данных объекту FilmStorage.
 */
package ru.yandex.practicum.filmorate.service.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.service.directors.DirectorService;
import ru.yandex.practicum.filmorate.service.film.filmValidation.FilmValidator;
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
    private final LikeEventService likeEventService;
    private final PopularFilmService popularFilmsService;

    /**
     * Создает новый фильм с проверкой уникальности.
     * Проверяет, что фильм с таким же названием и годом выпуска не существует.
     * Присваивает фильму уникальный идентификатор.
     *
     * @param filmDTO фильм для создания
     *
     * @return созданный фильм с присвоенным ID
     *
     * @throws DuplicateException если фильм с таким названием и годом выпуска уже существует
     */
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

    /**
     * Добавляет лайк фильму.
     *
     * @param filmId id фильма
     * @param userId id пользователя
     *
     * @throws NotFoundException если фильм с указанным ID не найден
     */
    @Override
    @Transactional
    public void addLike(Long filmId, Long userId) {
        log.debug("Добавление лайка фильму с ID: {} от пользователя {}", filmId, userId);

        getFilmById(filmId);
        userService.getUserById(userId);

        filmDbStorage.addLike(filmId, userId);
        likeEventService.recordLikeEvent(userId, filmId, Operation.ADD);
    }

    /**
     * Возвращает список всех фильмов.
     * Не выполняет дополнительной бизнес-логики, просто делегирует запрос в хранилище.
     *
     * @return список всех фильмов
     */
    @Override
    public List<FilmDTO> getAllFilms() {
        log.debug("Получение списка всех фильмов");
        return filmDbStorage.getAllFilms().stream()
                .map(filmMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает список популярных фильмов.
     * Популярность определяется количеством лайков.
     *
     * @param count количество фильмов (если null или отрицательное, то по умолчанию)
     *
     * @return список популярных фильмов, сортированных по количеству лайков по убыванию
     */
    @Override
    public List<FilmDTO> getPopularFilms(Integer count, Integer genreId, Integer year) {
        log.debug("Делегирование запроса популярных фильмов в специализированный сервис");
        return popularFilmsService.getPopularFilms(count, genreId, year);
    }

    public List<FilmDTO> getFilmsByDirector(Long directorId, String sortBy) {
        log.debug("Получение фильмов режиссера {} с сортировкой: {}", directorId, sortBy);

        directorService.getById(directorId);

        List<Film> films = filmDbStorage.getFilmsByDirector(directorId, sortBy);
        return films.stream()
                .map(filmMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Находит фильм по идентификатору.
     * Выполняет проверку существования фильма и генерирует исключение если фильм не найден.
     *
     * @param id идентификатор фильма
     *
     * @return найденный фильм
     *
     * @throws NotFoundException если фильм с указанным ID не найден
     */
    @Override
    public FilmDTO getFilmById(Long id) {
        log.debug("Получение фильма по ID: {}", id);
        Film film = filmDbStorage.getFilmById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + id + " не найден"));
        return filmMapper.toDTO(film);
    }

    /**
     * Обновляет существующий фильм.
     * Проверяет существование фильма и уникальность новых значений названия и года выпуска.
     * Разрешает обновление если ключевые поля (название и год) не изменились.
     *
     * @param filmDTO фильм с обновленными данными
     *
     * @return обновленный фильм
     *
     * @throws NotFoundException  если фильм с указанным ID не найден
     * @throws DuplicateException если фильм с новым названием и годом выпуска уже существует
     */
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

    /**
     * Удаляет лайк у фильма.
     * Проверяет существование фильма и пользователя.
     *
     * @param filmId id фильма
     * @param userId id пользователя
     *
     * @throws NotFoundException если фильм с указанными ID не найдены
     */
    @Override
    @Transactional
    public void removeLike(Long filmId, Long userId) {
        log.debug("Удаление лайка фильму с ID: {} от пользователя {}", filmId, userId);

        getFilmById(filmId);
        userService.getUserById(userId);

        filmDbStorage.removeLike(filmId, userId);
        likeEventService.recordLikeEvent(userId, filmId, Operation.REMOVE);
    }

    /**
     * Удаляет фильм по идентификатору.
     *
     * @param filmId идентификатор фильма для удаления
     *
     * @throws NotFoundException если фильм с указанным ID не найден
     */
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

        if (userId == null) {
            throw new IllegalArgumentException("Идентификатор пользователя не может быть null");
        }
        if (friendId == null) {
            throw new IllegalArgumentException("Идентификатор друга не может быть null");
        }

        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Нельзя искать общие фильмы с самим собой");
        }

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