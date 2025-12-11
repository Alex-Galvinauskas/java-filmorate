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

        /**
         * Проверяем существование пользователя
          */

        User user = userDbStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        /**
         * Получаем лайки всех пользователей
          */
        Map<Long, Set<Long>> likesByUser = filmDbStorage.getLikesByUsers();

        /**
         * Получаем лайки текущего пользователя
          */
        Set<Long> currentUserLikes = likesByUser.getOrDefault(userId, new HashSet<>());

        /**
         * Если у пользователя нет лайков, возвращаем популярные фильмы
          */
        if (currentUserLikes.isEmpty()) {
            log.debug("У пользователя {} нет лайков, возвращаем популярные фильмы", userId);
            return filmDbStorage.getPopularFilms(10).stream()
                    .map(filmMapper::toDTO)
                    .collect(Collectors.toList());
        }

        /**
         * Находим пользователя с максимальным пересечением по лайкам
          */
        Long bestMatchUserId = null;
        int maxCommonLikes = 0;

        for (Map.Entry<Long, Set<Long>> entry : likesByUser.entrySet()) {
            Long otherUserId = entry.getKey();

            /**
             * Пропускаем текущего пользователя
             */
            if (otherUserId.equals(userId)) {
                continue;
            }

            Set<Long> otherUserLikes = entry.getValue();

            /**
             * Находим пересечение лайков
              */
            Set<Long> commonLikes = new HashSet<>(currentUserLikes);
            commonLikes.retainAll(otherUserLikes);

            if (commonLikes.size() > maxCommonLikes) {
                maxCommonLikes = commonLikes.size();
                bestMatchUserId = otherUserId;
            }
        }

        /**
         * Если не нашли пользователя с общими лайками, возвращаем популярные фильмы
         */
        if (bestMatchUserId == null) {
            log.debug("Не найдено пользователей с общими лайками для пользователя {}", userId);
            return filmDbStorage.getPopularFilms(10).stream()
                    .map(filmMapper::toDTO)
                    .collect(Collectors.toList());
        }

        log.debug("Найден пользователь {} с {} общими лайками для пользователя {}",
                bestMatchUserId, maxCommonLikes, userId);

        /**
         * Получаем лайки похожего пользователя
          */
        Set<Long> bestMatchUserLikes = likesByUser.get(bestMatchUserId);

        /**
         * Находим фильмы, которые понравились похожему пользователю, но не текущему
          */
        Set<Long> recommendedFilmIds = new HashSet<>(bestMatchUserLikes);
        recommendedFilmIds.removeAll(currentUserLikes);

        /**
         * Если нет рекомендованных фильмов, возвращаем популярные
          */
        if (recommendedFilmIds.isEmpty()) {
            log.debug("Нет уникальных фильмов для рекомендации, возвращаем популярные фильмы");
            return filmDbStorage.getPopularFilms(10).stream()
                    .map(filmMapper::toDTO)
                    .collect(Collectors.toList());
        }

        /**
         *  Получаем информацию о рекомендованных фильмах
          */

        List<Film> recommendedFilms = filmDbStorage.getFilmsByIds(recommendedFilmIds);

        log.debug("Найдено {} рекомендованных фильмов для пользователя {}",
                recommendedFilms.size(), userId);

        return recommendedFilms.stream()
                .map(filmMapper::toDTO)
                .collect(Collectors.toList());
    }
}