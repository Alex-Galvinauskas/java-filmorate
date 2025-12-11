package ru.yandex.practicum.filmorate.integrationDb;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.managment.db.GenreDbStorage;
import ru.yandex.practicum.filmorate.managment.db.MpaDbStorage;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, GenreDbStorage.class, MpaDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Transactional
@Rollback
class FilmDbStorageIntegrationTest {

    private final FilmDbStorage filmDbStorage;
    private final JdbcTemplate jdbcTemplate;

    private Film testFilm;

    @BeforeEach
    void setUp() {
        // Очистка базы перед каждым тестом
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM likes");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM genres");
        jdbcTemplate.execute("DELETE FROM mpa_ratings");

        // Восстановление исходных данных
        jdbcTemplate.execute("INSERT INTO mpa_ratings (id, name, description) VALUES (1, 'G', 'Нет возрастных ограничений')");
        jdbcTemplate.execute("INSERT INTO mpa_ratings (id, name, description) VALUES (2, 'PG', 'Детям рекомендуется смотреть с родителями')");
        jdbcTemplate.execute("INSERT INTO mpa_ratings (id, name, description) VALUES (3, 'PG-13', 'Детям до 13 лет просмотр не желателен')");

        jdbcTemplate.execute("INSERT INTO genres (id, name) VALUES (1, 'Комедия')");
        jdbcTemplate.execute("INSERT INTO genres (id, name) VALUES (2, 'Драма')");
        jdbcTemplate.execute("INSERT INTO genres (id, name) VALUES (3, 'Мультфильм')");
        jdbcTemplate.execute("INSERT INTO genres (id, name) VALUES (4, 'Триллер')");

        testFilm = Film.builder()
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1L).name("G").build())
                .genres(List.of(
                        Genre.builder().id(1L).name("Комедия").build(),
                        Genre.builder().id(2L).name("Драма").build()
                ))
                .build();
    }

    @Test
    void shouldCreateAndFindFilm() {
        Film createdFilm = filmDbStorage.createFilm(testFilm);

        assertThat(createdFilm.getId()).isNotNull();

        Optional<Film> foundFilm = filmDbStorage.getFilmById(createdFilm.getId());
        assertThat(foundFilm).isPresent();
        assertThat(foundFilm.get().getName()).isEqualTo("Test Film");
        assertThat(foundFilm.get().getMpa().getName()).isEqualTo("G");
        assertThat(foundFilm.get().getGenres()).hasSize(2);
    }

    @Test
    void shouldUpdateFilm() {
        Film createdFilm = filmDbStorage.createFilm(testFilm);

        Film updatedFilm = Film.builder()
                .id(createdFilm.getId())
                .name("Updated Film")
                .description("Updated Description")
                .releaseDate(LocalDate.of(2021, 1, 1))
                .duration(150)
                .mpa(Mpa.builder().id(2L).name("PG").build())
                .genres(List.of(Genre.builder().id(3L).name("Мультфильм").build()))
                .build();

        Film result = filmDbStorage.updateFilm(updatedFilm);

        assertThat(result.getName()).isEqualTo("Updated Film");
        assertThat(result.getMpa().getName()).isEqualTo("PG");
        assertThat(result.getGenres()).hasSize(1);
        assertThat(result.getGenres().getFirst().getName()).isEqualTo("Мультфильм");
    }

    @Test
    void shouldFindAllFilms() {
        Film film1 = filmDbStorage.createFilm(testFilm);

        Film film2;
        film2 = Film.builder()
                .name("Another Film")
                .description("Another Description")
                .releaseDate(LocalDate.of(2021, 1, 1))
                .duration(90)
                .mpa(Mpa.builder().id(3L).name("PG-13").build())
                .genres(List.of(Genre.builder().id(4L).name("Триллер").build()))
                .build();
        filmDbStorage.createFilm(film2);

        List<Film> films = filmDbStorage.getAllFilms();

        assertThat(films).hasSize(2);
        assertThat(films).extracting(Film::getName)
                .contains("Test Film", "Another Film");
    }
}