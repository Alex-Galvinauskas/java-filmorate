/**
 * Реализация сервисного слоя для работы с фильмами.
 * Содержит бизнес-логику приложения для операций с фильмами.
 * Обеспечивает проверку уникальности фильмов и обработку исключительных ситуаций.
 * Делегирует операции хранения данных объекту FilmStorage.
 *
 * @see ru.yandex.practicum.filmorate.service.film.FilmService
 * @see ru.yandex.practicum.filmorate.managment.FilmStorage
 * @see ru.yandex.practicum.filmorate.model.Film
 */
package ru.yandex.practicum.filmorate.service.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.FilmStorage;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.film.validation.FilmValidatorRules;
import ru.yandex.practicum.filmorate.service.user.UserService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmServiceImpl implements FilmService {

    private final FilmStorage filmStorage;
    private final FilmValidatorRules filmValidator;
    private final UserService userService;

    /**
     * Создает новый фильм с проверкой уникальности.
     * Проверяет, что фильм с таким же названием и годом выпуска не существует.
     * Присваивает фильму уникальный идентификатор.
     *
     * @param film фильм для создания
     *
     * @return созданный фильм с присвоенным ID
     *
     * @throws DuplicateException если фильм с таким названием и годом выпуска уже существует
     */
    @Override
    public Film createFilm(Film film) {
        log.info("Создание нового фильма: {}", film);

        filmValidator.validateFilmUniqueness(film.getName(), film.getReleaseDate().getYear());

        return filmStorage.createFilm(film);
    }

    /**
     * Добавляет лайк фильму.
     * @param filmId id фильма
     * @param userId id пользователя
     *
     * @throws NotFoundException если фильм с указанным ID не найден
     */
    @Override
    public void addLike(Long filmId, Long userId) {
        log.info("Добавление лайка фильму с ID: {} от пользователя {}", filmId, userId);

        Film film = getFilmById(filmId);
        userService.getUserById(userId);

        film.getLikes().add(userId);
        Film updatedFilm = filmStorage.updateFilm(film);

        log.debug("Лайк добавлен. Текущее количество лайков: {}", updatedFilm.getLikes().size());
    }

    /**
     * Возвращает список всех фильмов.
     * Не выполняет дополнительной бизнес-логики, просто делегирует запрос в хранилище.
     *
     * @return список всех фильмов
     */
    @Override
    public List<Film> getAllFilms() {
        log.info("Получение списка всех фильмов");
        return filmStorage.getAllFilms();
    }

    /**
     * Возвращает список популярных фильмов.
     * Популярность определяется количеством лайков.
     * @param count количество фильмов (если null или отрицательное, то по умолчанию)
     * @return список популярных фильмов, сортированных по количеству лайков по убыванию
     */
    @Override
    public List<Film> getPopularFilms(Integer count) {
        log.info("Получение списка популярных фильмов. Количество: {}", count);

        int filmsCount = (count != null) && (count >= 0) ? count : 10;

        return filmStorage.getAllFilms().stream()
                .sorted((Comparator.comparingInt((Film film) ->
                        film.getLikes().size()).reversed()))
                .limit(filmsCount)
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
    public Film getFilmById(Long id) {
        log.info("Получение фильма по ID: {}", id);
        return filmStorage.getFilmById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + id + " не найден"));
    }

    /**
     * Обновляет существующий фильм.
     * Проверяет существование фильма и уникальность новых значений названия и года выпуска.
     * Разрешает обновление если ключевые поля (название и год) не изменились.
     *
     * @param film фильм с обновленными данными
     *
     * @return обновленный фильм
     *
     * @throws NotFoundException  если фильм с указанным ID не найден
     * @throws DuplicateException если фильм с новым названием и годом выпуска уже существует
     */
    @Override
    public Film updateFilm(Film film) {
        log.info("Обновление фильма с ID: {}", film.getId());

        Film existingFilm = getFilmById(film.getId());

        filmValidator.validateFilmUniquenessForUpdate(existingFilm, film);

        return filmStorage.updateFilm(film);
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
    public void removeLike(Long filmId, Long userId) {
        log.info("Удаление лайка фильму с ID: {} от пользователя {}", filmId, userId);

        Film film = getFilmById(filmId);
        userService.getUserById(userId);

        if (!film.getLikes().remove(userId)) {
            log.warn("Попытка удалить несуществующий лайк фильма с ID: {} от пользователя {}", filmId, userId);
        } else {
            Film updatedFilm = filmStorage.updateFilm(film);
            log.debug("Лайк удален. Теперь у фильма с ID: {} {} лайков", updatedFilm.getLikes().size(), filmId);
        }
    }
}