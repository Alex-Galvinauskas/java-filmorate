package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.dto.MpaDTO;
import ru.yandex.practicum.filmorate.dto.UserDTO;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.service.feed.FeedService;
import ru.yandex.practicum.filmorate.service.film.FilmServiceImpl;
import ru.yandex.practicum.filmorate.service.film.GenreService;
import ru.yandex.practicum.filmorate.service.film.MpaService;
import ru.yandex.practicum.filmorate.service.film.filmValidation.FilmValidatorService;
import ru.yandex.practicum.filmorate.service.user.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты сервиса управления фильмами")
class FilmServiceImplTest {

    @Mock
    private FilmDbStorage filmDbStorage;

    @Mock
    private FilmValidatorService filmValidator;

    @Mock
    private UserService userService;

    @Mock
    private MpaService mpaService;

    @Mock
    private GenreService genreService;

    @Mock
    private FilmMapper filmMapper;

    @Mock
    private FeedService feedService;

    @InjectMocks
    private FilmServiceImpl filmService;

    private FilmDTO createTestFilmDTO() {
        MpaDTO mpaDTO = MpaDTO.builder()
                .id(1L)
                .name("G")
                .description("General Audiences")
                .build();

        return FilmDTO.builder()
                .id(1L)
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpa(mpaDTO)
                .build();
    }

    private Film createTestFilm() {
        Mpa mpa = Mpa.builder()
                .id(1L)
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

    private FilmDTO createTestFilmDTOWithoutId() {
        FilmDTO filmDTO = createTestFilmDTO();
        filmDTO.setId(null);
        return filmDTO;
    }

    private Film createTestFilmWithoutId() {
        Film film = createTestFilm();
        film.setId(null);
        return film;
    }

    private FilmDTO createTestFilmDTOWithNullMpaId() {
        MpaDTO mpaDTO = MpaDTO.builder()
                .id(null)
                .name("G")
                .description("General Audiences")
                .build();

        return FilmDTO.builder()
                .id(null)
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpa(mpaDTO)
                .build();
    }

    private FilmDTO createTestFilmDTOWithNullMpa() {
        return FilmDTO.builder()
                .id(null)
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpa(null)
                .build();
    }

    private UserDTO createTestUserDTO() {
        return UserDTO.builder()
                .id(1L)
                .email("test@example.com")
                .login("testlogin")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }


    @Nested
    @DisplayName("Тесты получения фильмов")
    class GetFilmTests {

        @Test
        @DisplayName("Получение всех фильмов возвращает список фильмов")
        void getAllFilms_ReturnsFilmsListTest() {
            Film film = createTestFilm();
            FilmDTO filmDTO = createTestFilmDTO();

            when(filmDbStorage.getAllFilms()).thenReturn(List.of(film));
            when(filmMapper.toDTO(film)).thenReturn(filmDTO);

            List<FilmDTO> result = filmService.getAllFilms();

            assertEquals(1, result.size());
            assertEquals("Test Film", result.getFirst().getName());
            verify(filmDbStorage, times(1)).getAllFilms();
            verify(filmMapper, times(1)).toDTO(film);
        }

        @Test
        @DisplayName("Получение фильма по существующему ID возвращает фильм")
        void getFilmById_ExistingId_ReturnsFilmTest() {
            Film film = createTestFilm();
            FilmDTO filmDTO = createTestFilmDTO();

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(film));
            when(filmMapper.toDTO(film)).thenReturn(filmDTO);

            FilmDTO result = filmService.getFilmById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(filmDbStorage, times(1)).getFilmById(1L);
            verify(filmMapper, times(1)).toDTO(film);
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
            verify(filmMapper, never()).toDTO(any(Film.class));
        }
    }

    @Nested
    @DisplayName("Тесты управления лайками")
    class LikeManagementTests {


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
            verify(filmMapper, never()).toDTO(any(Film.class));
        }

        @Test
        @DisplayName("Добавление лайка - пользователь не существует")
        void addLike_UserNotExist_ThrowsNotFoundExceptionTest() {
            Film film = createTestFilm();
            FilmDTO filmDTO = createTestFilmDTO();

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(film));
            when(filmMapper.toDTO(film)).thenReturn(filmDTO);
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
        @DisplayName("Удаление лайка - пользователь не существует")
        void removeLike_UserNotExist_ThrowsNotFoundExceptionTest() {
            Film film = createTestFilm();
            FilmDTO filmDTO = createTestFilmDTO();

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(film));
            when(filmMapper.toDTO(film)).thenReturn(filmDTO);
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
}