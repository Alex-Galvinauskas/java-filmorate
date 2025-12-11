// RecommendationIntegrationTest.java
package ru.yandex.practicum.filmorate.integrationDb;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.managment.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.managment.db.GenreDbStorage;
import ru.yandex.practicum.filmorate.managment.db.MpaDbStorage;
import ru.yandex.practicum.filmorate.managment.db.UserDbStorage;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, UserDbStorage.class, GenreDbStorage.class, MpaDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class RecommendationIntegrationTest {

    private final FilmDbStorage filmDbStorage;
    private final UserDbStorage userDbStorage;

    @BeforeEach
    void clearDatabase() {
        /**
         * Очищаем таблицы
          */
        filmDbStorage.getAllFilms().forEach(film ->
                filmDbStorage.removeLike(film.getId(), 1L));
    }

    @Test
    void shouldGetLikesByUsers() {
        /**
         * Создаем пользователей
          */
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

        /**
         * Создаем фильмы
          */
        Film film1 = Film.builder()
                .name("Film One")
                .description("Description One")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1L).build())
                .build();

        Film film2 = Film.builder()
                .name("Film Two")
                .description("Description Two")
                .releaseDate(LocalDate.of(2021, 1, 1))
                .duration(150)
                .mpa(Mpa.builder().id(2L).build())
                .build();

        Film createdFilm1 = filmDbStorage.createFilm(film1);
        Film createdFilm2 = filmDbStorage.createFilm(film2);

        /**
         * Добавляем лайки
          */
        filmDbStorage.addLike(createdFilm1.getId(), createdUser1.getId());
        filmDbStorage.addLike(createdFilm2.getId(), createdUser1.getId());
        filmDbStorage.addLike(createdFilm1.getId(), createdUser2.getId());

        /**
         * Получаем лайки всех пользователей
          */
        Map<Long, Set<Long>> likesByUser = filmDbStorage.getLikesByUsers();

        assertThat(likesByUser).hasSize(2);
        assertThat(likesByUser.get(createdUser1.getId())).containsExactlyInAnyOrder(
                createdFilm1.getId(), createdFilm2.getId());
        assertThat(likesByUser.get(createdUser2.getId())).containsExactly(createdFilm1.getId());
    }

    @Test
    void shouldGetFilmsByIds() {
        /**
         * Создаем фильмы
          */
        Film film1 = Film.builder()
                .name("Film One")
                .description("Description One")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1L).build())
                .build();

        Film film2 = Film.builder()
                .name("Film Two")
                .description("Description Two")
                .releaseDate(LocalDate.of(2021, 1, 1))
                .duration(150)
                .mpa(Mpa.builder().id(2L).build())
                .build();

        Film createdFilm1 = filmDbStorage.createFilm(film1);
        Film createdFilm2 = filmDbStorage.createFilm(film2);

        /**
         * Получаем фильмы по ID
          */
        Set<Long> filmIds = Set.of(createdFilm1.getId(), createdFilm2.getId());
        var films = filmDbStorage.getFilmsByIds(filmIds);

        assertThat(films).hasSize(2);
        assertThat(films).extracting(Film::getName)
                .contains("Film One", "Film Two");
    }
}