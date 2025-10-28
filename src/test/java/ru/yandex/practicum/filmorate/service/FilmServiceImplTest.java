package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.FilmStorage;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.film.FilmServiceImpl;
import ru.yandex.practicum.filmorate.service.film.validation.FilmValidatorImpl;
import ru.yandex.practicum.filmorate.service.user.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Тесты сервиса управления фильмами")
class FilmServiceImplTest {

    @Mock
    private FilmStorage filmStorage;

    @Mock
    private FilmValidatorImpl filmValidator;

    @Mock
    private UserService userService;

    @InjectMocks
    private FilmServiceImpl filmService;

    private Film createTestFilm() {
        return Film.builder()
                .id(1L)
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .build();
    }

    private User createTestUser() {
        return User.builder()
                .id(1L)
                .email("test@example.com")
                .login("testlogin")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Nested
    @DisplayName("Тесты создания фильмов")
    class CreateFilmTests {

        @Test
        @DisplayName("Создание фильма с валидными данными возвращает созданный фильм")
        void createFilm_ValidFilm_ReturnsCreatedFilmTest() {
            Film film = createTestFilm();
            film.setId(null);

            when(filmStorage.existsFilmByNameAndReleaseYear(any(),
                    any())).thenReturn(false);
            when(filmStorage.createFilm(any(Film.class))).thenReturn(film);

            Film result = filmService.createFilm(film);

            assertNotNull(result);
            assertEquals("Test Film", result.getName());
            verify(filmStorage, times(1)).createFilm(any(Film.class));
        }

        @Test
        @DisplayName("Создание дублирующего фильма выбрасывает DuplicateException")
        void createFilm_DuplicateFilm_ThrowsDuplicateExceptionTest() {
            Film film = createTestFilm();
            film.setId(null);

            doThrow(new DuplicateException("Фильм с таким названием и годом выпуска уже существует"))
                    .when(filmValidator).validateFilmUniqueness(anyString(), anyInt());

            assertThrows(DuplicateException.class, () -> filmService.createFilm(film));
            verify(filmStorage, never()).createFilm(any(Film.class));
        }
    }

    @Nested
    @DisplayName("Тесты получения фильмов")
    class GetFilmTests {

        @Test
        @DisplayName("Получение всех фильмов возвращает список фильмов")
        void getAllFilms_ReturnsFilmsListTest() {
            Film film = createTestFilm();
            when(filmStorage.getAllFilms()).thenReturn(List.of(film));

            List<Film> result = filmService.getAllFilms();

            assertEquals(1, result.size());
            assertEquals("Test Film", result.getFirst().getName());
            verify(filmStorage, times(1)).getAllFilms();
        }

        @Test
        @DisplayName("Получение фильма по существующему ID возвращает фильм")
        void getFilmById_ExistingId_ReturnsFilmTest() {
            Film film = createTestFilm();
            when(filmStorage.getFilmById(1L)).thenReturn(Optional.of(film));

            Film result = filmService.getFilmById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(filmStorage, times(1)).getFilmById(1L);
        }

        @Test
        @DisplayName("Получение фильма по несуществующему ID выбрасывает NotFoundException")
        void getFilmById_NonExistingId_ThrowsNotFoundExceptionTest() {
            when(filmStorage.getFilmById(999L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> filmService.getFilmById(999L));
            verify(filmStorage, times(1)).getFilmById(999L);
        }
    }

    @Nested
    @DisplayName("Тесты обновления фильмов")
    class UpdateFilmTests {

        @Test
        @DisplayName("Обновление валидного фильма возвращает обновленный фильм")
        void updateFilm_ValidFilm_ReturnsUpdatedFilmTest() {
            Film existingFilm = createTestFilm();
            Film updatedFilm = createTestFilm();
            updatedFilm.setName("Updated Film");

            when(filmStorage.getFilmById(1L)).thenReturn(Optional.of(existingFilm));
            when(filmStorage.existsFilmByNameAndReleaseYear(any(), any()))
                    .thenReturn(false);
            when(filmStorage.updateFilm(any(Film.class))).thenReturn(updatedFilm);

            Film result = filmService.updateFilm(updatedFilm);

            assertNotNull(result);
            assertEquals("Updated Film", result.getName());
            verify(filmStorage, times(1)).updateFilm(any(Film.class));
        }

        @Test
        @DisplayName("Обновление несуществующего фильма выбрасывает NotFoundException")
        void updateFilm_NonExistingFilm_ThrowsNotFoundExceptionTest() {
            Film film = createTestFilm();
            when(filmStorage.getFilmById(1L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> filmService.updateFilm(film));
            verify(filmStorage, never()).updateFilm(any(Film.class));
        }

        @Test
        @DisplayName("Обновление фильма на дублирующие данные выбрасывает DuplicateException")
        void updateFilm_DuplicateFilm_ThrowsDuplicateExceptionTest() {
            Film existingFilm = createTestFilm();
            Film updatedFilm = createTestFilm();
            updatedFilm.setName("Different Film");

            when(filmStorage.getFilmById(1L)).thenReturn(Optional.of(existingFilm));
            doThrow(new DuplicateException("Фильм с таким названием и годом выпуска уже существует"))
                    .when(filmValidator).validateFilmUniquenessForUpdate(any(Film.class),
                            any(Film.class));

            assertThrows(DuplicateException.class, () -> filmService.updateFilm(updatedFilm));
            verify(filmStorage, never()).updateFilm(any(Film.class));
        }
    }

    @Nested
    @DisplayName("Тесты управления лайками")
    class LikeManagementTests {

        @Test
        @DisplayName("Добавление лайка - фильм и пользователь существуют")
        void addLike_BothExist_AddsLikeTest() {
            Film film = createTestFilm();
            User user = createTestUser();

            when(filmStorage.getFilmById(1L)).thenReturn(Optional.of(film));
            when(userService.getUserById(1L)).thenReturn(user);
            when(filmStorage.updateFilm(any(Film.class))).thenReturn(film);

            filmService.addLike(1L, 1L);

            assertTrue(film.getLikes().contains(1L));
            verify(filmStorage, times(1)).updateFilm(film);
        }

        @Test
        @DisplayName("Добавление лайка - фильм не существует")
        void addLike_FilmNotExist_ThrowsNotFoundExceptionTest() {
            when(filmStorage.getFilmById(1L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> filmService
                    .addLike(1L, 1L));
            verify(userService, never()).getUserById(anyLong());
        }

        @Test
        @DisplayName("Добавление лайка - пользователь не существует")
        void addLike_UserNotExist_ThrowsNotFoundExceptionTest() {
            Film film = createTestFilm();
            when(filmStorage.getFilmById(1L)).thenReturn(Optional.of(film));
            when(userService.getUserById(1L))
                    .thenThrow(new NotFoundException("Пользователь не найден"));

            assertThrows(NotFoundException.class, () -> filmService
                    .addLike(1L, 1L));
        }

        @Test
        @DisplayName("Удаление лайка - лайк существует")
        void removeLike_LikeExists_RemovesLikeTest() {
            Film film = createTestFilm();
            film.getLikes().add(1L);
            User user = createTestUser();

            when(filmStorage.getFilmById(1L)).thenReturn(Optional.of(film));
            when(userService.getUserById(1L)).thenReturn(user);
            when(filmStorage.updateFilm(any(Film.class))).thenReturn(film);

            filmService.removeLike(1L, 1L);

            assertFalse(film.getLikes().contains(1L));
            verify(filmStorage, times(1)).updateFilm(film);
        }

        @Test
        @DisplayName("Удаление лайка - лайк не существует")
        void removeLike_LikeNotExists_LogsWarningTest() {
            Film film = createTestFilm();
            User user = createTestUser();

            when(filmStorage.getFilmById(1L)).thenReturn(Optional.of(film));
            when(userService.getUserById(1L)).thenReturn(user);

            filmService.removeLike(1L, 1L);

            assertFalse(film.getLikes().contains(1L));
            verify(filmStorage, never()).updateFilm(any(Film.class));
        }
    }

    @Nested
    @DisplayName("Тесты получения популярных фильмов")
    class PopularFilmsTests {

        @Test
        @DisplayName("Получение популярных фильмов - указано количество")
        void getPopularFilms_WithCount_ReturnsLimitedListTest() {
            Film film1 = createTestFilm();
            film1.setId(1L);
            film1.getLikes().add(1L);

            Film film2 = createTestFilm();
            film2.setId(2L);
            film2.getLikes().addAll(List.of(1L, 2L));

            Film film3 = createTestFilm();
            film3.setId(3L);

            when(filmStorage.getAllFilms()).thenReturn(List.of(film1, film2, film3));

            List<Film> result = filmService.getPopularFilms(2);

            assertEquals(2, result.size());
            assertEquals(2L, result.get(0).getId());
            assertEquals(1L, result.get(1).getId());
        }

        @Test
        @DisplayName("Получение популярных фильмов - count null использует значение по умолчанию")
        void getPopularFilms_CountNull_UsesDefaultTest() {
            List<Film> films = IntStream.range(0, 15)
                    .mapToObj(i -> {
                        Film film = createTestFilm();
                        film.setId((long) i);
                        film.getLikes().addAll(List.of(1L, 2L, 3L));
                        return film;
                    })
                    .collect(Collectors.toList());

            when(filmStorage.getAllFilms()).thenReturn(films);

            List<Film> result = filmService.getPopularFilms(null);

            assertEquals(10, result.size());
        }

        @Test
        @DisplayName("Получение популярных фильмов - отрицательный count использует значение по умолчанию")
        void getPopularFilms_NegativeCount_UsesDefaultTest() {
            List<Film> films = IntStream.range(0, 15)
                    .mapToObj(i -> createTestFilm())
                    .collect(Collectors.toList());

            when(filmStorage.getAllFilms()).thenReturn(films);

            List<Film> result = filmService.getPopularFilms(-5);

            assertEquals(10, result.size());
        }

        @Test
        @DisplayName("Получение популярных фильмов - пустой список")
        void getPopularFilms_EmptyList_ReturnsEmptyListTest() {
            when(filmStorage.getAllFilms()).thenReturn(List.of());

            List<Film> result = filmService.getPopularFilms(10);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Получение популярных фильмов - count больше размера списка")
        void getPopularFilms_CountLargerThanList_ReturnsAllFilmsTest() {
            Film film1 = createTestFilm();
            film1.setId(1L);
            film1.getLikes().add(1L);

            Film film2 = createTestFilm();
            film2.setId(2L);

            when(filmStorage.getAllFilms()).thenReturn(List.of(film1, film2));

            List<Film> result = filmService.getPopularFilms(10);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Получение популярных фильмов - сортировка по количеству лайков")
        void getPopularFilms_SortedByLikesCountTest() {
            Film film1 = createTestFilm();
            film1.setId(1L);
            film1.getLikes().add(1L);

            Film film2 = createTestFilm();
            film2.setId(2L);
            film2.getLikes().addAll(List.of(1L, 2L, 3L));

            Film film3 = createTestFilm();
            film3.setId(3L);
            film3.getLikes().addAll(List.of(1L, 2L));

            when(filmStorage.getAllFilms()).thenReturn(List.of(film1, film2, film3));

            List<Film> result = filmService.getPopularFilms(3);

            assertEquals(3, result.size());
            assertEquals(2L, result.get(0).getId());
            assertEquals(3L, result.get(1).getId());
            assertEquals(1L, result.get(2).getId());
        }
    }
}