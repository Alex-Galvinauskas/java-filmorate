package ru.yandex.practicum.filmorate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.filmorate.dto.DirectorDTO;
import ru.yandex.practicum.filmorate.model.Director;

@Mapper(componentModel = "spring")
public interface DirectorMapper {

    @Mapping(target = "createdAt", ignore = true)
    Director toEntity(DirectorDTO directorDto);

    DirectorDTO toDTO(Director director);
}
