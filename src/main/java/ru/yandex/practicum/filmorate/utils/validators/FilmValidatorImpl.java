/**
 * Компонент для валидации фильмов.
 * Может быть внедрен через dependency injection.
 */
package ru.yandex.practicum.filmorate.utils.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.managment.FilmStorage;
import ru.yandex.practicum.filmorate.model.Film;

@Component
@Slf4j
@RequiredArgsConstructor
public class FilmValidatorImpl implements FilmValidator {

    private final FilmStorage filmStorage;

    /**
     * Проверяет уникальность фильма при обновлении.
     */
    public void validateFilmUniquenessForUpdate(Film existingFilm,
                                                Film updatedFilm) {

        boolean nameChanged = !existingFilm.getName().equals(updatedFilm.getName());
        boolean yearChanged = existingFilm.getReleaseDate().getYear() != updatedFilm.getReleaseDate().getYear();

        if (nameChanged || yearChanged) {
            validateFilmUniqueness(updatedFilm.getName(),
                    updatedFilm.getReleaseDate().getYear());
        }
    }

    /**
     * Проверяет уникальность фильма по названию и году выпуска.
     */
    public void validateFilmUniqueness(String name, int releaseYear) {
        log.debug("Проверка уникальности фильма: {} ({})", name, releaseYear);

        if (filmStorage.existsFilmByNameAndReleaseYear(name, releaseYear)) {
            throw new DuplicateException(buildDuplicateErrorMessage(name, releaseYear));
        }
    }

    public String buildDuplicateErrorMessage(String name, int releaseYear) {
        return String.format("Фильм с названием '%s' и годом выхода '%s' уже существует", name, releaseYear);
    }

}
