package ru.yandex.practicum.filmorate.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.managment.inMemory.FilmStorage;
import ru.yandex.practicum.filmorate.service.film.validation.FilmValidatorImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты валидации фильмов")
class FilmValidationTest {

    @Mock
    private FilmStorage filmStorage;

    @InjectMocks
    private FilmValidatorImpl filmValidator;

    @Nested
    @DisplayName("Тесты построения сообщений об ошибках")
    class BuildErrorMessageTests {

        @Test
        @DisplayName("Построение сообщения о дубликате фильма")
        void buildDuplicateErrorMessage_ValidParameters_ReturnsFormattedMessage() {
            String result = filmValidator.buildDuplicateErrorMessage("Test Film", 2000);

            assertEquals("Фильм с названием 'Test Film' и годом выхода '2000' уже существует",
                    result);
        }
    }

    @Nested
    @DisplayName("Тесты валидации уникальности")
    class ValidateUniquenessTests {

        @Test
        @DisplayName("Валидация уникальности - фильм с таким названием и годом существует")
        void validateFilmUniqueness_FilmExists_ThrowsDuplicateException() {
            when(filmStorage.existsFilmByNameAndReleaseYear("Existing Film", 2000))
                    .thenReturn(true);

            DuplicateException exception = assertThrows(DuplicateException.class,
                    () -> filmValidator.validateFilmUniqueness("Existing Film", 2000));

            assertTrue(exception.getMessage().contains("Existing Film"));
            assertTrue(exception.getMessage().contains("2000"));
        }

        @Test
        @DisplayName("Валидация уникальности - фильм не существует")
        void validateFilmUniqueness_FilmNotExists_NoException() {
            when(filmStorage.existsFilmByNameAndReleaseYear("New Film", 2000))
                    .thenReturn(false);

            assertDoesNotThrow(() ->
                    filmValidator.validateFilmUniqueness("New Film", 2000));
            verify(filmStorage, times(1))
                    .existsFilmByNameAndReleaseYear("New Film", 2000);
        }
    }

    @Nested
    @DisplayName("Тесты валидации уникальности при обновлении")
    class ValidateUniquenessForUpdateTests {

        @Test
        @DisplayName("Валидация уникальности для обновления - название и год не изменились")
        void validateFilmUniquenessForUpdate_SameNameAndYear_NoException() {
            Film existingFilm = createTestFilm(1L, "Test Film", 2000,
                    List.of(createDirector(1L)));
            Film updatedFilm = createTestFilm(1L, "Test Film", 2000,
                    List.of(createDirector(1L)));

            assertDoesNotThrow(() ->
                    filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));
            verify(filmStorage, never())
                    .existsFilmByNameAndReleaseYearExcludingId(anyString(), anyInt(),
                            anyLong());
        }

        @Test
        @DisplayName("Валидация уникальности для обновления - изменилось только название, режиссеры те же")
        void validateFilmUniquenessForUpdate_NameChangedSameDirectors_ValidatesUniqueness() {
            Director director = createDirector(1L);
            Film existingFilm = createTestFilm(1L, "Old Film", 2000, List.of(director));
            Film updatedFilm = createTestFilm(1L, "New Film", 2000, List.of(director));

            when(filmStorage.existsFilmByNameAndReleaseYearExcludingId("New Film",
                    2000, 1L))
                    .thenReturn(false);

            assertDoesNotThrow(() ->
                    filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));
            verify(filmStorage, times(1))
                    .existsFilmByNameAndReleaseYearExcludingId("New Film", 2000, 1L);
        }

        @Test
        @DisplayName("Валидация уникальности для обновления - изменился только год, режиссеры те же")
        void validateFilmUniquenessForUpdate_YearChangedSameDirectors_ValidatesUniqueness() {
            Director director = createDirector(1L);
            Film existingFilm = createTestFilm(1L, "Test Film", 2000,
                    List.of(director));
            Film updatedFilm = createTestFilm(1L, "Test Film", 2001, List.of(director));

            when(filmStorage.existsFilmByNameAndReleaseYearExcludingId("Test Film",
                    2001, 1L))
                    .thenReturn(false);

            assertDoesNotThrow(() ->
                    filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));
            verify(filmStorage, times(1))
                    .existsFilmByNameAndReleaseYearExcludingId("Test Film", 2001, 1L);
        }

        @Test
        @DisplayName("Валидация уникальности для обновления - изменились и название и год, режиссеры те же")
        void validateFilmUniquenessForUpdate_NameAndYearChangedSameDirectors_ValidatesUniqueness() {
            Director director = createDirector(1L);
            Film existingFilm = createTestFilm(1L, "Old Film", 2000, List.of(director));
            Film updatedFilm = createTestFilm(1L, "New Film", 2001, List.of(director));

            when(filmStorage.existsFilmByNameAndReleaseYearExcludingId("New Film",
                    2001, 1L))
                    .thenReturn(false);

            assertDoesNotThrow(() ->
                    filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));
            verify(filmStorage, times(1))
                    .existsFilmByNameAndReleaseYearExcludingId("New Film", 2001, 1L);
        }

        @Test
        @DisplayName("Валидация уникальности для обновления - дубликат при изменении названия, режиссеры те же")
        void validateFilmUniquenessForUpdate_NameChangedDuplicateSameDirectors_ThrowsException() {
            Director director = createDirector(1L);
            Film existingFilm = createTestFilm(1L, "Old Film", 2000,
                    List.of(director));
            Film updatedFilm = createTestFilm(1L, "Existing Film", 2000,
                    List.of(director));

            when(filmStorage.existsFilmByNameAndReleaseYearExcludingId("Existing Film",
                    2000, 1L))
                    .thenReturn(true);

            DuplicateException exception = assertThrows(DuplicateException.class,
                    () -> filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));

            assertTrue(exception.getMessage().contains("Existing Film"));
            assertTrue(exception.getMessage().contains("2000"));
            verify(filmStorage, times(1))
                    .existsFilmByNameAndReleaseYearExcludingId("Existing Film", 2000, 1L);
        }

        @Test
        @DisplayName("Валидация уникальности для обновления - режиссеры изменились")
        void validateFilmUniquenessForUpdate_DirectorsChanged_NoValidationNeeded() {
            Film existingFilm = createTestFilm(1L, "Test Film", 2000,
                    List.of(createDirector(1L)));
            Film updatedFilm = createTestFilm(1L, "Test Film", 2000,
                    List.of(createDirector(2L)));

            assertDoesNotThrow(() ->
                    filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));
            verify(filmStorage, never())
                    .existsFilmByNameAndReleaseYearExcludingId(anyString(), anyInt(),
                            anyLong());
        }

        @Test
        @DisplayName("Валидация уникальности для обновления - у существующего фильма нет режиссеров, у нового есть")
        void validateFilmUniquenessForUpdate_ExistingFilmHasNoDirectorsNewHasDirectors_NoValidationNeeded() {
            Film existingFilm = createTestFilm(1L, "Test Film", 2000, new ArrayList<>());
            Film updatedFilm = createTestFilm(1L, "Test Film", 2000,
                    List.of(createDirector(1L)));

            assertDoesNotThrow(() ->
                    filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));
            verify(filmStorage, never())
                    .existsFilmByNameAndReleaseYearExcludingId(anyString(), anyInt(),
                            anyLong());
        }

        @Test
        @DisplayName("Валидация уникальности для обновления - у существующего фильма есть режиссеры, у нового нет")
        void validateFilmUniquenessForUpdate_ExistingFilmHasDirectorsNewHasNoDirectors_NoValidationNeeded() {
            Film existingFilm = createTestFilm(1L, "Test Film", 2000,
                    List.of(createDirector(1L)));
            Film updatedFilm = createTestFilm(1L, "Test Film", 2000, new ArrayList<>());

            assertDoesNotThrow(() ->
                    filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));
            verify(filmStorage, never())
                    .existsFilmByNameAndReleaseYearExcludingId(anyString(), anyInt(),
                            anyLong());
        }

        @Test
        @DisplayName("Валидация уникальности для обновления - у обоих фильмов нет режиссеров, название изменилось")
        void validateFilmUniquenessForUpdate_BothFilmsHaveNoDirectorsNameChanged_NoValidationNeeded() {
            Film existingFilm = createTestFilm(1L, "Old Film", 2000, new ArrayList<>());
            Film updatedFilm = createTestFilm(1L, "New Film", 2000, new ArrayList<>());

            assertDoesNotThrow(() ->
                    filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));
            verify(filmStorage, never())
                    .existsFilmByNameAndReleaseYearExcludingId(anyString(), anyInt(),
                            anyLong());
        }
    }

    /**
     * Вспомогательный метод для создания тестового фильма
     */
    private Film createTestFilm(Long id, String name, int year, List<Director> directors) {
        return Film.builder()
                .id(id)
                .name(name)
                .description("Test Description")
                .releaseDate(LocalDate.of(year, 1, 1))
                .duration(120)
                .directors(directors != null ? directors : new ArrayList<>())
                .build();
    }

    /**
     * Вспомогательный метод для создания режиссера
     */
    private Director createDirector(Long id) {
        return Director.builder()
                .id(id)
                .name("Director " + id)
                .build();
    }
}