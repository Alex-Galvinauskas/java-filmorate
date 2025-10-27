package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.annotation.MinReleaseDate;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Film {

    private Long id;

    @NotBlank(message = "Название фильма не может быть пустым")
    @Size(max = 100, message = "Название фильма не может быть длиннее 100 символов")
    private String name;

    @Size(max = 200, message = "Описание фильма не может быть длиннее 200 символов")
    private String description;

    @NotNull(message = "Дата релиза обязательна")
    @MinReleaseDate
    private LocalDate releaseDate;

    @Positive(message = "Продолжительность фильма должна быть положительным числом")
    private Integer duration;

    @Builder.Default
    private Set<Long> likes = ConcurrentHashMap.newKeySet();

    public static Film copyWithId(Film source, Long newId) {
        if (source == null) {
            throw new IllegalArgumentException("Исходный файл не может быть null");
        }
        return Film.builder()
                .id(newId)
                .name(source.getName())
                .description(source.getDescription())
                .releaseDate(source.getReleaseDate())
                .duration(source.getDuration())
                .build();
    }
}