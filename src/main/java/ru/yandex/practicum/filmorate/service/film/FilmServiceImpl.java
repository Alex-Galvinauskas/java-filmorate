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

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmServiceImpl implements FilmService {

    private final FilmStorage filmStorage;
    private final FilmValidatorRules filmValidator;

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
}