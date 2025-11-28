package ru.yandex.practicum.filmorate.service.film;

import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

public interface MpaService {
    List<Mpa> getAllMpa();

    Mpa getMpaById(Integer id);
}