package ru.yandex.practicum.filmorate.service.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.film.filmValidation.FilmValidator;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PopularFilmService {

    private final FilmDbStorage filmDbStorage;
    private final FilmMapper filmMapper;
    private final FilmValidator filmValidator;

    /**
     * Возвращает список популярных фильмов.
     * Популярность определяется количеством лайков.
     * Поддерживает фильтрацию по жанру и году.
     *
     * @param count количество фильмов (если null или отрицательное, то по умолчанию 10)
     * @param genreId ID жанра для фильтрации (может быть null)
     * @param year год выпуска для фильтрации (может быть null)
     *
     * @return список популярных фильмов, сортированных по количеству лайков по убыванию
     */
    public List<FilmDTO> getPopularFilms(Integer count, Integer genreId, Integer year) {
        log.debug("Получение списка популярных фильмов. Количество: {}, genreId: {}, year: {}",
                count, genreId, year);

        filmValidator.validatePopularFilmsParams(count);

        List<Film> films = filmDbStorage.getPopularFilms(count, genreId, year);

        if (films == null || films.isEmpty()) {
            log.debug("Список популярных фильмов пуст");
            return Collections.emptyList();
        }

        log.debug("Найдено {} популярных фильмов", films.size());


        return films.stream()
                .map(filmMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает количество фильмов по умолчанию.
     * Может использоваться для конфигурации или UI.
     *
     * @return стандартное количество фильмов
     */
    public int getDefaultCount() {
        return 10;
    }

    /**
     * Проверяет, является ли значение count валидным.
     *
     * @param count значение для проверки
     * @return true если значение валидно (null, 0 или положительное)
     */
    public boolean isValidCount(Integer count) {
        return count == null || count >= 0;
    }
}