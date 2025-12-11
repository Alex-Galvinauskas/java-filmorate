package ru.yandex.practicum.filmorate.managment.inMemory;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewStorage {
    Review createReview(Review review);

    Optional<Review> getReviewById(Long id);

    Review updateReview(Review review);

    void deleteReview(Long id);

    List<Review> getReviewsByFilmId(Long filmId, Integer count);

    List<Review> getAllReviews(Integer count);

    void addLike(Long reviewId, Long userId);

    void addDislike(Long reviewId, Long userId);

    void removeLike(Long reviewId, Long userId);

    void removeDislike(Long reviewId, Long userId);
}


