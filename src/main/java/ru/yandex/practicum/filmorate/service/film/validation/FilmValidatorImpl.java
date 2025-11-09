package ru.yandex.practicum.filmorate.service.film.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.managment.FilmStorage;
import ru.yandex.practicum.filmorate.model.Film;

@Component
@Slf4j
@RequiredArgsConstructor
public class FilmValidatorImpl implements FilmValidatorRules {

    private final FilmStorage filmStorage;

    /**
     * Проверяет уникальность фильма при обновлении.
     */
    @Override
    public void validateFilmUniquenessForUpdate(Film existingFilm, Film updatedFilm) {
        if (existingFilm.getName().equals(updatedFilm.getName()) &&
                existingFilm.getReleaseDate().getYear() == updatedFilm.getReleaseDate().getYear()) {
            return;
        }

        validateFilmUniqueness(updatedFilm.getName(), updatedFilm.getReleaseDate().getYear());
    }

    /**
     * Проверяет уникальность фильма по названию и году выпуска.
     * Игнорирует фильм с тем же ID при обновлении.
     */
    @Override
    public void validateFilmUniqueness(String name, int releaseYear) {
        log.debug("Проверка уникальности фильма: {} ({})", name, releaseYear);

        if (filmStorage.existsFilmByNameAndReleaseYear(name, releaseYear)) {
            throw new DuplicateException(buildDuplicateErrorMessage(name, releaseYear));
        }
    }

    /**
     * Проверяет уникальность фильма при обновлении с учетом ID
     */
    public void validateFilmUniquenessForUpdateWithId(Film existingFilm, Film updatedFilm) {
        String newName = updatedFilm.getName();
        int newYear = updatedFilm.getReleaseDate().getYear();

        if (existingFilm.getName().equals(newName) && existingFilm.getReleaseDate().getYear() == newYear) {
            return;
        }

        if (filmStorage.existsFilmByNameAndReleaseYearAndIdNot(newName, newYear, existingFilm.getId())) {
            throw new DuplicateException(buildDuplicateErrorMessage(newName, newYear));
        }
    }

    @Override
    public String buildDuplicateErrorMessage(String name, int releaseYear) {
        return String.format("Фильм с названием '%s' и годом выхода '%d' уже существует", name, releaseYear);
    }
}