package ru.yandex.practicum.filmorate.integrationDb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.managment.db.UserDbStorage;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
class RecommendationIntegrationTest {

    @Autowired
    private FilmDbStorage filmDbStorage;
    
    @Autowired
    private UserDbStorage userDbStorage;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.update("DELETE FROM likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM film_directors");
        jdbcTemplate.update("DELETE FROM friendships");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM genres");
        jdbcTemplate.update("DELETE FROM mpa_ratings");
        jdbcTemplate.update("DELETE FROM directors");
        
        jdbcTemplate.update("INSERT INTO mpa_ratings (id, name, description) VALUES " +
                "(1, 'G', 'General Audiences'), " +
                "(2, 'PG', 'Parental Guidance Suggested'), " +
                "(3, 'PG-13', 'Parents Strongly Cautioned'), " +
                "(4, 'R', 'Restricted'), " +
                "(5, 'NC-17', 'Adults Only') " +
                "ON CONFLICT DO NOTHING");
        
        jdbcTemplate.update("INSERT INTO genres (id, name) VALUES " +
                "(1, 'Комедия'), " +
                "(2, 'Драма'), " +
                "(3, 'Мультфильм'), " +
                "(4, 'Триллер'), " +
                "(5, 'Документальный'), " +
                "(6, 'Боевик') " +
                "ON CONFLICT DO NOTHING");
    }

    @Test
    void shouldGetLikesByUsers() {
        User user1 = User.builder()
                .email("user1@test.com")
                .login("user1")
                .name("User One")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User user2 = User.builder()
                .email("user2@test.com")
                .login("user2")
                .name("User Two")
                .birthday(LocalDate.of(1991, 1, 1))
                .build();

        User createdUser1 = userDbStorage.createUser(user1);
        User createdUser2 = userDbStorage.createUser(user2);

        Film film1 = Film.builder()
                .name("Film One")
                .description("Description One")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1L).name("G").build())
                .build();

        Film film2 = Film.builder()
                .name("Film Two")
                .description("Description Two")
                .releaseDate(LocalDate.of(2021, 1, 1))
                .duration(150)
                .mpa(Mpa.builder().id(2L).name("PG").build())
                .build();

        Film createdFilm1 = filmDbStorage.createFilm(film1);
        Film createdFilm2 = filmDbStorage.createFilm(film2);

        filmDbStorage.addLike(createdFilm1.getId(), createdUser1.getId());
        filmDbStorage.addLike(createdFilm2.getId(), createdUser1.getId());
        filmDbStorage.addLike(createdFilm1.getId(), createdUser2.getId());

        Map<Long, Set<Long>> likesByUser = filmDbStorage.getLikesByUsers();

        assertThat(likesByUser).hasSize(2);
        assertThat(likesByUser.get(createdUser1.getId())).containsExactlyInAnyOrder(
                createdFilm1.getId(), createdFilm2.getId());
        assertThat(likesByUser.get(createdUser2.getId())).containsExactly(createdFilm1.getId());
    }

    @Test
    void shouldGetFilmsByIds() {
        Film film1 = Film.builder()
                .name("Film One")
                .description("Description One")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1L).name("G").build())
                .build();

        Film film2 = Film.builder()
                .name("Film Two")
                .description("Description Two")
                .releaseDate(LocalDate.of(2021, 1, 1))
                .duration(150)
                .mpa(Mpa.builder().id(2L).name("PG").build())
                .build();

        Film createdFilm1 = filmDbStorage.createFilm(film1);
        Film createdFilm2 = filmDbStorage.createFilm(film2);

        Set<Long> filmIds = Set.of(createdFilm1.getId(), createdFilm2.getId());
        List<Film> films = filmDbStorage.getFilmsByIds(filmIds);

        assertThat(films).hasSize(2);
        assertThat(films).extracting(Film::getName)
                .contains("Film One", "Film Two");
    }
}
