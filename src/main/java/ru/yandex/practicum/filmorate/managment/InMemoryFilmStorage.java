/**
 * Реализация хранилища фильмов в памяти.
 * Хранит данные о фильмах в ConcurrentHashMap и обеспечивает потокобезопасные операции.
 * Генерирует уникальные идентификаторы для новых фильмов с помощью AtomicLong.
 * Использует дополнительный индекс для быстрого поиска фильмов по названию и году выпуска.
 *
 * @see ru.yandex.practicum.filmorate.managment.FilmStorage
 * @see ru.yandex.practicum.filmorate.model.Film
 */
package ru.yandex.practicum.filmorate.managment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new ConcurrentHashMap<>();
    private final Map<FilmKey, Long> filmIndex = new ConcurrentHashMap<>();
    private final AtomicLong nextFilmId = new AtomicLong(1);

    /**
     * Создает новый фильм в хранилище.
     * Присваивает фильму уникальный идентификатор и сохраняет его.
     *
     * @param film фильм для создания (без ID)
     *
     * @return созданный фильм с присвоенным идентификатором
     */
    @Override
    public Film createFilm(Film film) {
        Long filmId = nextFilmId.getAndIncrement();
        Film filmToSave = Film.copyWithId(film, filmId);
        films.put(filmId, filmToSave);
        updateIndex(filmToSave, null);

        log.info("Добавлен новый фильм: '{}' (ID: {})", filmToSave.getName(), filmToSave.getId());
        return filmToSave;
    }

    /**
     * Возвращает список всех фильмов в хранилище.
     *
     * @return неизменяемый список всех фильмов
     */
    @Override
    public List<Film> getAllFilms() {
        log.debug("Получение всех фильмов. Текущее количество фильмов: {}", films.size());
        return List.copyOf(films.values());
    }

    /**
     * Находит фильм по его идентификатору.
     *
     * @param id идентификатор фильма
     *
     * @return Optional с найденным фильмом или пустой Optional если фильм не найден
     */
    @Override
    public Optional<Film> getFilmById(Long id) {
        if (id == null) {
            log.debug("Попытка поиска фильма с null ID");
            return Optional.empty();
        }

        Film film = films.get(id);
        log.debug("Поиск фильма по ID: {}. Найден: {}", id, film != null);
        return Optional.ofNullable(film);
    }

    /**
     * Обновляет существующий фильм в хранилище.
     * Заменяет фильм с указанным ID на новый объект фильма.
     *
     * @param film фильм с обновленными данными
     *
     * @return обновленный фильм
     *
     * @throws NotFoundException если фильм с указанным ID не найден
     */
    @Override
    public Film updateFilm(Film film) {
        Long filmId = film.getId();
        Film existingFilm = films.get(filmId);
        if (!films.containsKey(filmId)) {
            log.warn("Попытка обновления несуществующего фильма с ID {}", filmId);
            throw new NotFoundException("Фильм с указанным ID не найден");
        }

        Film filmToUpdate = Film.builder()
                .id(filmId)
                .name(film.getName())
                .description(film.getDescription())
                .releaseDate(film.getReleaseDate())
                .duration(film.getDuration())
                .likes(existingFilm.getLikes())
                .build();

        films.put(filmId, filmToUpdate);
        updateIndex(filmToUpdate, existingFilm);

        log.info("Обновлен фильм: '{}' (ID: {})", filmToUpdate.getName(), filmToUpdate.getId());
        return filmToUpdate;
    }

    /**
     * Проверяет существование фильма по идентификатору.
     *
     * @param id идентификатор фильма
     *
     * @return true если фильм существует, false в противном случае
     */
    @Override
    public boolean existsFilmById(Long id) {
        return films.containsKey(id);
    }

    /**
     * Проверяет существование фильма по названию и году выпуска.
     * Использует индекс для быстрого поиска.
     *
     * @param name        название фильма
     * @param releaseYear год выпуска
     *
     * @return true если фильм с такими названием и годом существует, false в противном случае
     */
    @Override
    public boolean existsFilmByNameAndReleaseYear(String name, Integer releaseYear) {
        if (name == null || releaseYear == null) {
            log.debug("Попытка поиска фильма с null названием или годом выпуска");
            return false;
        }

        FilmKey key = new FilmKey(name.toLowerCase(), releaseYear);
        boolean exists = filmIndex.containsKey(key);
        log.debug("Поиск фильма по названию '{}' и году {}. Найден: {}", name, releaseYear, exists);
        return exists;
    }

    /**
     * Обновляет индекс фильмов по имени и году выпуска.
     * Удаляет старую запись (если есть) и добавляет новую.
     *
     * @param newFilm новый фильм
     * @param oldFilm старый фильм (может быть null при создании)
     */
    private void updateIndex(Film newFilm, Film oldFilm) {
        if (oldFilm != null && oldFilm.getName() != null && oldFilm.getReleaseDate() != null) {
            FilmKey oldKey = new FilmKey(
                    oldFilm.getName().toLowerCase(),
                    oldFilm.getReleaseDate().getYear()
            );
            filmIndex.remove(oldKey);
            log.debug("Удалена старая запись из индекса: {}", oldKey);
        }

        if (newFilm.getName() != null && newFilm.getReleaseDate() != null) {
            FilmKey newKey = new FilmKey(
                    newFilm.getName().toLowerCase(),
                    newFilm.getReleaseDate().getYear()
            );

            filmIndex.put(newKey, newFilm.getId());
            log.debug("Добавлена новая запись в индекс: {} -> ID {}", newKey, newFilm.getId());
        }
    }

    /**
     * Ключ для индексации фильмов по названию и году выпуска.
     * Используется для быстрого поиска дубликатов фильмов.
     *
     * @param nameLowercase название в нижнем регистре
     * @param releaseYear   год выпуска
     */
    private record FilmKey(String nameLowercase, int releaseYear) {
    }
}