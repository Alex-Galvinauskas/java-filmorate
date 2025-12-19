package ru.yandex.practicum.filmorate.service.film.filmValidation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dto.DirectorDTO;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.dto.MpaDTO;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.managment.inMemory.FilmStorage;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.service.directors.DirectorService;
import ru.yandex.practicum.filmorate.service.film.GenreService;
import ru.yandex.practicum.filmorate.service.film.MpaService;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class FilmValidatorService implements FilmValidator {

    private final FilmStorage filmStorage;
    private final MpaService mpaService;
    private final GenreService genreService;
    private final DirectorService directorService;

    @Override
    public void validateFilmUniquenessForUpdate(Film existingFilm, Film updatedFilm) {
        log.debug("Валидация уникальности для обновления. Существующий: ID={}, name={}, year={}. Обновляемый: name={}, year={}",
                existingFilm.getId(), existingFilm.getName(),
                existingFilm.getReleaseDate() != null ? existingFilm.getReleaseDate().getYear() : "null",
                updatedFilm.getName(),
                updatedFilm.getReleaseDate() != null ? updatedFilm.getReleaseDate().getYear() : "null");

        boolean nameChanged = !existingFilm.getName().equals(updatedFilm.getName());
        boolean yearChanged = existingFilm.getReleaseDate().getYear() != updatedFilm.getReleaseDate().getYear();

        if (nameChanged || yearChanged) {
            Set<Long> existingDirectors = existingFilm.getDirectors().stream()
                    .map(Director::getId)
                    .collect(Collectors.toSet());

            Set<Long> updatedDirectors = updatedFilm.getDirectors().stream()
                    .map(Director::getId)
                    .collect(Collectors.toSet());

            if (!existingDirectors.isEmpty() && !updatedDirectors.isEmpty() &&
                    existingDirectors.equals(updatedDirectors)) {
                validateFilmUniquenessExcludingId(
                        updatedFilm.getName(),
                        updatedFilm.getReleaseDate().getYear(),
                        existingFilm.getId()
                );
            } else {
                log.debug("Режиссеры изменились или разные, разрешаем обновление");
            }
        } else {
            log.debug("Название и год не изменились, пропускаем проверку уникальности");
        }
    }

    @Override
    public void validateFilmUniqueness(String name, int releaseYear) {
        log.debug("Проверка уникальности фильма: {} ({})", name, releaseYear);

        if (filmStorage.existsFilmByNameAndReleaseYear(name, releaseYear)) {
            throw new DuplicateException(buildDuplicateErrorMessage(name, releaseYear));
        }
    }

    @Override
    public String buildDuplicateErrorMessage(String name, int releaseYear) {
        return String.format("Фильм с названием '%s' и годом выхода '%s' уже существует", name, releaseYear);
    }

    /**
     * Валидирует MPA рейтинг
     */
    public void validateMpa(MpaDTO mpa) {
        if (mpa == null) {
            throw new IllegalArgumentException("MPA рейтинг не может быть null");
        }
        if (mpa.getId() == null) {
            throw new IllegalArgumentException("ID MPA рейтинга не может быть null");
        }

        mpaService.getMpaById(mpa.getId());
    }

    /**
     * Валидирует и подготавливает список жанров для фильма.
     * Убирает дубликаты и проверяет существование жанров
     */
    public void validateAndPrepareGenres(Film film) {
        if (film == null) {
            log.debug("Film is null, skipping genre validation");
            return;
        }

        log.debug("Начальная валидация жанров для фильма: {}", film.getGenres());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Genre> distinctGenres = film.getGenres().stream()
                    .filter(genre -> genre.getId() != null)
                    .collect(Collectors.toMap(
                            Genre::getId,
                            genre -> genre,
                            (existing, replacement) -> existing
                    ))
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(Genre::getId))
                    .collect(Collectors.toList());

            log.debug("Жанры после удаления дубликатов и сортировки: {}", distinctGenres);

            for (Genre genre : distinctGenres) {
                genreService.getGenreById(genre.getId());
            }

            film.setGenres(distinctGenres);
            log.debug("Финальный список жанров для фильма: {}", film.getGenres());
        } else {
            log.debug("Жанры не указаны или пусты");
        }
    }

    /**
     * Валидирует и подготавливает список режиссеров для фильма.
     * Убирает дубликаты и проверяет существование режиссеров.
     */
    public void validateAndPrepareDirectors(FilmDTO filmDTO) {
        if (filmDTO.getDirectors() != null && !filmDTO.getDirectors().isEmpty()) {
            for (DirectorDTO directorDTO : filmDTO.getDirectors()) {
                if (directorDTO.getId() == null) {
                    throw new IllegalArgumentException("ID режиссера не может быть null");
                }
                directorService.getById(directorDTO.getId());
            }

            List<DirectorDTO> uniqueDirectors = filmDTO.getDirectors().stream()
                    .collect(Collectors.toMap(
                            DirectorDTO::getId,
                            d -> d,
                            (d1, d2) -> d1
                    ))
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(DirectorDTO::getId))
                    .collect(Collectors.toList());

            filmDTO.setDirectors(uniqueDirectors);
        }
    }

    /**
     * Валидация параметров для поиска общих фильмов
     */
    public void validateCommonFilmsParams(Long userId, Long friendId) {
        if (userId == null) {
            throw new IllegalArgumentException("Идентификатор пользователя не может быть null");
        }
        if (friendId == null) {
            throw new IllegalArgumentException("Идентификатор друга не может быть null");
        }

        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Нельзя искать общие фильмы с самим собой");
        }
    }

    /**
     * Валидация параметров для популярных фильмов
     */
    public void validatePopularFilmsParams(Integer count) {
        if (count != null && count <= 0) {
            log.debug("Передано недопустимое значение count: {}, будет использовано значение по умолчанию", count);
        }
    }

    /**
     * Валидация поисковых параметров
     */
    public void validateSearchParams(String query, String searchBy) {
        if (query == null && searchBy == null) {
            log.debug("При поиске фильмов не были переданы параметры запроса -> в ответ список всех фильмов по популярности.");
            return;
        } else if (query == null || searchBy == null) {
            log.debug("При поиске фильмов должно быть указано 2 параметра, но был указан только 1.");
            throw new IllegalArgumentException("Для осуществления поиска параметры 'query' и 'by' должны иметь непустые значения.");
        }
    }

    private void validateFilmUniquenessExcludingId(String name, int releaseYear, Long excludedId) {
        log.debug("Проверка уникальности фильма: {} ({}), исключая ID: {}", name, releaseYear, excludedId);

        if (filmStorage.existsFilmByNameAndReleaseYearExcludingId(name, releaseYear, excludedId)) {
            throw new DuplicateException(buildDuplicateErrorMessage(name, releaseYear));
        }
    }
}