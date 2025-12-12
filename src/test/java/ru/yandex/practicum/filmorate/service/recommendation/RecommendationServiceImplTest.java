package ru.yandex.practicum.filmorate.service.recommendation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.managment.db.UserDbStorage;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    }

    @Test
    @DisplayName("Получение рекомендаций - пользователь без лайков")
    void getRecommendations_UserWithoutLikes_ReturnsEmptyList() {
        Long userId = 1L;
        User user = createTestUser(userId);

        when(userDbStorage.getUserById(userId)).thenReturn(Optional.of(user));
        when(filmDbStorage.getLikesByUsers()).thenReturn(new HashMap<>());

        List<FilmDTO> result = recommendationService.getRecommendations(userId);

        assertTrue(result.isEmpty(), "Должен возвращаться пустой список при отсутствии лайков");
        assertEquals(0, result.size());
        verify(filmDbStorage, times(1)).getLikesByUsers();
        verify(filmDbStorage, never()).getPopularFilms(anyInt());
    }

    @Test
    @DisplayName("Получение рекомендаций - найдены рекомендации")
    void getRecommendations_FoundRecommendations_ReturnsFilms() {
        Long userId1 = 1L;
        Long userId2 = 2L;
        User user1 = createTestUser(userId1);

        Film film3 = createTestFilm(3L);
        FilmDTO filmDTO3 = createTestFilmDTO(3L);

        when(userDbStorage.getUserById(userId1)).thenReturn(Optional.of(user1));

        Map<Long, Set<Long>> likesByUser = new HashMap<>();
        /**
         * Пользователь 1 лайкнул фильмы 1 и 2
          */
        likesByUser.put(userId1, new HashSet<>(Arrays.asList(1L, 2L)));
        /**
         *   Пользователь 2 лайкнул фильмы 1, 2 и 3
          */
        likesByUser.put(userId2, new HashSet<>(Arrays.asList(1L, 2L, 3L)));

        when(filmDbStorage.getLikesByUsers()).thenReturn(likesByUser);
        when(filmDbStorage.getFilmsByIds(anySet())).thenReturn(Collections.singletonList(film3));
        when(filmMapper.toDTO(film3)).thenReturn(filmDTO3);

        List<FilmDTO> result = recommendationService.getRecommendations(userId1);

        assertEquals(1, result.size(), "Должен вернуться один рекомендованный фильм");
        assertEquals(3L, result.get(0).getId(), "Должен вернуться фильм с ID 3");
        verify(filmDbStorage, times(1)).getFilmsByIds(anySet());
    }

    @Test
    @DisplayName("Получение рекомендаций - нет общих лайков")
    void getRecommendations_NoCommonLikes_ReturnsEmptyList() {
        Long userId1 = 1L;
        Long userId2 = 2L;
        User user1 = createTestUser(userId1);

        when(userDbStorage.getUserById(userId1)).thenReturn(Optional.of(user1));

        Map<Long, Set<Long>> likesByUser = new HashMap<>();
        /**
         *  У пользователей нет общих лайков
          */
        likesByUser.put(userId1, new HashSet<>(Collections.singletonList(1L)));
        likesByUser.put(userId2, new HashSet<>(Collections.singletonList(2L)));

        when(filmDbStorage.getLikesByUsers()).thenReturn(likesByUser);

        List<FilmDTO> result = recommendationService.getRecommendations(userId1);

        assertTrue(result.isEmpty(), "Должен возвращаться пустой список при отсутствии общих лайков");
        assertEquals(0, result.size());
        verify(filmDbStorage, never()).getPopularFilms(anyInt());
    }

    @Test
    @DisplayName("Получение рекомендаций - нет уникальных фильмов для рекомендации")
    void getRecommendations_NoUniqueFilms_ReturnsEmptyList() {
        Long userId1 = 1L;
        Long userId2 = 2L;
        User user1 = createTestUser(userId1);

        when(userDbStorage.getUserById(userId1)).thenReturn(Optional.of(user1));

        Map<Long, Set<Long>> likesByUser = new HashMap<>();
        /**
         * У пользователей одинаковые лайки
          */
        likesByUser.put(userId1, new HashSet<>(Arrays.asList(1L, 2L)));
        likesByUser.put(userId2, new HashSet<>(Arrays.asList(1L, 2L)));

        when(filmDbStorage.getLikesByUsers()).thenReturn(likesByUser);

        List<FilmDTO> result = recommendationService.getRecommendations(userId1);

        assertTrue(result.isEmpty(), "Должен возвращаться пустой список при отсутствии уникальных фильмов");
        assertEquals(0, result.size());
        verify(filmDbStorage, never()).getPopularFilms(anyInt());
    }

    @Test
    @DisplayName("Получение рекомендаций - недостаточно пользователей с лайками")
    void getRecommendations_InsufficientUsers_ReturnsEmptyList() {
        Long userId = 1L;
        User user = createTestUser(userId);

        when(userDbStorage.getUserById(userId)).thenReturn(Optional.of(user));

        Map<Long, Set<Long>> likesByUser = new HashMap<>();
        /**
         * Только один пользователь с лайками
          */
        likesByUser.put(userId, new HashSet<>(Arrays.asList(1L, 2L)));

        when(filmDbStorage.getLikesByUsers()).thenReturn(likesByUser);

        List<FilmDTO> result = recommendationService.getRecommendations(userId);

        assertTrue(result.isEmpty(), "Должен возвращаться пустой список при недостаточном количестве пользователей");
        assertEquals(0, result.size());
    }
}