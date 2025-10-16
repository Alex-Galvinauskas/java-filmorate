package ru.yandex.practicum.filmorate.service.film.validation;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;

@Component
public interface FilmValidatorRules {

    void validateFilmUniquenessForUpdate(Film existingFilm,
                                         Film updatedFilm);

    void validateFilmUniqueness(String name, int releaseYear);

    default String buildDuplicateErrorMessage(String name, int releaseYear) {
        return null;
    }
}
