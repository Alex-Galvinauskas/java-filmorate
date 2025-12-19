package ru.yandex.practicum.filmorate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("reviewId")
    private Long reviewId;

    @NotBlank(message = "Содержание отзыва не может быть пустым")
    private String content;

    @NotNull(message = "Тип отзыва (положительный/негативный) должен быть указан")
    @JsonProperty("isPositive")
    private Boolean isPositive;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("filmId")
    private Long filmId;

    @Builder.Default
    @JsonProperty("useful")
    private Integer useful = 0;

    @Builder.Default
    @JsonProperty("likes")
    private Set<Long> likes = new HashSet<>();

    @Builder.Default
    @JsonProperty("dislikes")
    private Set<Long> dislikes = new HashSet<>();
}


