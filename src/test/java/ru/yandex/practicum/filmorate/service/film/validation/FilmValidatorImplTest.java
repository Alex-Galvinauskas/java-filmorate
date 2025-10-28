package ru.yandex.practicum.filmorate.service.film.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.managment.FilmStorage;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

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
        when(filmStorage.existsFilmByNameAndReleaseYear("Existing Film",
                2000)).thenReturn(true);

        DuplicateException exception = assertThrows(DuplicateException.class,
                () -> filmValidator.validateFilmUniqueness("Existing Film", 2000));

        assertTrue(exception.getMessage().contains("Existing Film"));
        assertTrue(exception.getMessage().contains("2000"));
    }

    @Test
    @DisplayName("Валидация уникальности для обновления - название и год не изменились")
    void validateFilmUniquenessForUpdate_SameNameAndYear_NoExceptionTest() {
        Film existingFilm = Film.builder()
                .name("Test Film")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .build();

        Film updatedFilm = Film.builder()
                .name("Test Film")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .build();

        assertDoesNotThrow(() -> filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));
        verify(filmStorage, never()).existsFilmByNameAndReleaseYear(anyString(),
                anyInt());
    }

    @Test
    @DisplayName("Валидация уникальности для обновления - изменилось только название")
    void validateFilmUniquenessForUpdate_NameChanged_ValidatesUniquenessTest() {
        Film existingFilm = Film.builder()
                .name("Old Film")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .build();

        Film updatedFilm = Film.builder()
                .name("New Film")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .build();

        when(filmStorage.existsFilmByNameAndReleaseYear("New Film",
                2000)).thenReturn(false);

        assertDoesNotThrow(() -> filmValidator.validateFilmUniquenessForUpdate(existingFilm, updatedFilm));
        verify(filmStorage, times(1))
                .existsFilmByNameAndReleaseYear("New Film", 2000);
    }
}