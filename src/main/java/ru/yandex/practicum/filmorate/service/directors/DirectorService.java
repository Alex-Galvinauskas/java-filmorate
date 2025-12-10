package ru.yandex.practicum.filmorate.service.directors;

import ru.yandex.practicum.filmorate.dto.DirectorDTO;

import java.util.List;

public interface DirectorService {
    DirectorDTO createDirector(DirectorDTO directorDTO);

    DirectorDTO updateDirector(DirectorDTO directorDTO);

    DirectorDTO getById(Long id);

    List<DirectorDTO> getAll();

    void delete(Long id);
}
