package ru.yandex.practicum.filmorate.service.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.ReviewDTO;
import ru.yandex.practicum.filmorate.dto.UserDTO;
import ru.yandex.practicum.filmorate.exception.ForbiddenException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.managment.db.ReviewDbStorage;
import ru.yandex.practicum.filmorate.mapper.ReviewMapper;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.feed.FeedService;
import ru.yandex.practicum.filmorate.service.film.FilmService;
import ru.yandex.practicum.filmorate.service.user.UserService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewDbStorage reviewDbStorage;
    private final ReviewMapper reviewMapper;
    private final FilmService filmService;
    private final UserService userService;
    private final FeedService feedService;

    @Override
    public ReviewDTO createReview(ReviewDTO reviewDTO) {
        log.debug("Создание нового отзыва для фильма {} от пользователя {}",
                reviewDTO.getFilmId(), reviewDTO.getUserId());

        if (reviewDTO.getUserId() == null) {
            throw new ValidationException("ID пользователя не может быть null при создании отзыва");
        }
        if (reviewDTO.getFilmId() == null) {
            throw new ValidationException("ID фильма не может быть null при создании отзыва");
        }

        filmService.getFilmById(reviewDTO.getFilmId());
        userService.getUserById(reviewDTO.getUserId());

        reviewDTO.setUseful(0);

        Review review = reviewMapper.toEntity(reviewDTO);
        Review createdReview = reviewDbStorage.createReview(review);
        ReviewDTO result = reviewMapper.toDTO(createdReview);

        // ИСПРАВЛЕНО: передаем filmId вместо reviewId для entityId
        feedService.recordEvent(
                reviewDTO.getUserId(),
                reviewDTO.getUserId(),
                EventType.REVIEW,
                Operation.ADD,
                reviewDTO.getFilmId()  // ← ВАЖНО: передаем filmId, а не reviewId!
        );

        List<Long> friendIds = userService.getFriends(reviewDTO.getUserId())
                .stream()
                .map(UserDTO::getId)
                .toList();
        for (Long friendId : friendIds) {
            feedService.recordEvent(
                    friendId,
                    reviewDTO.getUserId(),
                    EventType.REVIEW,
                    Operation.ADD,
                    reviewDTO.getFilmId()
            );
        }

        log.info("Создан отзыв с ID: {}", result.getReviewId());
        return result;
    }


    @Override
    public ReviewDTO updateReview(ReviewDTO reviewDTO) {
        log.debug("Обновление отзыва с ID: {}", reviewDTO.getReviewId());

        if (reviewDTO.getReviewId() == null) {
            throw new NotFoundException("ID отзыва не может быть null при обновлении");
        }

        ReviewDTO existingReview = getReviewById(reviewDTO.getReviewId());

        reviewDTO.setUserId(existingReview.getUserId());
        if (reviewDTO.getFilmId() == null) {
            reviewDTO.setFilmId(existingReview.getFilmId());
        }

        Review review = reviewMapper.toEntity(reviewDTO);
        Review updatedReview = reviewDbStorage.updateReview(review);
        ReviewDTO result = reviewMapper.toDTO(updatedReview);

        // ИСПРАВЛЕНО: передаем filmId вместо reviewId для entityId
        feedService.recordEvent(
                reviewDTO.getUserId(),
                reviewDTO.getUserId(),
                EventType.REVIEW,
                Operation.UPDATE,
                reviewDTO.getFilmId()  // ← ВАЖНО: передаем filmId, а не reviewId!
        );

        log.info("Обновлен отзыв с ID: {}", result.getReviewId());
        return result;
    }

    @Override
    public void deleteReview(Long id, Long userId) {
        Optional<Review> existingReviewOpt = reviewDbStorage.getReviewById(id);

        if (existingReviewOpt.isEmpty()) {
            log.debug("Попытка удаления несуществующего отзыва с ID: {}", id);
            return;
        }

        Review existingReview = existingReviewOpt.get();
        ReviewDTO existingReviewDTO = reviewMapper.toDTO(existingReview);

        if (userId != null) {
            log.debug("Удаление отзыва с ID: {} пользователем {}", id, userId);
            if (!existingReviewDTO.getUserId().equals(userId)) {
                throw new ForbiddenException("Пользователь может удалять только свои отзывы");
            }
            log.info("Удален отзыв с ID: {} пользователем {}", id, userId);
        } else {
            log.warn("Удаление отзыва с ID: {} без указания userId (обратная совместимость)", id);
            log.info("Удален отзыв с ID: {}", id);
            userId = existingReview.getUserId();
        }

        reviewDbStorage.deleteReview(id);

        // ИСПРАВЛЕНО: передаем filmId вместо reviewId для entityId
        feedService.recordEvent(
                userId,
                userId,
                EventType.REVIEW,
                Operation.REMOVE,
                existingReview.getFilmId()  // ← ВАЖНО: передаем filmId, а не reviewId!
        );
    }

    @Override
    public ReviewDTO getReviewById(Long id) {
        log.debug("Получение отзыва по ID: {}", id);
        Review review = reviewDbStorage.getReviewById(id)
                .orElseThrow(() -> new NotFoundException("Отзыв с ID " + id + " не найден"));
        return reviewMapper.toDTO(review);
    }

    @Override
    public List<ReviewDTO> getReviews(Long filmId, Integer count) {
        log.debug("Получение отзывов. Фильм: {}, количество: {}", filmId, count);

        List<Review> reviews;
        if (filmId != null) {
            reviews = reviewDbStorage.getReviewsByFilmId(filmId, count);
        } else {
            reviews = reviewDbStorage.getAllReviews(count);
        }

        return reviews.stream()
                .map(reviewMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void addLike(Long reviewId, Long userId) {
        log.debug("Добавление лайка отзыву {} от пользователя {}", reviewId, userId);

        getReviewById(reviewId);
        userService.getUserById(userId);

        reviewDbStorage.addLike(reviewId, userId);
        log.info("Добавлен лайк отзыву {} от пользователя {}", reviewId, userId);
    }

    @Override
    public void addDislike(Long reviewId, Long userId) {
        log.debug("Добавление дизлайка отзыву {} от пользователя {}", reviewId, userId);

        getReviewById(reviewId);
        userService.getUserById(userId);

        reviewDbStorage.addDislike(reviewId, userId);
        log.info("Добавлен дизлайк отзыву {} от пользователя {}", reviewId, userId);
    }

    @Override
    public void removeLike(Long reviewId, Long userId) {
        log.debug("Удаление лайка отзыву {} от пользователя {}", reviewId, userId);

        getReviewById(reviewId);
        userService.getUserById(userId);

        reviewDbStorage.removeLike(reviewId, userId);
        log.info("Удален лайк отзыву {} от пользователя {}", reviewId, userId);
    }

    @Override
    public void removeDislike(Long reviewId, Long userId) {
        log.debug("Удаление дизлайка отзыву {} от пользователя {}", reviewId, userId);

        getReviewById(reviewId);
        userService.getUserById(userId);

        reviewDbStorage.removeDislike(reviewId, userId);
        log.info("Удален дизлайк отзыву {} от пользователя {}", reviewId, userId);
    }
}