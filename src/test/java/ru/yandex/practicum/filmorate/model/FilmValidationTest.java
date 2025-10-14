package ru.yandex.practicum.filmorate.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilmValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Создание фильма с валидными данными не вызывает нарушений валидации")
    void createFilm_ValidFilm_NoViolations() {
        Film film = Film.builder()
                .name("Valid Film")
                .description("Valid Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .build();

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Создание фильма с пустым названием вызывает нарушение валидации")
    void createFilm_EmptyName_Violation() {
        Film film = Film.builder()
                .name("")
                .description("Valid Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .build();

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Название фильма не может быть пустым")));
    }

    @Test
    @DisplayName("Создание фильма с описанием длиннее 200 символов вызывает нарушение валидации")
    void createFilm_TooLongDescription_Violation() {
        String longDescription = "A".repeat(201);
        Film film = Film.builder()
                .name("Valid Film")
                .description(longDescription)
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .build();

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Описание фильма не может быть длиннее 200 символов")));
    }

    @Test
    @DisplayName("Создание фильма с датой выпуска до 28 декабря 1895 года вызывает нарушение валидации")
    void createFilm_TooEarlyReleaseDate_Violation() {
        Film film = Film.builder()
                .name("Valid Film")
                .description("Valid Description")
                .releaseDate(LocalDate.of(1890, 1, 1))
                .duration(120)
                .build();

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Дата выхода фильма не может быть раньше 28 декабря 1895 года")));
    }

    @Test
    @DisplayName("Создание фильма с отрицательной продолжительностью вызывает нарушение валидации")
    void createFilm_NegativeDuration_Violation() {
        Film film = Film.builder()
                .name("Valid Film")
                .description("Valid Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(-10)
                .build();

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Продолжительность фильма должна быть положительным числом")));
    }
}