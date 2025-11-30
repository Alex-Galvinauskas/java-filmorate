package ru.yandex.practicum.filmorate.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.filmorate.dto.MpaDTO;
import ru.yandex.practicum.filmorate.model.Mpa;

@Mapper(componentModel = "spring")
public interface MpaMapper {
    MpaDTO toDTO(Mpa mpa);

    Mpa toEntity(MpaDTO mpaDTO);
}