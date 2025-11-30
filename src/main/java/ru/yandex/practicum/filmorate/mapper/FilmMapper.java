package ru.yandex.practicum.filmorate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.model.Film;

@Mapper(componentModel = "spring")
public interface FilmMapper {

    @Mapping(source = "releaseDate", target = "releaseDate")
    @Mapping(source = "mpa", target = "mpa")
    @Mapping(source = "genres", target = "genres")
    @Mapping(source = "likes", target = "likes")
    FilmDTO toDTO(Film film);

    @Mapping(source = "releaseDate", target = "releaseDate")
    @Mapping(source = "mpa", target = "mpa")
    @Mapping(source = "genres", target = "genres")
    @Mapping(source = "likes", target = "likes")
    Film toEntity(FilmDTO filmDTO);
}