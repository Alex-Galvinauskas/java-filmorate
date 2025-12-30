package ru.yandex.practicum.filmorate.service.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmSearchServiceImpl implements FilmSearchService {

    private final FilmDbStorage filmDbStorage;
    private final FilmMapper filmMapper;
    // УДАЛЕНО: private final FilmService filmService;

    private static final List<String> AVAILABLE_SEARCH_FIELDS = List.of("title", "director");

    @Override
    public List<FilmDTO> searchFilms(String query, String searchBy) {
        if (query == null && searchBy == null) {
            log.debug("При поиске фильмов не были переданы параметры запроса " +
                    "-> в ответ список всех фильмов по популярности.");
            // Получаем все фильмы и сортируем по популярности
            return filmDbStorage.getAllFilms().stream()
                    .sorted(Comparator.comparingInt((Film f) -> f.getLikes().size()).reversed())
                    .map(filmMapper::toDTO)
                    .collect(Collectors.toList());
        } else if (query == null || searchBy == null) {
            log.debug("При поиске фильмов должно быть указано 2 параметра, но был указан только 1.");
            throw new IllegalArgumentException("Для осуществления поиска параметры 'query' и" +
                    " 'by' должны иметь непустые значения.");
        }

        List<String> searchByParams = Stream.of(searchBy.split(","))
                .peek(searchField -> {
                    if (!AVAILABLE_SEARCH_FIELDS.contains(searchField)) {
                        log.debug("При поиске фильмов передано недопустимое для параметра 'by' значение: {}", searchField);
                        throw new IllegalArgumentException("Параметр 'by' может принимать только значения 'title'/'director'");
                    }
                })
                .collect(Collectors.toList());

        List<Film> filmsByTitle = List.of();
        List<Film> filmsByDirector = List.of();

        for (String searchField : searchByParams) {
            if (searchField.equalsIgnoreCase("title")) {
                filmsByTitle = filmDbStorage.getFilmsViaSearchByName(query);
                log.debug("При поиске фильмов по названию найдено фильмов: {}", filmsByTitle.size());
            } else if (searchField.equalsIgnoreCase("director")) {
                filmsByDirector = filmDbStorage.getFilmsViaSearchByDirector(query);
                log.debug("При поиске фильмов по имени режиссера найдено фильмов: {}", filmsByDirector.size());
            }
        }

        return Stream
                .concat(filmsByTitle.stream(), filmsByDirector.stream())
                .distinct()
                .sorted(Comparator.comparingInt((Film f) -> f.getLikes().size()).reversed())
                .map(filmMapper::toDTO)
                .collect(Collectors.toList());
    }
}