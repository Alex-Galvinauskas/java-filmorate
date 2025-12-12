package ru.yandex.practicum.filmorate.service.film.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.managment.inMemory.FilmStorage;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Nested
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты построения сообщений об ошибках и валидации для сервиса FilmValidatorImpl")
class FilmValidatorImplTest {

    @Mock
    private FilmStorage filmStorage;

    @InjectMocks
    private FilmValidatorImpl filmValidator;

    @Test
    @DisplayName("Построение сообщения о дубликате фильма")
    void buildDuplicateErrorMessage_ValidParameters_ReturnsFormattedMessageTest() {
        String result = filmValidator.buildDuplicateErrorMessage("Test Film", 2000);

        assertEquals("Фильм с названием 'Test Film' и годом выхода '2000' уже существует", result);
    }

    @Test
    @DisplayName("Валидация уникальности - фильм с таким названием и годом существует")
    void validateFilmUniqueness_FilmExists_ThrowsDuplicateExceptionTest() {
        when(filmStorage.existsFilmByNameAndReleaseYear("Existing Film", 2000))
                .thenReturn(true);

        DuplicateException exception = assertThrows(DuplicateException.class,
                () -> filmValidator.validateFilmUniqueness("Existing Film", 2000));

        assertTrue(exception.getMessage().contains("Existing Film"));
        assertTrue(exception.getMessage().contains("2000"));
    }

    @Test
    @DisplayName("Валидация уникальности для обновления - название и год не изменились")
    void validateFilmUniquenessForUpdate_SameNameAndYear_NoExceptionTest() {
        List<Director> directors = new ArrayList<>();
        directors.add(Director.builder().id(1L).name("Director1")
                .createdAt(LocalDateTime.now()).build());

        Film existingFilm = Film.builder()
                .id(1L)
                .name("Test Film")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .directors(directors)
                .build();

        Film updatedFilm = Film.builder()
                .name("Test Film")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .directors(new ArrayList<>(directors))
                .build();

        assertDoesNotThrow(() -> filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));
        verify(filmStorage, never())
                .existsFilmByNameAndReleaseYearExcludingId(anyString(),
                        anyInt(), anyLong());
        verify(filmStorage, never())
                .existsFilmByNameAndReleaseYear(anyString(), anyInt());
    }

    @Test
    @DisplayName("Валидация уникальности для обновления - изменилось только название")
    void validateFilmUniquenessForUpdate_NameChanged_ValidatesUniquenessTest() {
        List<Director> directors = new ArrayList<>();
        directors.add(Director.builder().id(1L).name("Director1")
                .createdAt(LocalDateTime.now()).build());

        Film existingFilm = Film.builder()
                .id(1L)
                .name("Old Film")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .directors(directors)
                .build();

        Film updatedFilm = Film.builder()
                .name("New Film")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .directors(new ArrayList<>(directors))
                .build();

        when(filmStorage.existsFilmByNameAndReleaseYearExcludingId("New Film",
                2000, 1L))
                .thenReturn(false);

        assertDoesNotThrow(() -> filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));

        try {
            verify(filmStorage, times(1))
                    .existsFilmByNameAndReleaseYearExcludingId("New Film", 2000, 1L);
        } catch (AssertionError e) {
            verify(filmStorage, times(1))
                    .existsFilmByNameAndReleaseYear("New Film", 2000);
        }
    }

    @Test
    @DisplayName("Валидация уникальности для обновления - изменился только год")
    void validateFilmUniquenessForUpdate_YearChanged_ValidatesUniquenessTest() {
        List<Director> directors = new ArrayList<>();
        directors.add(Director.builder().id(1L).name("Director1")
                .createdAt(LocalDateTime.now()).build());

        Film existingFilm = Film.builder()
                .id(1L)
                .name("Test Film")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .directors(directors)
                .build();

        Film updatedFilm = Film.builder()
                .name("Test Film")
                .releaseDate(LocalDate.of(2001, 1, 1))
                .directors(new ArrayList<>(directors))
                .build();

        when(filmStorage.existsFilmByNameAndReleaseYearExcludingId("Test Film",
                2001, 1L))
                .thenReturn(false);

        assertDoesNotThrow(() -> filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));

        try {
            verify(filmStorage, times(1))
                    .existsFilmByNameAndReleaseYearExcludingId("Test Film", 2001, 1L);
        } catch (AssertionError e) {
            verify(filmStorage, times(1))
                    .existsFilmByNameAndReleaseYear("Test Film", 2001);
        }
    }

    @Test
    @DisplayName("Валидация уникальности для обновления - разные режиссеры, проверка не вызывается")
    void validateFilmUniquenessForUpdate_DifferentDirectors_NoValidationTest() {
        List<Director> existingDirectors = new ArrayList<>();
        existingDirectors.add(Director.builder().id(1L).name("Director1")
                .createdAt(LocalDateTime.now()).build());

        List<Director> updatedDirectors = new ArrayList<>();
        updatedDirectors.add(Director.builder().id(2L).name("Director2")
                .createdAt(LocalDateTime.now()).build());

        Film existingFilm = Film.builder()
                .id(1L)
                .name("Test Film")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .directors(existingDirectors)
                .build();

        Film updatedFilm = Film.builder()
                .name("Test Film")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .directors(updatedDirectors)
                .build();

        assertDoesNotThrow(() -> filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));

        verify(filmStorage, never())
                .existsFilmByNameAndReleaseYearExcludingId(anyString(), anyInt(),
                        anyLong());
        verify(filmStorage, never())
                .existsFilmByNameAndReleaseYear(anyString(), anyInt());
    }

    @Test
    @DisplayName("Валидация уникальности для обновления - изменилось название на уже существующее")
    void validateFilmUniquenessForUpdate_NameChangedToExisting_ThrowsExceptionTest() {
        List<Director> directors = new ArrayList<>();
        directors.add(Director.builder().id(1L).name("Director1")
                .createdAt(LocalDateTime.now()).build());

        Film existingFilm = Film.builder()
                .id(1L)
                .name("Old Film")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .directors(directors)
                .build();

        Film updatedFilm = Film.builder()
                .name("Existing Film")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .directors(new ArrayList<>(directors))
                .build();

        when(filmStorage.existsFilmByNameAndReleaseYearExcludingId("Existing Film",
                2000, 1L))
                .thenReturn(true);

        DuplicateException exception = assertThrows(DuplicateException.class,
                () -> filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));

        assertTrue(exception.getMessage().contains("Existing Film"));
        assertTrue(exception.getMessage().contains("2000"));
    }
}