package ru.yandex.practicum.filmorate.service.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.ReviewDTO;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.ReviewDbStorage;
import ru.yandex.practicum.filmorate.mapper.ReviewMapper;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.film.FilmService;
import ru.yandex.practicum.filmorate.service.user.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewDbStorage reviewDbStorage;
    private final ReviewMapper reviewMapper;
    private final FilmService filmService;
    private final UserService userService;

    @Override
    public ReviewDTO createReview(ReviewDTO reviewDTO) {
        log.debug("Создание нового отзыва для фильма {} от пользователя {}",
                reviewDTO.getFilmId(), reviewDTO.getUserId());

        filmService.getFilmById(reviewDTO.getFilmId());
        userService.getUserById(reviewDTO.getUserId());

        reviewDTO.setUseful(0);

        Review review = reviewMapper.toEntity(reviewDTO);
        Review createdReview = reviewDbStorage.createReview(review);
        ReviewDTO result = reviewMapper.toDTO(createdReview);

        log.info("Создан отзыв с ID: {}", result.getReviewId());
        return result;
    }

    @Override
    public ReviewDTO updateReview(ReviewDTO reviewDTO) {
        log.debug("Обновление отзыва с ID: {}", reviewDTO.getReviewId());

        getReviewById(reviewDTO.getReviewId());

        Review review = reviewMapper.toEntity(reviewDTO);
        Review updatedReview = reviewDbStorage.updateReview(review);
        ReviewDTO result = reviewMapper.toDTO(updatedReview);

        log.info("Обновлен отзыв с ID: {}", result.getReviewId());
        return result;
    }

    @Override
    public void deleteReview(Long id) {
        log.debug("Удаление отзыва с ID: {}", id);

        getReviewById(id);

        reviewDbStorage.deleteReview(id);
        log.info("Удален отзыв с ID: {}", id);
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


