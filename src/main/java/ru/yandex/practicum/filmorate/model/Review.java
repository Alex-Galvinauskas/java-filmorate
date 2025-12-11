package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    private Long reviewId;

    private String content;

    private Boolean isPositive;

    private Long userId;

    private Long filmId;

    @Builder.Default
    private Integer useful = 0;

    @Builder.Default
    private Set<Long> likes = ConcurrentHashMap.newKeySet();

    @Builder.Default
    private Set<Long> dislikes = ConcurrentHashMap.newKeySet();

    public static Review copyWithId(Review source, Long newId) {
        if (source == null) {
            throw new IllegalArgumentException("Исходный отзыв не может быть null");
        }

        Set<Long> copiedLikes = ConcurrentHashMap.newKeySet();
        if (source.getLikes() != null) {
            copiedLikes.addAll(source.getLikes());
        }

        Set<Long> copiedDislikes = ConcurrentHashMap.newKeySet();
        if (source.getDislikes() != null) {
            copiedDislikes.addAll(source.getDislikes());
        }

        return Review.builder()
                .reviewId(newId)
                .content(source.getContent())
                .isPositive(source.getIsPositive())
                .userId(source.getUserId())
                .filmId(source.getFilmId())
                .useful(source.getUseful())
                .likes(copiedLikes)
                .dislikes(copiedDislikes)
                .build();
    }
}


