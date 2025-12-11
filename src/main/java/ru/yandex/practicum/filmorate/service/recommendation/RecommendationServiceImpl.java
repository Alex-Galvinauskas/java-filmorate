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
         *  Проверяем существование пользователя
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
         * Если у пользователя нет лайков, возвращаем пустой список
          */
        if (currentUserLikes.isEmpty()) {
            log.debug("У пользователя {} нет лайков, возвращаем пустой список", userId);
            return Collections.emptyList();
        }

        /**
         * Если в системе меньше 2 пользователей или только один пользователь с лайками
          */
        long usersWithLikesCount = likesByUser.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .count();

        if (usersWithLikesCount < 2) {
            log.debug("Недостаточно пользователей с лайками для рекомендаций (только {})", usersWithLikesCount);
            return Collections.emptyList();
        }

        /**
         * Находим пользователей с максимальным пересечением по лайкам
          */
        Map<Long, Integer> commonLikesMap = new HashMap<>();

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
             * Если у другого пользователя нет лайков, пропускаем
              */
            if (otherUserLikes.isEmpty()) {
                continue;
            }

            /**
             * Находим пересечение лайков
              */
            Set<Long> commonLikes = new HashSet<>(currentUserLikes);
            commonLikes.retainAll(otherUserLikes);

            if (!commonLikes.isEmpty()) {
                commonLikesMap.put(otherUserId, commonLikes.size());
            }
        }

        /**
         * Если не нашли пользователей с общими лайками, возвращаем пустой список
          */
        if (commonLikesMap.isEmpty()) {
            log.debug("Не найдено пользователей с общими лайками для пользователя {}", userId);
            return Collections.emptyList();
        }

        /**
         * Находим лучших совпадений (пользователей с максимальным количеством общих лайков)
          */
        int maxCommonLikes = commonLikesMap.values().stream()
                .max(Integer::compareTo)
                .orElse(0);

        List<Long> bestMatchUserIds = commonLikesMap.entrySet().stream()
                .filter(entry -> entry.getValue() == maxCommonLikes)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.debug("Найдено {} пользователей с {} общими лайками для пользователя {}",
                bestMatchUserIds.size(), maxCommonLikes, userId);

        /**
         * Собираем рекомендованные фильмы от всех лучших совпадений
          */
        Set<Long> recommendedFilmIds = new HashSet<>();

        for (Long bestMatchUserId : bestMatchUserIds) {
            Set<Long> bestMatchUserLikes = likesByUser.get(bestMatchUserId);

            /**
             * Находим фильмы, которые понравились похожему пользователю, но не текущему
              */
            Set<Long> uniqueFilmIds = new HashSet<>(bestMatchUserLikes);
            uniqueFilmIds.removeAll(currentUserLikes);

            recommendedFilmIds.addAll(uniqueFilmIds);
        }

        /**
         * Если нет рекомендованных фильмов, возвращаем пустой список
          */
        if (recommendedFilmIds.isEmpty()) {
            log.debug("Нет уникальных фильмов для рекомендации пользователю {}", userId);
            return Collections.emptyList();
        }

        /**
         * Получаем информацию о рекомендованных фильмах
          */
        List<Film> recommendedFilms = filmDbStorage.getFilmsByIds(recommendedFilmIds);

        /**
         * Сортируем по популярности (количеству лайков)
          */
        recommendedFilms.sort((f1, f2) -> {
            int likes1 = filmDbStorage.getLikesByFilmId(f1.getId()).size();
            int likes2 = filmDbStorage.getLikesByFilmId(f2.getId()).size();
            return Integer.compare(likes2, likes1); // по убыванию
        });

        /**
         * Ограничиваем количество рекомендаций
          */
        int limit = 10;
        if (recommendedFilms.size() > limit) {
            recommendedFilms = recommendedFilms.subList(0, limit);
        }

        log.debug("Возвращаем {} рекомендованных фильмов для пользователя {}",
                recommendedFilms.size(), userId);

        return recommendedFilms.stream()
                .map(filmMapper::toDTO)
                .collect(Collectors.toList());
    }
}