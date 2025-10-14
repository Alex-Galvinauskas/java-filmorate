package ru.yandex.practicum.filmorate.managment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryFilmStorageTest {

    private InMemoryFilmStorage filmStorage;

    @BeforeEach
    void setUp() {
        filmStorage = new InMemoryFilmStorage();
    }

    private Film createTestFilm() {
        return Film.builder()
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .build();
    }

    @Test
    @DisplayName("Создание фильма с валидными данными присваивает ID и сохраняет фильм")
    void createFilm_ValidFilm_AssignsIdAndStoresFilm() {
        Film film = createTestFilm();

        Film result = filmStorage.createFilm(film);

        assertNotNull(result.getId());
        assertEquals("Test Film", result.getName());
        assertTrue(filmStorage.existsFilmById(result.getId()));
    }

    @Test
    @DisplayName("Получение всех фильмов возвращает все сохраненные фильмы")
    void getAllFilms_ReturnsAllStoredFilms() {
        Film film1 = filmStorage.createFilm(createTestFilm());
        Film film2 = filmStorage.createFilm(createTestFilm());

        List<Film> result = filmStorage.getAllFilms();

        assertEquals(2, result.size());
        assertTrue(result.contains(film1));
        assertTrue(result.contains(film2));
    }

    @Test
    @DisplayName("Получение фильма по существующему ID возвращает фильм")
    void getFilmById_ExistingId_ReturnsFilm() {
        Film film = filmStorage.createFilm(createTestFilm());
        Long filmId = film.getId();

        Optional<Film> result = filmStorage.getFilmById(filmId);

        assertTrue(result.isPresent());
        assertEquals(filmId, result.get().getId());
    }

    @Test
    @DisplayName("Получение фильма по несуществующему ID возвращает пустой Optional")
    void getFilmById_NonExistingId_ReturnsEmpty() {
        Optional<Film> result = filmStorage.getFilmById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Обновление существующего фильма обновляет его данные")
    void updateFilm_ExistingFilm_UpdatesFilm() {
        Film film = filmStorage.createFilm(createTestFilm());
        Film updatedFilm = Film.builder()
                .id(film.getId())
                .name("Updated Film")
                .description("Updated Description")
                .releaseDate(LocalDate.of(2001, 1, 1))
                .duration(150)
                .build();

        Film result = filmStorage.updateFilm(updatedFilm);

        assertEquals("Updated Film", result.getName());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(150, result.getDuration());
    }

    @Test
    @DisplayName("Обновление несуществующего фильма выбрасывает NotFoundException")
    void updateFilm_NonExistingFilm_ThrowsNotFoundException() {
        Film film = createTestFilm();
        film.setId(999L);

        assertThrows(NotFoundException.class, () -> filmStorage.updateFilm(film));
    }

    @Test
    @DisplayName("Проверка существования фильма по названию и году выпуска для существующего фильма возвращает true")
    void existsFilmByNameAndReleaseYear_ExistingFilm_ReturnsTrue() {
        Film film = filmStorage.createFilm(createTestFilm());

        boolean result = filmStorage.existsFilmByNameAndReleaseYear(
                "Test Film",
                film.getReleaseDate().getYear()
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("Проверка существования фильма по названию и году выпуска для несуществующего фильма возвращает false")
    void existsFilmByNameAndReleaseYear_NonExistingFilm_ReturnsFalse() {
        boolean result = filmStorage.existsFilmByNameAndReleaseYear("Non Existing", 1999);

        assertFalse(result);
    }
}