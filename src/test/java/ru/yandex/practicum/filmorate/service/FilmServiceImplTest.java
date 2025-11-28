package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.film.FilmServiceImpl;
import ru.yandex.practicum.filmorate.service.film.MpaService;
import ru.yandex.practicum.filmorate.service.film.validation.FilmValidatorImpl;
import ru.yandex.practicum.filmorate.service.user.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты сервиса управления фильмами")
class FilmServiceImplTest {

    @Mock
    private FilmDbStorage filmDbStorage;

    @Mock
    private FilmValidatorImpl filmValidator;

    @Mock
    private UserService userService;

    @Mock
    private MpaService mpaService;

    @InjectMocks
    private FilmServiceImpl filmService;

    private Film createTestFilm() {
        Mpa mpa = Mpa.builder()
                .id(1)
                .name("G")
                .description("General Audiences")
                .build();

        return Film.builder()
                .id(1L)
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpa(mpa)
                .build();
    }

    private Film createTestFilmWithoutId() {
        Film film = createTestFilm();
        film.setId(null);
        return film;
    }

    private Film createTestFilmWithNullMpaId() {
        Mpa mpa = Mpa.builder()
                .id(null)
                .name("G")
                .description("General Audiences")
                .build();

        return Film.builder()
                .id(null)
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpa(mpa)
                .build();
    }

    private Film createTestFilmWithNullMpa() {
        return Film.builder()
                .id(null)
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpa(null)
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
            Film film = createTestFilmWithoutId();
            Film filmWithId = createTestFilm();

            assertNotNull(film.getMpa().getId(), "MPA ID должен быть не null");

            when(mpaService.getMpaById(1)).thenReturn(film.getMpa());
            when(filmDbStorage.createFilm(film)).thenReturn(filmWithId);

            Film result = filmService.createFilm(film);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Test Film", result.getName());
            verify(filmDbStorage, times(1)).createFilm(film);
            verify(mpaService, times(1)).getMpaById(1);
        }

        @Test
        @DisplayName("Создание фильма без MPA выбрасывает IllegalArgumentException")
        void createFilm_WithoutMpa_ThrowsIllegalArgumentExceptionTest() {
            Film film = createTestFilmWithNullMpa();

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> filmService.createFilm(film)
            );

            assertTrue(exception.getMessage().contains("MPA"));
            verify(filmDbStorage, never()).createFilm(any(Film.class));
            verify(mpaService, never()).getMpaById(anyInt());
            verify(filmValidator, never()).validateFilmUniqueness(anyString(), anyInt());
        }

        @Test
        @DisplayName("Создание фильма без ID MPA выбрасывает IllegalArgumentException")
        void createFilm_WithoutMpaId_ThrowsIllegalArgumentExceptionTest() {
            Film film = createTestFilmWithNullMpaId();

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> filmService.createFilm(film)
            );

            assertTrue(exception.getMessage().contains("MPA"));
            verify(filmDbStorage, never()).createFilm(any(Film.class));
            verify(mpaService, never()).getMpaById(anyInt());
            verify(filmValidator, never()).validateFilmUniqueness(anyString(), anyInt());
        }
    }

    @Nested
    @DisplayName("Тесты получения фильмов")
    class GetFilmTests {

        @Test
        @DisplayName("Получение всех фильмов возвращает список фильмов")
        void getAllFilms_ReturnsFilmsListTest() {
            Film film = createTestFilm();
            when(filmDbStorage.getAllFilms()).thenReturn(List.of(film));

            List<Film> result = filmService.getAllFilms();

            assertEquals(1, result.size());
            assertEquals("Test Film", result.getFirst().getName());
            verify(filmDbStorage, times(1)).getAllFilms();
        }

        @Test
        @DisplayName("Получение фильма по существующему ID возвращает фильм")
        void getFilmById_ExistingId_ReturnsFilmTest() {
            Film film = createTestFilm();
            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(film));

            Film result = filmService.getFilmById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(filmDbStorage, times(1)).getFilmById(1L);
        }

        @Test
        @DisplayName("Получение фильма по несуществующему ID выбрасывает NotFoundException")
        void getFilmById_NonExistingId_ThrowsNotFoundExceptionTest() {
            when(filmDbStorage.getFilmById(999L)).thenReturn(Optional.empty());

            NotFoundException exception = assertThrows(
                    NotFoundException.class,
                    () -> filmService.getFilmById(999L)
            );

            assertTrue(exception.getMessage().contains("не найден"));
            verify(filmDbStorage, times(1)).getFilmById(999L);
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

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(existingFilm));
            when(mpaService.getMpaById(anyInt())).thenReturn(updatedFilm.getMpa());
            doNothing().when(filmValidator).validateFilmUniquenessForUpdate(existingFilm, updatedFilm);
            when(filmDbStorage.updateFilm(any(Film.class))).thenReturn(updatedFilm);

            Film result = filmService.updateFilm(updatedFilm);

            assertNotNull(result);
            assertEquals("Updated Film", result.getName());
            verify(filmDbStorage, times(1)).
                    updateFilm(any(Film.class));
            verify(filmValidator, times(1)).
                    validateFilmUniquenessForUpdate(existingFilm, updatedFilm);
            verify(mpaService, times(1)).getMpaById(anyInt());
        }

        @Test
        @DisplayName("Обновление фильма без MPA выбрасывает IllegalArgumentException")
        void updateFilm_WithoutMpa_ThrowsIllegalArgumentExceptionTest() {
            Film existingFilm = createTestFilm();
            Film updatedFilm = createTestFilm();
            updatedFilm.setMpa(null);

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(existingFilm));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> filmService.updateFilm(updatedFilm)
            );

            assertTrue(exception.getMessage().contains("MPA"));
            verify(filmDbStorage, never()).updateFilm(any(Film.class));
            verify(mpaService, never()).getMpaById(anyInt());
        }

        @Test
        @DisplayName("Обновление фильма без ID MPA выбрасывает IllegalArgumentException")
        void updateFilm_WithoutMpaId_ThrowsIllegalArgumentExceptionTest() {
            Film existingFilm = createTestFilm();
            Film updatedFilm = createTestFilm();
            updatedFilm.getMpa().setId(null);

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(existingFilm));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> filmService.updateFilm(updatedFilm)
            );

            assertTrue(exception.getMessage().contains("MPA"));
            verify(filmDbStorage, never()).updateFilm(any(Film.class));
            verify(mpaService, never()).getMpaById(anyInt());
        }

        @Test
        @DisplayName("Обновление несуществующего фильма выбрасывает NotFoundException")
        void updateFilm_NonExistingFilm_ThrowsNotFoundExceptionTest() {
            Film film = createTestFilm();
            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.empty());

            NotFoundException exception = assertThrows(
                    NotFoundException.class,
                    () -> filmService.updateFilm(film)
            );

            assertTrue(exception.getMessage().contains("не найден"));
            verify(filmDbStorage, never()).updateFilm(any(Film.class));
        }

        @Test
        @DisplayName("Обновление фильма на дублирующие данные выбрасывает DuplicateException")
        void updateFilm_DuplicateFilm_ThrowsDuplicateExceptionTest() {
            Film existingFilm = createTestFilm();
            Film updatedFilm = createTestFilm();
            updatedFilm.setName("Different Film");

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(existingFilm));
            when(mpaService.getMpaById(anyInt())).thenReturn(updatedFilm.getMpa());
            doThrow(new DuplicateException("Фильм с таким названием и годом выпуска уже существует"))
                    .when(filmValidator).validateFilmUniquenessForUpdate(existingFilm, updatedFilm);

            DuplicateException exception = assertThrows(
                    DuplicateException.class,
                    () -> filmService.updateFilm(updatedFilm)
            );

            assertEquals("Фильм с таким названием и годом выпуска уже существует",
                    exception.getMessage());
            verify(filmDbStorage, never()).updateFilm(any(Film.class));
            verify(mpaService, times(1)).getMpaById(anyInt());
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

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(film));
            when(userService.getUserById(1L)).thenReturn(user);
            doNothing().when(filmDbStorage).addLike(1L, 1L);

            filmService.addLike(1L, 1L);

            verify(filmDbStorage, times(1)).addLike(1L, 1L);
            verify(filmDbStorage, times(1)).getFilmById(1L);
            verify(userService, times(1)).getUserById(1L);
        }

        @Test
        @DisplayName("Добавление лайка - фильм не существует")
        void addLike_FilmNotExist_ThrowsNotFoundExceptionTest() {
            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.empty());

            NotFoundException exception = assertThrows(
                    NotFoundException.class,
                    () -> filmService.addLike(1L, 1L)
            );

            assertTrue(exception.getMessage().contains("не найден"));
            verify(userService, never()).getUserById(anyLong());
            verify(filmDbStorage, never()).addLike(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Добавление лайка - пользователь не существует")
        void addLike_UserNotExist_ThrowsNotFoundExceptionTest() {
            Film film = createTestFilm();
            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(film));
            when(userService.getUserById(1L))
                    .thenThrow(new NotFoundException("Пользователь не найден"));

            NotFoundException exception = assertThrows(
                    NotFoundException.class,
                    () -> filmService.addLike(1L, 1L)
            );

            assertEquals("Пользователь не найден", exception.getMessage());
            verify(filmDbStorage, never()).addLike(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Удаление лайка - лайк существует")
        void removeLike_LikeExists_RemovesLikeTest() {
            Film film = createTestFilm();
            User user = createTestUser();

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(film));
            when(userService.getUserById(1L)).thenReturn(user);
            doNothing().when(filmDbStorage).removeLike(1L, 1L);

            filmService.removeLike(1L, 1L);

            verify(filmDbStorage, times(1)).removeLike(1L, 1L);
            verify(filmDbStorage, times(1)).getFilmById(1L);
            verify(userService, times(1)).getUserById(1L);
        }

        @Test
        @DisplayName("Удаление лайка - пользователь не существует")
        void removeLike_UserNotExist_ThrowsNotFoundExceptionTest() {
            Film film = createTestFilm();
            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(film));
            when(userService.getUserById(1L))
                    .thenThrow(new NotFoundException("Пользователь не найден"));

            NotFoundException exception = assertThrows(
                    NotFoundException.class,
                    () -> filmService.removeLike(1L, 1L)
            );

            assertEquals("Пользователь не найден", exception.getMessage());
            verify(filmDbStorage, never()).removeLike(anyLong(), anyLong());
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

            Film film2 = createTestFilm();
            film2.setId(2L);

            Film film3 = createTestFilm();
            film3.setId(3L);

            when(filmDbStorage.getPopularFilms(2)).thenReturn(List.of(film2, film1));

            List<Film> result = filmService.getPopularFilms(2);

            assertEquals(2, result.size());
            assertEquals(2L, result.get(0).getId());
            assertEquals(1L, result.get(1).getId());
            verify(filmDbStorage, times(1)).getPopularFilms(2);
        }

        @Test
        @DisplayName("Получение популярных фильмов - count null использует значение по умолчанию")
        void getPopularFilms_CountNull_UsesDefaultTest() {
            List<Film> films = IntStream.range(0, 15)
                    .mapToObj(i -> {
                        Film film = createTestFilm();
                        film.setId((long) i);
                        return film;
                    })
                    .collect(Collectors.toList());

            when(filmDbStorage.getPopularFilms(10)).thenReturn(films.subList(0, 10));

            List<Film> result = filmService.getPopularFilms(null);

            assertEquals(10, result.size());
            verify(filmDbStorage, times(1)).getPopularFilms(10);
        }

        @Test
        @DisplayName("Получение популярных фильмов - отрицательный count использует значение по умолчанию")
        void getPopularFilms_NegativeCount_UsesDefaultTest() {
            List<Film> films = IntStream.range(0, 15)
                    .mapToObj(i -> createTestFilm())
                    .collect(Collectors.toList());

            when(filmDbStorage.getPopularFilms(10)).thenReturn(films.subList(0, 10));

            List<Film> result = filmService.getPopularFilms(-5);

            assertEquals(10, result.size());
            verify(filmDbStorage, times(1)).getPopularFilms(10);
        }

        @Test
        @DisplayName("Получение популярных фильмов - пустой список")
        void getPopularFilms_EmptyList_ReturnsEmptyListTest() {
            when(filmDbStorage.getPopularFilms(10)).thenReturn(List.of());

            List<Film> result = filmService.getPopularFilms(10);

            assertTrue(result.isEmpty());
            verify(filmDbStorage, times(1)).getPopularFilms(10);
        }

        @Test
        @DisplayName("Получение популярных фильмов - count больше размера списка")
        void getPopularFilms_CountLargerThanList_ReturnsAllFilmsTest() {
            Film film1 = createTestFilm();
            film1.setId(1L);

            Film film2 = createTestFilm();
            film2.setId(2L);

            when(filmDbStorage.getPopularFilms(10)).thenReturn(List.of(film1, film2));

            List<Film> result = filmService.getPopularFilms(10);

            assertEquals(2, result.size());
            verify(filmDbStorage, times(1)).getPopularFilms(10);
        }
    }
}