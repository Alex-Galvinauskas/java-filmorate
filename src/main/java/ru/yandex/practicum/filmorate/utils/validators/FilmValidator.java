/**
 * Компонент для валидации фильмов.
 * Может быть внедрен через dependency injection.
 */
package ru.yandex.practicum.filmorate.utils.validators;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.managment.FilmStorage;
import ru.yandex.practicum.filmorate.model.Film;

@Component
@Slf4j
public class FilmValidator {

    /**
     * Проверяет уникальность фильма по названию и году выпуска.
     */
    public void validateFilmUniqueness(FilmStorage filmStorage, String name, int releaseYear) {
        log.debug("Проверка уникальности фильма: {} ({})", name, releaseYear);

        if (filmStorage.existsFilmByNameAndReleaseYear(name, releaseYear)) {
            throw new DuplicateException(buildDuplicateErrorMessage(name, releaseYear));
        }
    }

    /**
     * Проверяет уникальность фильма при обновлении.
     */
    public void validateFilmUniquenessForUpdate(FilmStorage filmStorage,
                                                Film existingFilm,
                                                Film updatedFilm) {

        boolean nameChanged = !existingFilm.getName().equals(updatedFilm.getName());
        boolean yearChanged = existingFilm.getReleaseDate().getYear() != updatedFilm.getReleaseDate().getYear();

        if (nameChanged || yearChanged) {
            validateFilmUniqueness(filmStorage, updatedFilm.getName(),
                    updatedFilm.getReleaseDate().getYear());
        }
    }

    private String buildDuplicateErrorMessage(String name, int releaseYear) {
        return String.format("Фильм с названием '%s' и годом выхода '%s' уже существует", name, releaseYear);
    }

}
