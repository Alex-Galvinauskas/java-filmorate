package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {

    private Long reviewId;

    @NotBlank(message = "Содержание отзыва не может быть пустым")
    private String content;

    @NotNull(message = "Тип отзыва (положительный/негативный) должен быть указан")
    private Boolean isPositive;

    @NotNull(message = "ID пользователя не может быть null")
    private Long userId;

    @NotNull(message = "ID фильма не может быть null")
    private Long filmId;

    @Builder.Default
    private Integer useful = 0;

    @Builder.Default
    private Set<Long> likes = new HashSet<>();

    @Builder.Default
    private Set<Long> dislikes = new HashSet<>();
}


