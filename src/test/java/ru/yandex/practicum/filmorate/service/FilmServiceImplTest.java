package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.FilmStorage;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilmServiceImplTest {

    @Mock
    private FilmStorage filmStorage;

    @InjectMocks
    private FilmServiceImpl filmService;

    @Test
    @DisplayName("Создание фильма с валидными данными возвращает созданный фильм")
    void createFilm_ValidFilm_ReturnsCreatedFilm() {
        Film film = createTestFilm();
        film.setId(null);

        when(filmStorage.existsFilmByNameAndReleaseYear(any(), any())).thenReturn(false);
        when(filmStorage.createFilm(any(Film.class))).thenReturn(film);

        Film result = filmService.createFilm(film);

        assertNotNull(result);
        assertEquals("Test Film", result.getName());
        verify(filmStorage, times(1)).createFilm(any(Film.class));
    }

    private Film createTestFilm() {
        return Film.builder()
                .id(1L)
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .build();
    }

    @Test
    @DisplayName("Создание дублирующего фильма выбрасывает DuplicateException")
    void createFilm_DuplicateFilm_ThrowsDuplicateException() {
        Film film = createTestFilm();
        film.setId(null);

        when(filmStorage.existsFilmByNameAndReleaseYear(any(), any())).thenReturn(true);

        assertThrows(DuplicateException.class, () -> filmService.createFilm(film));
        verify(filmStorage, never()).createFilm(any(Film.class));
    }

    @Test
    @DisplayName("Получение всех фильмов возвращает список фильмов")
    void getAllFilms_ReturnsFilmsList() {
        Film film = createTestFilm();
        when(filmStorage.getAllFilms()).thenReturn(List.of(film));

        List<Film> result = filmService.getAllFilms();

        assertEquals(1, result.size());
        assertEquals("Test Film", result.get(0).getName());
        verify(filmStorage, times(1)).getAllFilms();
    }

    @Test
    @DisplayName("Получение фильма по существующему ID возвращает фильм")
    void getFilmById_ExistingId_ReturnsFilm() {
        Film film = createTestFilm();
        when(filmStorage.getFilmById(1L)).thenReturn(Optional.of(film));

        Film result = filmService.getFilmById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(filmStorage, times(1)).getFilmById(1L);
    }

    @Test
    @DisplayName("Получение фильма по несуществующему ID выбрасывает NotFoundException")
    void getFilmById_NonExistingId_ThrowsNotFoundException() {
        when(filmStorage.getFilmById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> filmService.getFilmById(999L));
        verify(filmStorage, times(1)).getFilmById(999L);
    }

    @Test
    @DisplayName("Обновление валидного фильма возвращает обновленный фильм")
    void updateFilm_ValidFilm_ReturnsUpdatedFilm() {
        Film existingFilm = createTestFilm();
        Film updatedFilm = createTestFilm();
        updatedFilm.setName("Updated Film");

        when(filmStorage.getFilmById(1L)).thenReturn(Optional.of(existingFilm));
        when(filmStorage.existsFilmByNameAndReleaseYear(any(), any())).thenReturn(false);
        when(filmStorage.updateFilm(any(Film.class))).thenReturn(updatedFilm);

        Film result = filmService.updateFilm(updatedFilm);

        assertNotNull(result);
        assertEquals("Updated Film", result.getName());
        verify(filmStorage, times(1)).updateFilm(any(Film.class));
    }

    @Test
    @DisplayName("Обновление несуществующего фильма выбрасывает NotFoundException")
    void updateFilm_NonExistingFilm_ThrowsNotFoundException() {
        Film film = createTestFilm();
        when(filmStorage.getFilmById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> filmService.updateFilm(film));
        verify(filmStorage, never()).updateFilm(any(Film.class));
    }

    @Test
    @DisplayName("Обновление фильма на дублирующие данные выбрасывает DuplicateException")
    void updateFilm_DuplicateFilm_ThrowsDuplicateException() {
        Film existingFilm = createTestFilm();
        Film updatedFilm = createTestFilm();
        updatedFilm.setName("Different Film");

        when(filmStorage.getFilmById(1L)).thenReturn(Optional.of(existingFilm));
        when(filmStorage.existsFilmByNameAndReleaseYear(any(), any())).thenReturn(true);

        assertThrows(DuplicateException.class, () -> filmService.updateFilm(updatedFilm));
        verify(filmStorage, never()).updateFilm(any(Film.class));
    }
}