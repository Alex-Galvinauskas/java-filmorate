package ru.yandex.practicum.filmorate.service.recommendation;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.*;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
    FilmDbStorage.class, 
    UserDbStorage.class, 
    GenreDbStorage.class,
    MpaDbStorage.class,
    DirectorDbStorage.class
})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты сервиса рекомендаций")
class RecommendationServiceImplTest {

    @Mock
    private FilmDbStorage filmDbStorage;

    @Mock
    private UserDbStorage userDbStorage;

    @Mock
    private FilmMapper filmMapper;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private User createTestUser(Long id) {
        return User.builder()
                .id(id)
                .email("user" + id + "@test.com")
                .login("user" + id)
                .name("User " + id)
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }

    private Film createTestFilm(Long id) {
        return Film.builder()
                .id(id)
                .name("Film " + id)
                .description("Description " + id)
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1L).name("G").build())
                .build();
    }

    private FilmDTO createTestFilmDTO(Long id) {
        return FilmDTO.builder()
                .id(id)
                .name("Film " + id)
                .description("Description " + id)
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .build();
    }

    @Test
    @DisplayName("Получение рекомендаций - пользователь не найден")
    void getRecommendations_UserNotFound_ThrowsException() {
        Long userId = 1L;
        when(userDbStorage.getUserById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> recommendationService.getRecommendations(userId)
        );

        assertTrue(exception.getMessage().contains("не найден"));
        verify(userDbStorage, times(1)).getUserById(userId);
        verify(filmDbStorage, never()).getRecommendationsForUser(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Получение рекомендаций - пользователь без рекомендаций")
    void getRecommendations_UserWithoutRecommendations_ReturnsEmptyList() {
        Long userId = 1L;
        User user = createTestUser(userId);

        when(userDbStorage.getUserById(userId)).thenReturn(Optional.of(user));
        when(filmDbStorage.getRecommendationsForUser(userId, 10)).thenReturn(Collections.emptyList());

        List<FilmDTO> result = recommendationService.getRecommendations(userId);

        assertTrue(result.isEmpty(), "Должен возвращаться пустой список при отсутствии рекомендаций");
        assertEquals(0, result.size());
        verify(filmDbStorage, times(1)).getRecommendationsForUser(userId, 10);
    }

    @Test
    @DisplayName("Получение рекомендаций - найдены рекомендации")
    void getRecommendations_FoundRecommendations_ReturnsFilms() {
        Long userId = 1L;
        User user = createTestUser(userId);

        Film film3 = createTestFilm(3L);
        FilmDTO filmDTO3 = createTestFilmDTO(3L);

        when(userDbStorage.getUserById(userId)).thenReturn(Optional.of(user));
        when(filmDbStorage.getRecommendationsForUser(userId, 10)).thenReturn(Collections.singletonList(film3));
        when(filmMapper.toDTO(film3)).thenReturn(filmDTO3);

        List<FilmDTO> result = recommendationService.getRecommendations(userId);

        assertEquals(1, result.size(), "Должен вернуться один рекомендованный фильм");
        assertEquals(3L, result.get(0).getId(), "Должен вернуться фильм с ID 3");
        verify(filmDbStorage, times(1)).getRecommendationsForUser(userId, 10);
    }

    @Test
    @DisplayName("Получение рекомендаций - нет рекомендаций")
    void getRecommendations_NoRecommendations_ReturnsEmptyList() {
        Long userId = 1L;
        User user = createTestUser(userId);

        when(userDbStorage.getUserById(userId)).thenReturn(Optional.of(user));
        when(filmDbStorage.getRecommendationsForUser(userId, 10)).thenReturn(Collections.emptyList());

        List<FilmDTO> result = recommendationService.getRecommendations(userId);

        assertTrue(result.isEmpty(), "Должен возвращаться пустой список при отсутствии рекомендаций");
        assertEquals(0, result.size());
        verify(filmDbStorage, times(1)).getRecommendationsForUser(userId, 10);
    }

    @Test
    @DisplayName("Получение рекомендаций - несколько рекомендаций")
    void getRecommendations_MultipleRecommendations_ReturnsFilms() {
        Long userId = 1L;
        User user = createTestUser(userId);

        Film film1 = createTestFilm(1L);
        Film film2 = createTestFilm(2L);
        FilmDTO filmDTO1 = createTestFilmDTO(1L);
        FilmDTO filmDTO2 = createTestFilmDTO(2L);

        List<Film> films = Arrays.asList(film1, film2);

        when(userDbStorage.getUserById(userId)).thenReturn(Optional.of(user));
        when(filmDbStorage.getRecommendationsForUser(userId, 10)).thenReturn(films);
        when(filmMapper.toDTO(film1)).thenReturn(filmDTO1);
        when(filmMapper.toDTO(film2)).thenReturn(filmDTO2);

        List<FilmDTO> result = recommendationService.getRecommendations(userId);

        assertEquals(2, result.size(), "Должен вернуться два рекомендованных фильма");
        verify(filmDbStorage, times(1)).getRecommendationsForUser(userId, 10);
    }

    @Test
    @DisplayName("Получение рекомендаций - ограничение количества рекомендаций")
    void getRecommendations_WithLimit_ReturnsLimitedFilms() {
        Long userId = 1L;
        User user = createTestUser(userId);

        // Создаем только 10 фильмов, которые действительно вернет метод
        List<Film> films = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            films.add(createTestFilm((long) i));
        }

        when(userDbStorage.getUserById(userId)).thenReturn(Optional.of(user));
        when(filmDbStorage.getRecommendationsForUser(userId, 10)).thenReturn(films);

        // Используем общую заглушку для всех фильмов
        when(filmMapper.toDTO(any(Film.class))).thenAnswer(invocation -> {
            Film film = invocation.getArgument(0);
            return createTestFilmDTO(film.getId());
        });

        List<FilmDTO> result = recommendationService.getRecommendations(userId);

        assertEquals(10, result.size(), "Должен вернуться ограниченное количество фильмов");
        verify(filmDbStorage, times(1)).getRecommendationsForUser(userId, 10);
    }
}
