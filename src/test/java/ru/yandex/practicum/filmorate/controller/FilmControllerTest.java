package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.film.FilmService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты API контроллера фильмов")
class FilmControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FilmService filmService;

    @InjectMocks
    private FilmController filmController;

    private ObjectMapper objectMapper;
    private Film testFilm;
    private Film testFilm2;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders.standaloneSetup(filmController).build();

        testFilm = Film.builder()
                .id(1L)
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .build();

        testFilm2 = Film.builder()
                .id(2L)
                .name("Another Film")
                .description("Another Description")
                .releaseDate(LocalDate.of(2021, 1, 1))
                .duration(150)
                .build();
    }

    @Nested
    @DisplayName("Тесты операций с фильмами")
    class FilmOperationsTests {

        @Test
        @DisplayName("Создание фильма возвращает созданный фильм")
        void createFilm_ShouldReturnCreatedFilmTest() throws Exception {
            when(filmService.createFilm(any(Film.class))).thenReturn(testFilm);

            mockMvc.perform(post("/films")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testFilm)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Test Film")))
                    .andExpect(jsonPath("$.description", is("Test Description")));

            verify(filmService, times(1)).createFilm(any(Film.class));
        }

        @Test
        @DisplayName("Получение всех фильмов возвращает список фильмов")
        void getAllFilms_ShouldReturnListOfFilmsTest() throws Exception {
            List<Film> films = Arrays.asList(testFilm, testFilm2);
            when(filmService.getAllFilms()).thenReturn(films);

            mockMvc.perform(get("/films"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name", is("Test Film")))
                    .andExpect(jsonPath("$[1].name", is("Another Film")));

            verify(filmService, times(1)).getAllFilms();
        }

        @Test
        @DisplayName("Получение фильма по ID возвращает фильм")
        void getFilmById_ShouldReturnFilmTest() throws Exception {
            when(filmService.getFilmById(1L)).thenReturn(testFilm);

            mockMvc.perform(get("/films/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Test Film")));

            verify(filmService, times(1)).getFilmById(1L);
        }

        @Test
        @DisplayName("Обновление фильма возвращает обновленный фильм")
        void updateFilm_ShouldReturnUpdatedFilmTest() throws Exception {
            Film updatedFilm = Film.builder()
                    .id(1L)
                    .name("Updated Film")
                    .description("Updated Description")
                    .releaseDate(LocalDate.of(2020, 1, 1))
                    .duration(130)
                    .build();

            when(filmService.updateFilm(any(Film.class))).thenReturn(updatedFilm);

            mockMvc.perform(put("/films")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatedFilm)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Updated Film")))
                    .andExpect(jsonPath("$.duration", is(130)));

            verify(filmService, times(1)).updateFilm(any(Film.class));
        }
    }

    @Nested
    @DisplayName("Тесты операций с лайками")
    class LikeOperationsTests {

        @Test
        @DisplayName("Добавление лайка вызывает сервис")
        void addLike_ShouldCallServiceTest() throws Exception {
            doNothing().when(filmService).addLike(1L, 1L);

            mockMvc.perform(put("/films/1/like/1"))
                    .andExpect(status().isOk());

            verify(filmService, times(1)).addLike(1L, 1L);
        }

        @Test
        @DisplayName("Удаление лайка вызывает сервис")
        void deleteLike_ShouldCallServiceTest() throws Exception {
            doNothing().when(filmService).removeLike(1L, 1L);

            mockMvc.perform(delete("/films/1/like/1"))
                    .andExpect(status().isOk());

            verify(filmService, times(1)).removeLike(1L, 1L);
        }
    }

    @Nested
    @DisplayName("Тесты получения популярных фильмов")
    class PopularFilmsTests {

        @Test
        @DisplayName("Получение популярных фильмов с указанным количеством возвращает фильмы")
        void getPopularFilms_ShouldReturnPopularFilmsTest() throws Exception {
            List<Film> popularFilms = Arrays.asList(testFilm2, testFilm);
            when(filmService.getPopularFilms(2)).thenReturn(popularFilms);

            mockMvc.perform(get("/films/popular")
                            .param("count", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name", is("Another Film")))
                    .andExpect(jsonPath("$[1].name", is("Test Film")));

            verify(filmService, times(1)).getPopularFilms(2);
        }

        @Test
        @DisplayName("Получение популярных фильмов без указания количества использует значение по умолчанию")
        void getPopularFilms_WithDefaultCount_ShouldUseDefaultValueTest() throws Exception {
            List<Film> popularFilms = Collections.singletonList(testFilm);
            when(filmService.getPopularFilms(10)).thenReturn(popularFilms);

            mockMvc.perform(get("/films/popular"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(filmService, times(1)).getPopularFilms(10);
        }

        @Test
        @DisplayName("Получение популярных фильмов с нулевым количеством возвращает пустой список")
        void getPopularFilms_WithZeroCount_ShouldReturnEmptyListTest() throws Exception {
            List<Film> popularFilms = Collections.emptyList();
            when(filmService.getPopularFilms(0)).thenReturn(popularFilms);

            mockMvc.perform(get("/films/popular")
                            .param("count", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(filmService, times(1)).getPopularFilms(0);
        }
    }

    @Nested
    @DisplayName("Тесты валидации endpoints")
    class EndpointValidationTests {

        @Test
        @DisplayName("Некорректный ID в пути возвращает ошибку")
        void getFilmById_InvalidId_ReturnsBadRequestTest() throws Exception {
            mockMvc.perform(get("/films/invalid"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Некорректный ID для лайка возвращает ошибку")
        void addLike_InvalidIds_ReturnsBadRequestTest() throws Exception {
            mockMvc.perform(put("/films/invalid/like/invalid"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Некорректный параметр count возвращает ошибку")
        void getPopularFilms_InvalidCount_ReturnsBadRequestTest() throws Exception {
            mockMvc.perform(get("/films/popular")
                            .param("count", "invalid"))
                    .andExpect(status().isBadRequest());
        }
    }
}