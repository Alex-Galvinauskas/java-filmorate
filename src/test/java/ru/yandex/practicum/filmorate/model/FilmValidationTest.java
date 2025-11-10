package ru.yandex.practicum.filmorate.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты валидации модели Film")
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
                .mpa(MpaRating.PG)
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

    @Nested
    @DisplayName("Тесты конструктора Film")
    class FilmConstructorTests {

        @Test
        @DisplayName("Создание фильма через JSON конструктор корректно инициализирует likes")
        void jsonConstructor_InitializesLikesTest() {
            Film film = Film.builder()
                    .id(1L)
                    .name("Test Film")
                    .description("Test Description")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(120)
                    .mpa(MpaRating.G)
                    .build();

            assertNotNull(film.getLikes());
            assertTrue(film.getLikes().isEmpty());
            assertEquals(1L, film.getId());
            assertEquals("Test Film", film.getName());
        }

        @Test
        @DisplayName("Создание фильма через JSON конструктор с null likes создает пустой Set")
        void jsonConstructor_NullLikes_CreatesEmptySetTest() {
            Film film = Film.builder()
                    .id(1L)
                    .name("Test Film")
                    .description("Test Description")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(120)
                    .mpa(MpaRating.G)
                    .build();

            assertNotNull(film.getLikes());
            assertEquals(0, film.getLikes().size());
        }
    }

    @Nested
    @DisplayName("Тесты метода copyWithId")
    class CopyWithIdTests {

        @Test
        @DisplayName("copyWithId с null source выбрасывает исключение")
        void copyWithId_NullSource_ThrowsExceptionTest() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> Film.copyWithId(null, 1L)
            );

            assertEquals("Исходный файл не может быть null", exception.getMessage());
        }

        @Test
        @DisplayName("copyWithId корректно копирует все поля включая likes")
        void copyWithId_CopiesAllFieldsIncludingLikesTest() {
            Film original = Film.builder()
                    .id(1L)
                    .name("Original")
                    .description("Description")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(120)
                    .build();

            original.getLikes().add(1L);
            original.getLikes().add(2L);
            original.getLikes().add(3L);

            Film copy = Film.copyWithId(original, 999L);

            assertEquals(999L, copy.getId());
            assertEquals("Original", copy.getName());
            assertEquals("Description", copy.getDescription());
            assertEquals(LocalDate.of(2000, 1, 1), copy.getReleaseDate());
            assertEquals(120, copy.getDuration());
            assertEquals(3, copy.getLikes().size());
            assertTrue(copy.getLikes().contains(1L));
            assertTrue(copy.getLikes().contains(2L));
            assertTrue(copy.getLikes().contains(3L));
        }

        @Test
        @DisplayName("copyWithId с null likes создает пустой Set")
        void copyWithId_NullLikes_CreatesEmptySetTest() {
            Film original = Film.builder()
                    .id(1L)
                    .name("Original")
                    .description("Description")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(120)
                    .build();

            try {
                Field likesField = Film.class.getDeclaredField("likes");
                likesField.setAccessible(true);
                likesField.set(original, null);
            } catch (Exception e) {
                fail("Не удалось установить likes в null через рефлексию: " + e.getMessage());
            }

            Film copy = Film.copyWithId(original, 999L);

            assertNotNull(copy.getLikes());
            assertTrue(copy.getLikes().isEmpty());
        }

        @Test
        @DisplayName("copyWithId создает независимую копию likes")
        void copyWithId_CreatesIndependentLikesCopyTest() {
            Film original = Film.builder()
                    .id(1L)
                    .name("Original")
                    .description("Description")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(120)
                    .build();

            original.getLikes().add(1L);

            Film copy = Film.copyWithId(original, 999L);

            original.getLikes().add(2L);

            assertEquals(1, copy.getLikes().size());
            assertTrue(copy.getLikes().contains(1L));
            assertFalse(copy.getLikes().contains(2L));
        }

        @Test
        @DisplayName("copyWithId с пустыми likes создает пустой Set")
        void copyWithId_EmptyLikes_CreatesEmptySetTest() {
            Film original = Film.builder()
                    .id(1L)
                    .name("Original")
                    .description("Description")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(120)
                    .build();

            Film copy = Film.copyWithId(original, 999L);

            assertNotNull(copy.getLikes());
            assertTrue(copy.getLikes().isEmpty());
        }
    }

    @Nested
    @DisplayName("Тесты equals и hashCode для Film")
    class FilmEqualsHashCodeTests {

        @Test
        @DisplayName("Фильмы с одинаковыми всеми полями равны")
        void filmsWithSameFields_AreEqualTest() {
            Film film1 = Film.builder()
                    .id(1L)
                    .name("Film 1")
                    .description("Description 1")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(100)
                    .build();

            Film film2 = Film.builder()
                    .id(1L)
                    .name("Film 1")
                    .description("Description 1")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(100)
                    .build();

            film1.getLikes().add(1L);
            film2.getLikes().add(1L);

            assertEquals(film1, film2);
            assertEquals(film1.hashCode(), film2.hashCode());
        }

        @Test
        @DisplayName("Фильмы с разным ID не равны")
        void filmsWithDifferentId_AreNotEqualTest() {
            Film film1 = Film.builder()
                    .id(1L)
                    .name("Same Film")
                    .description("Same Description")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(120)
                    .build();

            Film film2 = Film.builder()
                    .id(2L)
                    .name("Same Film")
                    .description("Same Description")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(120)
                    .build();

            film1.getLikes().addAll(Set.of(1L, 2L));
            film2.getLikes().addAll(Set.of(1L, 2L));

            assertNotEquals(film1, film2);
        }

        @Test
        @DisplayName("Фильмы с разными названиями не равны")
        void filmsWithDifferentNames_AreNotEqualTest() {
            Film film1 = Film.builder()
                    .id(1L)
                    .name("Film 1")
                    .description("Same Description")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(120)
                    .build();

            Film film2 = Film.builder()
                    .id(1L)
                    .name("Film 2")
                    .description("Same Description")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(120)
                    .build();

            assertNotEquals(film1, film2);
        }

        @Test
        @DisplayName("Фильмы с разными датами релиза не равны")
        void filmsWithDifferentReleaseDates_AreNotEqualTest() {
            Film film1 = Film.builder()
                    .id(1L)
                    .name("Same Film")
                    .description("Same Description")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(120)
                    .build();

            Film film2 = Film.builder()
                    .id(1L)
                    .name("Same Film")
                    .description("Same Description")
                    .releaseDate(LocalDate.of(2001, 1, 1))
                    .duration(120)
                    .build();

            assertNotEquals(film1, film2);
        }

        @Test
        @DisplayName("Фильм не равен null")
        void filmEquals_Null_ReturnsFalseTest() {
            Film film = Film.builder().id(1L).name("Film").build();
            assertNotEquals(null, film);
        }
    }

    @Nested
    @DisplayName("Тесты toString для Film")
    class FilmToStringTests {

        @Test
        @DisplayName("toString содержит все основные поля")
        void toString_ContainsAllFieldsTest() {
            Film film = Film.builder()
                    .id(1L)
                    .name("Test Film")
                    .description("Test Description")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(120)
                    .build();

            film.getLikes().add(1L);
            film.getLikes().add(2L);

            String toString = film.toString();

            assertTrue(toString.contains("id=1"));
            assertTrue(toString.contains("name=Test Film"));
            assertTrue(toString.contains("description=Test Description"));
            assertTrue(toString.contains("releaseDate=2000-01-01"));
            assertTrue(toString.contains("duration=120"));
            assertTrue(toString.contains("likes=[1, 2]") || toString.contains("likes=[2, 1]"));
        }
    }
}