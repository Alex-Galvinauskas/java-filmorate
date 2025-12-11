package ru.yandex.practicum.filmorate.managment.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.managment.inMemory.ReviewStorage;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
@Primary
@Slf4j
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Review createReview(Review review) {
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id, useful) " +
                "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"review_id"});
            stmt.setString(1, review.getContent());
            stmt.setBoolean(2, review.getIsPositive());
            stmt.setLong(3, review.getUserId());
            stmt.setLong(4, review.getFilmId());
            stmt.setInt(5, review.getUseful() != null ? review.getUseful() : 0);
            return stmt;
        }, keyHolder);

        Long reviewId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        review.setReviewId(reviewId);

        log.info("Создан новый отзыв в БД: ID: {}, фильм: {}, пользователь: {}", 
                reviewId, review.getFilmId(), review.getUserId());
        return getReviewById(reviewId).orElse(review);
    }

    @Override
    public Optional<Review> getReviewById(Long id) {
        String sql = "SELECT * FROM reviews WHERE review_id = ?";
        log.debug("Поиск отзыва по ID: {}", id);
        List<Review> result = jdbcTemplate.query(sql, new ReviewRowMapper(), id);

        if (result.isEmpty()) {
            return Optional.empty();
        }

        Review review = result.getFirst();
        loadLikesAndDislikes(review);
        return Optional.of(review);
    }

    @Override
    public Review updateReview(Review review) {
        String sql = "UPDATE reviews SET content = ?, is_positive = ? WHERE review_id = ?";

        int updated = jdbcTemplate.update(sql,
                review.getContent(),
                review.getIsPositive(),
                review.getReviewId());

        if (updated == 0) {
            throw new RuntimeException("Отзыв с ID " + review.getReviewId() + " не найден");
        }

        log.info("Обновлен отзыв в БД: ID: {}", review.getReviewId());
        return getReviewById(review.getReviewId()).orElse(review);
    }

    @Override
    public void deleteReview(Long id) {
        String sql = "DELETE FROM reviews WHERE review_id = ?";
        int deleted = jdbcTemplate.update(sql, id);
        if (deleted == 0) {
            throw new RuntimeException("Отзыв с ID " + id + " не найден");
        }
        log.info("Удален отзыв из БД: ID: {}", id);
    }

    @Override
    public List<Review> getReviewsByFilmId(Long filmId, Integer count) {
        int limit = (count != null && count > 0) ? count : 10;
        String sql = "SELECT * FROM reviews WHERE film_id = ? ORDER BY useful DESC LIMIT ?";
        log.debug("Получение отзывов для фильма {} (лимит: {})", filmId, limit);
        List<Review> reviews = jdbcTemplate.query(sql, new ReviewRowMapper(), filmId, limit);
        reviews.forEach(this::loadLikesAndDislikes);
        return reviews;
    }

    @Override
    public List<Review> getAllReviews(Integer count) {
        int limit = (count != null && count > 0) ? count : 10;
        String sql = "SELECT * FROM reviews ORDER BY useful DESC LIMIT ?";
        log.debug("Получение всех отзывов (лимит: {})", limit);
        List<Review> reviews = jdbcTemplate.query(sql, new ReviewRowMapper(), limit);
        reviews.forEach(this::loadLikesAndDislikes);
        return reviews;
    }

    @Override
    public void addLike(Long reviewId, Long userId) {
        String deleteDislikeSql = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ? AND is_like = false";
        jdbcTemplate.update(deleteDislikeSql, reviewId, userId);
        
        String checkSql = "SELECT COUNT(*) FROM review_likes WHERE review_id = ? AND user_id = ? AND is_like = true";
        Integer existingCount = jdbcTemplate.queryForObject(checkSql, Integer.class, reviewId, userId);
        
        if (existingCount == null || existingCount == 0) {
            String sql = "INSERT INTO review_likes (review_id, user_id, is_like) VALUES (?, ?, true)";
            jdbcTemplate.update(sql, reviewId, userId);
            log.debug("Добавлен лайк отзыву {} от пользователя {}", reviewId, userId);
        }
        
        updateUsefulRating(reviewId);
    }

    @Override
    public void addDislike(Long reviewId, Long userId) {
        String deleteLikeSql = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ? AND is_like = true";
        jdbcTemplate.update(deleteLikeSql, reviewId, userId);
        
        String checkSql = "SELECT COUNT(*) FROM review_likes WHERE review_id = ? AND user_id = ? AND is_like = false";
        Integer existingCount = jdbcTemplate.queryForObject(checkSql, Integer.class, reviewId, userId);
        
        if (existingCount == null || existingCount == 0) {
            String sql = "INSERT INTO review_likes (review_id, user_id, is_like) VALUES (?, ?, false)";
            jdbcTemplate.update(sql, reviewId, userId);
            log.debug("Добавлен дизлайк отзыву {} от пользователя {}", reviewId, userId);
        }
        
        updateUsefulRating(reviewId);
    }

    @Override
    public void removeLike(Long reviewId, Long userId) {
        String sql = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ? AND is_like = true";
        int deleted = jdbcTemplate.update(sql, reviewId, userId);
        if (deleted > 0) {
            updateUsefulRating(reviewId);
            log.debug("Удален лайк отзыву {} от пользователя {}", reviewId, userId);
        }
    }

    @Override
    public void removeDislike(Long reviewId, Long userId) {
        String sql = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ? AND is_like = false";
        int deleted = jdbcTemplate.update(sql, reviewId, userId);
        if (deleted > 0) {
            updateUsefulRating(reviewId);
            log.debug("Удален дизлайк отзыву {} от пользователя {}", reviewId, userId);
        }
    }

    private void updateUsefulRating(Long reviewId) {
        String sql = "UPDATE reviews SET useful = " +
                "(SELECT COALESCE(SUM(CASE WHEN is_like THEN 1 ELSE -1 END), 0) " +
                "FROM review_likes WHERE review_id = ?) " +
                "WHERE review_id = ?";
        jdbcTemplate.update(sql, reviewId, reviewId);
    }

    private void loadLikesAndDislikes(Review review) {
        String sql = "SELECT user_id, is_like FROM review_likes WHERE review_id = ?";
        Set<Long> likes = new HashSet<>();
        Set<Long> dislikes = new HashSet<>();

        jdbcTemplate.query(sql, (rs) -> {
            Long userId = rs.getLong("user_id");
            boolean isLike = rs.getBoolean("is_like");
            if (isLike) {
                likes.add(userId);
            } else {
                dislikes.add(userId);
            }
        }, review.getReviewId());

        review.setLikes(likes);
        review.setDislikes(dislikes);
        review.setUseful(likes.size() - dislikes.size());
    }

    private static class ReviewRowMapper implements RowMapper<Review> {
        @Override
        public Review mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Review.builder()
                    .reviewId(rs.getLong("review_id"))
                    .content(rs.getString("content"))
                    .isPositive(rs.getBoolean("is_positive"))
                    .userId(rs.getLong("user_id"))
                    .filmId(rs.getLong("film_id"))
                    .useful(rs.getInt("useful"))
                    .likes(new HashSet<>())
                    .dislikes(new HashSet<>())
                    .build();
        }
    }
}

