package ru.yandex.practicum.filmorate.service.film.filmValidation;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.dto.MpaDTO;
import ru.yandex.practicum.filmorate.model.Film;

@Component
public interface FilmValidator {

    void validateFilmUniquenessForUpdate(Film existingFilm,
                                         Film updatedFilm);

    void validateFilmUniqueness(String name, int releaseYear);

    String buildDuplicateErrorMessage(String name, int releaseYear);

    void validateMpa(MpaDTO mpa);

    void validateAndPrepareGenres(Film film);

    void validateAndPrepareDirectors(FilmDTO filmDTO);

    void validateCommonFilmsParams(Long userId, Long friendId);

    void validatePopularFilmsParams(Integer count);
}
