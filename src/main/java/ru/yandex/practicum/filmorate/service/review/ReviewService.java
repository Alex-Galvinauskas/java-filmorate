package ru.yandex.practicum.filmorate.service.review;

import ru.yandex.practicum.filmorate.dto.ReviewDTO;

import java.util.List;

public interface ReviewService {
    ReviewDTO createReview(ReviewDTO reviewDTO);

    ReviewDTO updateReview(ReviewDTO reviewDTO);

    void deleteReview(Long id, Long userId);

    ReviewDTO getReviewById(Long id);

    List<ReviewDTO> getReviews(Long filmId, Integer count);

    void addLike(Long reviewId, Long userId);

    void addDislike(Long reviewId, Long userId);

    void removeLike(Long reviewId, Long userId);

    void removeDislike(Long reviewId, Long userId);
}


