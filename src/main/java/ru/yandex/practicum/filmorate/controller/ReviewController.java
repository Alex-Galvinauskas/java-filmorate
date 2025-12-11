package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.ReviewDTO;
import ru.yandex.practicum.filmorate.service.review.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@Slf4j
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(@Valid @RequestBody ReviewDTO reviewDTO) {
        log.info("POST /reviews - создание нового отзыва");
        ReviewDTO createdReview = reviewService.createReview(reviewDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReview);
    }

    @PutMapping
    public ResponseEntity<ReviewDTO> updateReview(@Valid @RequestBody ReviewDTO reviewDTO) {
        log.info("PUT /reviews - обновление отзыва с ID: {}", reviewDTO.getReviewId());
        ReviewDTO updatedReview = reviewService.updateReview(reviewDTO);
        return ResponseEntity.ok(updatedReview);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        log.info("DELETE /reviews/{} - удаление отзыва", id);
        reviewService.deleteReview(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable Long id) {
        log.info("GET /reviews/{} - получение отзыва по ID", id);
        ReviewDTO review = reviewService.getReviewById(id);
        return ResponseEntity.ok(review);
    }

    @GetMapping
    public ResponseEntity<List<ReviewDTO>> getReviews(
            @RequestParam(required = false) Long filmId,
            @RequestParam(required = false, defaultValue = "10") Integer count) {
        log.info("GET /reviews?filmId={}&count={} - получение отзывов", filmId, count);
        List<ReviewDTO> reviews = reviewService.getReviews(filmId, count);
        return ResponseEntity.ok(reviews);
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("PUT /reviews/{}/like/{} - добавление лайка", id, userId);
        reviewService.addLike(id, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/dislike/{userId}")
    public ResponseEntity<Void> addDislike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("PUT /reviews/{}/dislike/{} - добавление дизлайка", id, userId);
        reviewService.addDislike(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> removeLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("DELETE /reviews/{}/like/{} - удаление лайка", id, userId);
        reviewService.removeLike(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    public ResponseEntity<Void> removeDislike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("DELETE /reviews/{}/dislike/{} - удаление дизлайка", id, userId);
        reviewService.removeDislike(id, userId);
        return ResponseEntity.ok().build();
    }
}


