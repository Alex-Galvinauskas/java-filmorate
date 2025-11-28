package ru.yandex.practicum.filmorate.integrationDb;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.managment.db.MpaDbStorage;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(MpaDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class MpaDbStorageIntegrationTest {

    private final MpaDbStorage mpaDbStorage;

    @Test
    void shouldGetAllMpa() {
        List<Mpa> mpaList = mpaDbStorage.getAllMpa();

        assertThat(mpaList).hasSize(5);
        assertThat(mpaList).extracting(Mpa::getName)
                .contains("G", "PG", "PG-13", "R", "NC-17");
    }

    @Test
    void shouldGetMpaById() {
        Optional<Mpa> mpa = mpaDbStorage.getMpaById(1);

        assertThat(mpa).isPresent();
        assertThat(mpa.get().getName()).isEqualTo("G");
    }

    @Test
    void shouldReturnEmptyForNonExistingMpa() {
        Optional<Mpa> mpa = mpaDbStorage.getMpaById(999);

        assertThat(mpa).isEmpty();
    }
}