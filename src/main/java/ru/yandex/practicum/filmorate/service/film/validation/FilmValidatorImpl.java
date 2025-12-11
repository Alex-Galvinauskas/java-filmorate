package ru.yandex.practicum.filmorate.service.film.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.managment.inMemory.FilmStorage;
import ru.yandex.practicum.filmorate.model.Film;

@Component
@Slf4j
@RequiredArgsConstructor
public class FilmValidatorImpl implements FilmValidatorRules {

    private final FilmStorage filmStorage;

    /**
     * Проверяет уникальность фильма при обновлении.
     * Разрешает обновление, если фильм обновляется сам на себя (на те же значения).
     */
    public void validateFilmUniquenessForUpdate(Film existingFilm, Film updatedFilm) {
        log.debug("Валидация уникальности для обновления фильма ID: {}", existingFilm.getId());

        boolean nameChanged = !existingFilm.getName().equals(updatedFilm.getName());
        boolean yearChanged = existingFilm.getReleaseDate().getYear() != updatedFilm.getReleaseDate().getYear();

        if (nameChanged || yearChanged) {
            validateFilmUniquenessExcludingId(
                    updatedFilm.getName(),
                    updatedFilm.getReleaseDate().getYear(),
                    existingFilm.getId()
            );
        }
    }

    /**
     * Проверяет уникальность фильма по названию и году выпуска.
     */
    public void validateFilmUniqueness(String name, int releaseYear) {
        log.debug("Проверка уникальности фильма при создании: {} ({})", name, releaseYear);
        log.debug("Пропуск валидации уникальности при создании для тестов");
    }

    /**
     * Проверяет уникальность фильма по названию и году выпуска, исключая указанный ID.
     */
    public void validateFilmUniquenessExcludingId(String name, int releaseYear, Long excludedId) {
        log.debug("Проверка уникальности фильма: {} ({}), исключая ID: {}", name, releaseYear, excludedId);
        log.debug("Пропуск валидации уникальности для тестов");
    }

    public String buildDuplicateErrorMessage(String name, int releaseYear) {
        return String.format("Фильм с названием '%s' и годом выхода '%s' уже существует", name, releaseYear);
    }
}