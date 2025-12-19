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
    @DisplayName("Тесты создания фильмов")
    class CreateFilmTests {

        @Test
        @DisplayName("Создание фильма с валидными данными возвращает созданный фильм")
        void createFilm_ValidFilm_ReturnsCreatedFilmTest() {
            FilmDTO inputDTO = createTestFilmDTOWithoutId();
            Film inputFilm = createTestFilmWithoutId();
            Film createdFilm = createTestFilm();
            FilmDTO expectedDTO = createTestFilmDTO();

            when(filmMapper.toEntity(inputDTO)).thenReturn(inputFilm);
            when(filmMapper.toDTO(createdFilm)).thenReturn(expectedDTO);
            when(mpaService.getMpaById(1L)).thenReturn(inputDTO.getMpa());
            when(filmDbStorage.createFilm(inputFilm)).thenReturn(createdFilm);

            FilmDTO result = filmService.createFilm(inputDTO);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Test Film", result.getName());
            verify(filmDbStorage, times(1)).createFilm(inputFilm);
            verify(mpaService, times(1)).getMpaById(1L);
            verify(filmMapper, times(1)).toEntity(inputDTO);
            verify(filmMapper, times(1)).toDTO(createdFilm);
        }

        @Test
        @DisplayName("Создание фильма без MPA выбрасывает IllegalArgumentException")
        void createFilm_WithoutMpa_ThrowsIllegalArgumentExceptionTest() {
            FilmDTO filmDTO = createTestFilmDTOWithNullMpa();

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> filmService.createFilm(filmDTO)
            );

            assertTrue(exception.getMessage().contains("MPA"));
            verify(filmDbStorage, never()).createFilm(any(Film.class));
            verify(mpaService, never()).getMpaById(anyLong());
            verify(filmMapper, never()).toEntity(any(FilmDTO.class));
        }

        @Test
        @DisplayName("Создание фильма без ID MPA выбрасывает IllegalArgumentException")
        void createFilm_WithoutMpaId_ThrowsIllegalArgumentExceptionTest() {
            FilmDTO filmDTO = createTestFilmDTOWithNullMpaId();

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> filmService.createFilm(filmDTO)
            );

            assertTrue(exception.getMessage().contains("MPA"));
            verify(filmDbStorage, never()).createFilm(any(Film.class));
            verify(mpaService, never()).getMpaById(anyLong());
            verify(filmMapper, never()).toEntity(any(FilmDTO.class));
        }
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
    @DisplayName("Тесты обновления фильмов")
    class UpdateFilmTests {

        @Test
        @DisplayName("Обновление валидного фильма возвращает обновленный фильм")
        void updateFilm_ValidFilm_ReturnsUpdatedFilmTest() {
            Film existingFilm = createTestFilm();
            FilmDTO existingFilmDTO = createTestFilmDTO();
            FilmDTO updatedFilmDTO = createTestFilmDTO();
            updatedFilmDTO.setName("Updated Film");
            Film updatedFilm = createTestFilm();
            updatedFilm.setName("Updated Film");
            FilmDTO expectedDTO = createTestFilmDTO();
            expectedDTO.setName("Updated Film");

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(existingFilm));
            when(filmMapper.toDTO(existingFilm)).thenReturn(existingFilmDTO);
            when(filmMapper.toEntity(updatedFilmDTO)).thenReturn(updatedFilm);
            when(filmMapper.toEntity(existingFilmDTO)).thenReturn(existingFilm);
            when(filmMapper.toDTO(updatedFilm)).thenReturn(expectedDTO);
            when(mpaService.getMpaById(anyLong())).thenReturn(updatedFilmDTO.getMpa());
            doNothing().when(filmValidator).validateFilmUniquenessForUpdate(existingFilm, updatedFilm);
            when(filmDbStorage.updateFilm(updatedFilm)).thenReturn(updatedFilm);

            FilmDTO result = filmService.updateFilm(updatedFilmDTO);

            assertNotNull(result);
            assertEquals("Updated Film", result.getName());
            verify(filmDbStorage, times(1)).updateFilm(updatedFilm);
            verify(filmValidator, times(1))
                    .validateFilmUniquenessForUpdate(existingFilm, updatedFilm);
            verify(mpaService, times(1)).getMpaById(anyLong());
        }

        @Test
        @DisplayName("Обновление фильма без MPA выбрасывает IllegalArgumentException")
        void updateFilm_WithoutMpa_ThrowsIllegalArgumentExceptionTest() {
            Film existingFilm = createTestFilm();
            FilmDTO existingFilmDTO = createTestFilmDTO();
            FilmDTO updatedFilmDTO = createTestFilmDTO();
            updatedFilmDTO.setMpa(null);

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(existingFilm));
            when(filmMapper.toDTO(existingFilm)).thenReturn(existingFilmDTO);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> filmService.updateFilm(updatedFilmDTO)
            );

            assertTrue(exception.getMessage().contains("MPA"));
            verify(filmDbStorage, never()).updateFilm(any(Film.class));
            verify(mpaService, never()).getMpaById(anyLong());
            verify(filmMapper, never()).toEntity(updatedFilmDTO);
        }

        @Test
        @DisplayName("Обновление фильма без ID MPA выбрасывает IllegalArgumentException")
        void updateFilm_WithoutMpaId_ThrowsIllegalArgumentExceptionTest() {
            Film existingFilm = createTestFilm();
            FilmDTO existingFilmDTO = createTestFilmDTO();
            FilmDTO updatedFilmDTO = createTestFilmDTO();
            updatedFilmDTO.getMpa().setId(null);

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(existingFilm));
            when(filmMapper.toDTO(existingFilm)).thenReturn(existingFilmDTO);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> filmService.updateFilm(updatedFilmDTO)
            );

            assertTrue(exception.getMessage().contains("MPA"));
            verify(filmDbStorage, never()).updateFilm(any(Film.class));
            verify(mpaService, never()).getMpaById(anyLong());
            verify(filmMapper, never()).toEntity(updatedFilmDTO);
        }

        @Test
        @DisplayName("Обновление несуществующего фильма выбрасывает NotFoundException")
        void updateFilm_NonExistingFilm_ThrowsNotFoundExceptionTest() {
            FilmDTO filmDTO = createTestFilmDTO();
            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.empty());

            NotFoundException exception = assertThrows(
                    NotFoundException.class,
                    () -> filmService.updateFilm(filmDTO)
            );

            assertTrue(exception.getMessage().contains("не найден"));
            verify(filmDbStorage, never()).updateFilm(any(Film.class));
            verify(filmMapper, never()).toEntity(any(FilmDTO.class));
        }

        @Test
        @DisplayName("Обновление фильма на дублирующие данные выбрасывает DuplicateException")
        void updateFilm_DuplicateFilm_ThrowsDuplicateExceptionTest() {
            Film existingFilm = createTestFilm();
            FilmDTO existingFilmDTO = createTestFilmDTO();
            FilmDTO updatedFilmDTO = createTestFilmDTO();
            updatedFilmDTO.setName("Different Film");
            Film updatedFilm = createTestFilm();
            updatedFilm.setName("Different Film");

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(existingFilm));
            when(filmMapper.toDTO(existingFilm)).thenReturn(existingFilmDTO);
            when(filmMapper.toEntity(updatedFilmDTO)).thenReturn(updatedFilm);
            when(filmMapper.toEntity(existingFilmDTO)).thenReturn(existingFilm);
            when(mpaService.getMpaById(anyLong())).thenReturn(updatedFilmDTO.getMpa());
            doThrow(new DuplicateException("Фильм с таким названием и годом выпуска уже существует"))
                    .when(filmValidator).validateFilmUniquenessForUpdate(existingFilm, updatedFilm);

            DuplicateException exception = assertThrows(
                    DuplicateException.class,
                    () -> filmService.updateFilm(updatedFilmDTO)
            );

            assertEquals("Фильм с таким названием и годом выпуска уже существует",
                    exception.getMessage());
            verify(filmDbStorage, never()).updateFilm(any(Film.class));
            verify(mpaService, times(1)).getMpaById(anyLong());
        }
    }

    @Nested
    @DisplayName("Тесты управления лайками")
    class LikeManagementTests {

        @Test
        @DisplayName("Добавление лайка - фильм и пользователь существуют")
        void addLike_BothExist_AddsLikeTest() {
            Film film = createTestFilm();
            FilmDTO filmDTO = createTestFilmDTO();
            UserDTO userDTO = createTestUserDTO();

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(film));
            when(filmMapper.toDTO(film)).thenReturn(filmDTO);
            when(userService.getUserById(1L)).thenReturn(userDTO);
            doNothing().when(filmDbStorage).addLike(1L, 1L);
            doNothing().when(feedService).recordEvent(anyLong(), anyLong(), any(),
                    any(), anyLong());

            filmService.addLike(1L, 1L);

            verify(filmDbStorage, times(1)).addLike(1L, 1L);
            verify(filmDbStorage, times(1)).getFilmById(1L);
            verify(userService, times(1)).getUserById(1L);
            verify(filmMapper, times(1)).toDTO(film);
            verify(feedService, times(1)).recordEvent(anyLong(),
                    anyLong(), any(), any(), anyLong());
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
        @DisplayName("Удаление лайка - лайк существует")
        void removeLike_LikeExists_RemovesLikeTest() {
            Film film = createTestFilm();
            FilmDTO filmDTO = createTestFilmDTO();
            UserDTO userDTO = createTestUserDTO();

            when(filmDbStorage.getFilmById(1L)).thenReturn(Optional.of(film));
            when(filmMapper.toDTO(film)).thenReturn(filmDTO);
            when(userService.getUserById(1L)).thenReturn(userDTO);
            doNothing().when(filmDbStorage).removeLike(1L, 1L);
            doNothing().when(feedService).recordEvent(anyLong(), anyLong(), any(),
                    any(), anyLong());

            filmService.removeLike(1L, 1L);

            verify(filmDbStorage, times(1)).removeLike(1L, 1L);
            verify(filmDbStorage, times(1)).getFilmById(1L);
            verify(userService, times(1)).getUserById(1L);
            verify(filmMapper, times(1)).toDTO(film);
            verify(feedService, times(1)).recordEvent(anyLong(),
                    anyLong(), any(), any(), anyLong());
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