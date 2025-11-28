package ru.yandex.practicum.filmorate.integrationDb;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.managment.db.GenreDbStorage;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(GenreDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class GenreDbStorageIntegrationTest {

    private final GenreDbStorage genreDbStorage;

    @Test
    void shouldGetAllGenres() {
        List<Genre> genres = genreDbStorage.getAllGenres();

        assertThat(genres).hasSize(6);
        assertThat(genres).extracting(Genre::getName)
                .contains("Комедия", "Драма", "Мультфильм", "Триллер", "Документальный", "Боевик");
    }

    @Test
    void shouldGetGenreById() {
        Optional<Genre> genre = genreDbStorage.getGenreById(1);

        assertThat(genre).isPresent();
        assertThat(genre.get().getName()).isEqualTo("Комедия");
    }

    @Test
    void shouldReturnEmptyForNonExistingGenre() {
        Optional<Genre> genre = genreDbStorage.getGenreById(999);

        assertThat(genre).isEmpty();
    }
}