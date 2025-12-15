/**
 * Реализация сервисного слоя для работы с фильмами.
 * Содержит бизнес-логику приложения для операций с фильмами.
 * Обеспечивает проверку уникальности фильмов и обработку исключительных ситуаций.
 * Делегирует операции хранения данных объекту FilmStorage.
 *
 * @see ru.yandex.practicum.filmorate.service.film.FilmService
 * @see ru.yandex.practicum.filmorate.managment.inMemory.FilmStorage
 * @see ru.yandex.practicum.filmorate.model.Film
 */
package ru.yandex.practicum.filmorate.service.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dto.DirectorDTO;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.service.directors.DirectorService;
import ru.yandex.practicum.filmorate.service.feed.FeedService;
import ru.yandex.practicum.filmorate.service.film.validation.FilmValidatorRules;
import ru.yandex.practicum.filmorate.service.user.UserService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmServiceImpl implements FilmService {

    private final FilmDbStorage filmDbStorage;
    private final FilmValidatorRules filmValidator;
    private final UserService userService;
    private final MpaService mpaService;
    private final GenreService genreService;
    private final FilmMapper filmMapper;
    private final DirectorService directorService;
    private final FeedService feedService;

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

        validateMpa(filmDTO.getMpa());

        Film film = filmMapper.toEntity(filmDTO);
        validateAndPrepareGenres(film);
        validateAndPrepareDirectors(filmDTO);

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
        recordLikeEvent(userId, filmId, Operation.ADD);
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
    public List<FilmDTO> getPopularFilms(Integer count) {
        log.debug("Получение списка популярных фильмов. Количество: {}", count);

        int filmsCount = (count != null) && (count > 0) ? count : 10;

        return filmDbStorage.getPopularFilms(filmsCount).stream()
                .map(filmMapper::toDTO)
                .collect(Collectors.toList());
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

        validateMpa(filmDTO.getMpa());

        Film film = filmMapper.toEntity(filmDTO);
        validateAndPrepareGenres(film);
        validateAndPrepareDirectors(filmDTO);

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
        recordLikeEvent(userId, filmId, Operation.REMOVE);
    }

    /**
     * Валидирует и подготавливает список жанров для фильма
     * Убирает дубликаты и проверяет существование жанров
     */
    private void validateAndPrepareGenres(Film film) {
        if (film == null) {
            log.debug("Film is null, skipping genre validation");
            return;
        }

        log.debug("Начальная валидация жанров для фильма: {}", film.getGenres());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Genre> distinctGenres = film.getGenres().stream()
                    .filter(genre -> genre.getId() != null)
                    .collect(Collectors.toMap(
                            Genre::getId,
                            genre -> genre,
                            (existing, replacement) -> existing
                    ))
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(Genre::getId))
                    .collect(Collectors.toList());

            log.debug("Жанры после удаления дубликатов и сортировки: {}", distinctGenres);

            for (Genre genre : distinctGenres) {
                genreService.getGenreById(genre.getId());
            }

            film.setGenres(distinctGenres);
            log.debug("Финальный список жанров для фильма: {}", film.getGenres());
        } else {
            log.debug("Жанры не указаны или пусты");
        }
    }

    /**
     * Валидирует MPA рейтинг
     */
    private void validateMpa(ru.yandex.practicum.filmorate.dto.MpaDTO mpa) {
        if (mpa == null) {
            throw new IllegalArgumentException("MPA рейтинг не может быть null");
        }
        if (mpa.getId() == null) {
            throw new IllegalArgumentException("ID MPA рейтинга не может быть null");
        }

        mpaService.getMpaById(mpa.getId());
    }

    private void recordLikeEvent(Long userId, Long filmId, Operation operation) {
        try {
            feedService.recordEvent(userId, userId, EventType.LIKE, operation, filmId);
            log.debug("Событие лайка ({}) записано в ленту пользователя {}", operation, userId);
        } catch (Exception e) {
            log.error("Ошибка при записи события лайка в ленту: {}", e.getMessage());
            throw e;
        }
    }


    private void validateAndPrepareDirectors(FilmDTO filmDTO) {
        if (filmDTO.getDirectors() != null && !filmDTO.getDirectors().isEmpty()) {
            for (DirectorDTO directorDTO : filmDTO.getDirectors()) {
                if (directorDTO.getId() == null) {
                    throw new IllegalArgumentException("ID режиссера не может быть null");
                }
                directorService.getById(directorDTO.getId());
            }

            List<DirectorDTO> uniqueDirectors = filmDTO.getDirectors().stream()
                    .collect(Collectors.toMap(
                            DirectorDTO::getId,
                            d -> d,
                            (d1, d2) -> d1
                    ))
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(DirectorDTO::getId))
                    .collect(Collectors.toList());

            filmDTO.setDirectors(uniqueDirectors);
        }
    }
}