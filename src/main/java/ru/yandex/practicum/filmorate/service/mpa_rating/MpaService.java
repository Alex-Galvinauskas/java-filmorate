package ru.yandex.practicum.filmorate.service.mpa_rating;

import ru.yandex.practicum.filmorate.dto.MpaDTO;

import java.util.List;

public interface MpaService {
    List<MpaDTO> getAllMpa();

    MpaDTO getMpaById(Long id);
}