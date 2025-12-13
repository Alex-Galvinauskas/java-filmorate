package ru.yandex.practicum.filmorate.service.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.managment.db.UserDbStorage;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final FilmDbStorage filmDbStorage;
    private final UserDbStorage userDbStorage;
    private final FilmMapper filmMapper;

    @Override
    public List<FilmDTO> getRecommendations(Long userId) {
        log.debug("Получение рекомендаций для пользователя {}", userId);

        // Проверяем существование пользователя
        User user = userDbStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        // Используем оптимизированный метод для получения рекомендаций
        List<Film> recommendedFilms = filmDbStorage.getRecommendationsForUser(userId, 10);

        log.debug("Возвращаем {} рекомендованных фильмов для пользователя {}",
                recommendedFilms.size(), userId);

        return recommendedFilms.stream()
                .map(filmMapper::toDTO)
                .collect(Collectors.toList());
    }
}