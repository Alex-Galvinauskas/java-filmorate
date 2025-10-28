package ru.yandex.practicum.filmorate.managment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты хранилища фильмов")
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

    @Nested
    @DisplayName("Тесты создания фильмов")
    class CreateFilmTests {

        @Test
        @DisplayName("Создание фильма с валидными данными присваивает ID и сохраняет фильм")
        void createFilm_ValidFilm_AssignsIdAndStoresFilmTest() {
            Film film = createTestFilm();

            Film result = filmStorage.createFilm(film);

            assertNotNull(result.getId());
            assertEquals("Test Film", result.getName());
            assertTrue(filmStorage.existsFilmById(result.getId()));
        }

        @Test
        @DisplayName("Создание нескольких фильмов присваивает уникальные ID")
        void createFilm_MultipleFilms_AssignsUniqueIdsTest() {
            Film film1 = filmStorage.createFilm(createTestFilm());
            Film film2 = filmStorage.createFilm(createTestFilm());

            assertNotNull(film1.getId());
            assertNotNull(film2.getId());
            assertNotEquals(film1.getId(), film2.getId());
        }

        @Test
        @DisplayName("Создание фильма с null названием корректно обрабатывается")
        void createFilm_NullName_HandledCorrectlyTest() {
            Film film = createTestFilm();
            film.setName(null);

            Film result = filmStorage.createFilm(film);

            assertNotNull(result.getId());
            assertNull(result.getName());
        }
    }

    @Nested
    @DisplayName("Тесты получения фильмов")
    class GetFilmTests {

        @Test
        @DisplayName("Получение всех фильмов возвращает все сохраненные фильмы")
        void getAllFilms_ReturnsAllStoredFilmsTest() {
            Film film1 = filmStorage.createFilm(createTestFilm());
            Film film2 = filmStorage.createFilm(createTestFilm());

            List<Film> result = filmStorage.getAllFilms();

            assertEquals(2, result.size());
            assertTrue(result.contains(film1));
            assertTrue(result.contains(film2));
        }

        @Test
        @DisplayName("Получение всех фильмов при пустом хранилище возвращает пустой список")
        void getAllFilms_EmptyStorage_ReturnsEmptyListTest() {
            List<Film> result = filmStorage.getAllFilms();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Получение фильма по существующему ID возвращает фильм")
        void getFilmById_ExistingId_ReturnsFilmTest() {
            Film film = filmStorage.createFilm(createTestFilm());
            Long filmId = film.getId();

            Optional<Film> result = filmStorage.getFilmById(filmId);

            assertTrue(result.isPresent());
            assertEquals(filmId, result.get().getId());
        }

        @Test
        @DisplayName("Получение фильма по несуществующему ID возвращает пустой Optional")
        void getFilmById_NonExistingId_ReturnsEmptyTest() {
            Optional<Film> result = filmStorage.getFilmById(999L);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Получение фильма по null ID возвращает пустой Optional")
        void getFilmById_NullId_ReturnsEmptyTest() {
            Optional<Film> result = filmStorage.getFilmById(null);
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Проверка существования фильма с null названием возвращает false")
        void existsFilmByNameAndReleaseYear_NullName_ReturnsFalseTest() {
            boolean result = filmStorage.existsFilmByNameAndReleaseYear(null, 2000);
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Тесты обновления фильмов")
    class UpdateFilmTests {

        @Test
        @DisplayName("Обновление существующего фильма обновляет его данные")
        void updateFilm_ExistingFilm_UpdatesFilmTest() {
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
        void updateFilm_NonExistingFilm_ThrowsNotFoundExceptionTest() {
            Film film = createTestFilm();
            film.setId(999L);

            assertThrows(NotFoundException.class, () -> filmStorage.updateFilm(film));
        }

        @Test
        @DisplayName("Обновление фильма сохраняет связь в хранилище")
        void updateFilm_ExistingFilm_MaintainsStorageConsistencyTest() {
            Film film = filmStorage.createFilm(createTestFilm());
            Film updatedFilm = Film.builder()
                    .id(film.getId())
                    .name("Updated Film")
                    .description("Updated Description")
                    .releaseDate(LocalDate.of(2001, 1, 1))
                    .duration(150)
                    .build();

            filmStorage.updateFilm(updatedFilm);
            Optional<Film> retrievedFilm = filmStorage.getFilmById(film.getId());

            assertTrue(retrievedFilm.isPresent());
            assertEquals("Updated Film", retrievedFilm.get().getName());
        }
    }

    @Nested
    @DisplayName("Тесты проверки существования фильмов")
    class ExistenceTests {

        @Test
        @DisplayName("Проверка существования фильма по ID для существующего фильма возвращает true")
        void existsFilmById_ExistingFilm_ReturnsTrueTest() {
            Film film = filmStorage.createFilm(createTestFilm());

            boolean result = filmStorage.existsFilmById(film.getId());

            assertTrue(result);
        }

        @Test
        @DisplayName("Проверка существования фильма по ID для несуществующего фильма возвращает false")
        void existsFilmById_NonExistingFilm_ReturnsFalseTest() {
            boolean result = filmStorage.existsFilmById(999L);

            assertFalse(result);
        }

        @Test
        @DisplayName("Проверка существования по названию и году выпуска для существующего фильма возвращает true")
        void existsFilmByNameAndReleaseYear_ExistingFilm_ReturnsTrueTest() {
            Film film = filmStorage.createFilm(createTestFilm());

            boolean result = filmStorage.existsFilmByNameAndReleaseYear(
                    "Test Film",
                    film.getReleaseDate().getYear()
            );

            assertTrue(result);
        }

        @Test
        @DisplayName("Проверка существования по названию и году выпуска для несуществующего фильма возвращает false")
        void existsFilmByNameAndReleaseYear_NonExistingFilm_ReturnsFalseTest() {
            boolean result = filmStorage.existsFilmByNameAndReleaseYear("Non Existing", 1999);

            assertFalse(result);
        }

        @Test
        @DisplayName("Проверка существования по названию и году выпуска с другим названием возвращает false")
        void existsFilmByNameAndReleaseYear_DifferentName_ReturnsFalseTest() {
            Film film = filmStorage.createFilm(createTestFilm());

            boolean result = filmStorage.existsFilmByNameAndReleaseYear(
                    "Different Name",
                    film.getReleaseDate().getYear()
            );

            assertFalse(result);
        }

        @Test
        @DisplayName("Проверка существования фильма по названию и году выпуска с другим годом возвращает false")
        void existsFilmByNameAndReleaseYear_DifferentYear_ReturnsFalseTest() {
            Film film = filmStorage.createFilm(createTestFilm());

            boolean result = filmStorage.existsFilmByNameAndReleaseYear(
                    "Test Film",
                    1999
            );

            assertFalse(result);
        }

        @Test
        @DisplayName("Проверка существования фильма с null годом возвращает false")
        void existsFilmByNameAndReleaseYear_NullYear_ReturnsFalseTest() {
            boolean result = filmStorage.existsFilmByNameAndReleaseYear("Test Film", null);
            assertFalse(result);
        }
    }
}