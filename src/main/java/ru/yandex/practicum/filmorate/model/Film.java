package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"name", "releaseDate"})
public class Film {

    private Long id;

    private String name;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonProperty("releaseDate")
    private LocalDate releaseDate;

    private Integer duration;

    private Mpa mpa;

    private List<Genre> genres;

    @Builder.Default
    private Set<Long> likes = ConcurrentHashMap.newKeySet();

    @Builder.Default
    private List<Director> directors = new ArrayList<>();

    @JsonCreator
    public Film(
            @JsonProperty("id") Long id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("releaseDate") LocalDate releaseDate,
            @JsonProperty("duration") Integer duration,
            @JsonProperty("mpa") Mpa mpa,
            @JsonProperty("genres") List<Genre> genres,
            @JsonProperty("directors") List<Director> directors) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.releaseDate = releaseDate;
        this.duration = duration;
        this.mpa = mpa;
        this.genres = genres;
        this.directors = directors;
        this.likes = ConcurrentHashMap.newKeySet();
    }

    public static Film copyWithId(Film source, Long newId) {
        if (source == null) {
            throw new IllegalArgumentException("Исходный файл не может быть null");
        }

        Set<Long> copiedLikes = ConcurrentHashMap.newKeySet();
        if (source.getLikes() != null) {
            copiedLikes.addAll(source.getLikes());
        }

        List<Director> copiedDirectors = new ArrayList<>();
        if (source.getDirectors() != null) {
            copiedDirectors.addAll(source.getDirectors());
        }

        return Film.builder()
                .id(newId)
                .name(source.getName())
                .description(source.getDescription())
                .releaseDate(source.getReleaseDate())
                .duration(source.getDuration())
                .mpa(source.getMpa())
                .genres(source.getGenres())
                .likes(copiedLikes)
                .directors(copiedDirectors)
                .build();
    }
}