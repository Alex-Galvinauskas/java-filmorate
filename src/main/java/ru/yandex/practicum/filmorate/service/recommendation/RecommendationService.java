package ru.yandex.practicum.filmorate.service.recommendation;

import ru.yandex.practicum.filmorate.dto.FilmDTO;

import java.util.List;

public interface RecommendationService {
    List<FilmDTO> getRecommendations(Long userId);
}
