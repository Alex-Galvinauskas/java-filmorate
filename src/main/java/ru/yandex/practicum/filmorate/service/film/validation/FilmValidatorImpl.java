package ru.yandex.practicum.filmorate.service.film.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.managment.inMemory.FilmStorage;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class FilmValidatorImpl implements FilmValidatorRules {

    private final FilmStorage filmStorage;

    /**
     * Проверяет уникальность фильма при обновлении.
     * Проверяет только если изменилось название ИЛИ год, учитывает режиссеров.
     */
    public void validateFilmUniquenessForUpdate(Film existingFilm, Film updatedFilm) {
        log.debug("Валидация уникальности для обновления. Существующий: ID={}, name={}, year={}. Обновляемый: name={}, year={}",
                existingFilm.getId(), existingFilm.getName(),
                existingFilm.getReleaseDate() != null ? existingFilm.getReleaseDate().getYear() : "null",
                updatedFilm.getName(),
                updatedFilm.getReleaseDate() != null ? updatedFilm.getReleaseDate().getYear() : "null");

        boolean nameChanged = !existingFilm.getName().equals(updatedFilm.getName());
        boolean yearChanged = existingFilm.getReleaseDate().getYear() != updatedFilm.getReleaseDate().getYear();

        if (nameChanged || yearChanged) {
            Set<Long> existingDirectors = existingFilm.getDirectors().stream()
                    .map(Director::getId)
                    .collect(Collectors.toSet());

            Set<Long> updatedDirectors = updatedFilm.getDirectors().stream()
                    .map(Director::getId)
                    .collect(Collectors.toSet());

            if (!existingDirectors.isEmpty() && !updatedDirectors.isEmpty() &&
                    existingDirectors.equals(updatedDirectors)) {
                validateFilmUniquenessExcludingId(
                        updatedFilm.getName(),
                        updatedFilm.getReleaseDate().getYear(),
                        existingFilm.getId()
                );
            } else {
                log.debug("Режиссеры изменились или разные, разрешаем обновление");
            }
        } else {
            log.debug("Название и год не изменились, пропускаем проверку уникальности");
        }
    }

    /**
     * Проверяет уникальность фильма по названию и году выпуска.
     * Вызывается только при создании нового фильма.
     */
    public void validateFilmUniqueness(String name, int releaseYear) {
        log.debug("Проверка уникальности фильма: {} ({})", name, releaseYear);

        if (filmStorage.existsFilmByNameAndReleaseYear(name, releaseYear)) {
            throw new DuplicateException(buildDuplicateErrorMessage(name, releaseYear));
        }
    }

    /**
     * Проверяет уникальность фильма по названию и году выпуска, исключая указанный ID.
     */
    private void validateFilmUniquenessExcludingId(String name, int releaseYear, Long excludedId) {
        log.debug("Проверка уникальности фильма: {} ({}), исключая ID: {}", name, releaseYear, excludedId);

        if (filmStorage.existsFilmByNameAndReleaseYearExcludingId(name, releaseYear, excludedId)) {
            throw new DuplicateException(buildDuplicateErrorMessage(name, releaseYear));
        }
    }

    public String buildDuplicateErrorMessage(String name, int releaseYear) {
        return String.format("Фильм с названием '%s' и годом выхода '%s' уже существует", name, releaseYear);
    }
}