package ru.yandex.practicum.filmorate.integrationDb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.DirectorDbStorage;
import ru.yandex.practicum.filmorate.model.Director;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DirectorDbStorageTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DirectorDbStorage directorDbStorage;

    private Director testDirector;

    @BeforeEach
    void setUp() {
        testDirector = Director.builder()
                .id(1L)
                .name("Christopher Nolan")
                .build();
    }

    @Test
    void create_shouldReturnDirectorWithGeneratedId() {
        Director directorToSave = Director.builder()
                .name("New Director")
                .build();

        Long expectedId = 1L;

        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);

            Map<String, Object> keys = new HashMap<>();
            keys.put("id", expectedId);

            List<Map<String, Object>> keyList = new ArrayList<>();
            keyList.add(keys);

            if (keyHolder instanceof GeneratedKeyHolder) {
                ((GeneratedKeyHolder) keyHolder).getKeyList().addAll(keyList);
            }

            return 1;
        }).when(jdbcTemplate).update(
                any(PreparedStatementCreator.class),
                any(KeyHolder.class)
        );

        Director result = directorDbStorage.create(directorToSave);

        assertEquals(expectedId, result.getId());
        assertEquals("New Director", result.getName());
    }

    @Test
    void getById_shouldReturnDirectorWhenExists() {
        when(jdbcTemplate.query(eq("SELECT * FROM directors WHERE id = ?"),
                any(RowMapper.class), eq(1L)))
                .thenReturn(List.of(testDirector));

        Optional<Director> result = Optional.ofNullable(directorDbStorage.getById(1L));

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("Christopher Nolan", result.get().getName());
        verify(jdbcTemplate, times(1))
                .query(eq("SELECT * FROM directors WHERE id = ?"),
                        any(RowMapper.class), eq(1L));
    }


    @Test
    void delete_shouldDeleteDirectorWhenExists() {
        when(jdbcTemplate.update(eq("DELETE FROM directors WHERE id = ?"),
                eq(1L)))
                .thenReturn(1);

        assertDoesNotThrow(() -> directorDbStorage.delete(1L));

        verify(jdbcTemplate, times(1))
                .update(eq("DELETE FROM directors WHERE id = ?"),
                        eq(1L));
    }

    @Test
    void delete_shouldThrowNotFoundExceptionWhenDirectorNotExists() {
        when(jdbcTemplate.update(eq("DELETE FROM directors WHERE id = ?"),
                eq(999L)))
                .thenReturn(0);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> directorDbStorage.delete(999L));

        assertEquals("Режиссер с ID 999 не найден", exception.getMessage());
        verify(jdbcTemplate, times(1))
                .update(eq("DELETE FROM directors WHERE id = ?"), eq(999L));
    }

    @Test
    void getDirectorsByFilmId_shouldReturnDirectorsList() {
        Director director1 = Director.builder()
                .id(1L)
                .name("Director 1")
                .build();
        Director director2 = Director.builder()
                .id(2L)
                .name("Director 2")
                .build();

        when(jdbcTemplate.query(
                anyString(),
                any(RowMapper.class),
                eq(100L)))
                .thenReturn(List.of(director1, director2));

        List<Director> result = directorDbStorage.getDirectorsByFilmId(100L);

        assertEquals(2, result.size());
        assertEquals("Director 1", result.get(0).getName());
        assertEquals("Director 2", result.get(1).getName());

        verify(jdbcTemplate, times(1))
                .query(anyString(), any(RowMapper.class), eq(100L));
    }

    @Test
    void addDirectorToFilm_shouldExecuteUpdate() {
        String existsDirectorSql = "SELECT COUNT(*) FROM directors WHERE id = ?";
        String existsLinkSql = "SELECT COUNT(*) FROM film_directors WHERE film_id = ? AND director_id = ?";
        String insertSql = "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";

        when(jdbcTemplate.queryForObject(eq(existsDirectorSql),
                eq(Integer.class), eq(1L)))
                .thenReturn(1);

        when(jdbcTemplate.queryForObject(eq(existsLinkSql),
                eq(Integer.class), eq(100L), eq(1L)))
                .thenReturn(0);

        when(jdbcTemplate.update(eq(insertSql), eq(100L), eq(1L)))
                .thenReturn(1);

        directorDbStorage.addDirectorToFilm(1L, 100L);

        verify(jdbcTemplate, times(1))
                .queryForObject(eq(existsDirectorSql), eq(Integer.class),
                        eq(1L));

        verify(jdbcTemplate, times(1))
                .queryForObject(eq(existsLinkSql), eq(Integer.class),
                        eq(100L), eq(1L));

        verify(jdbcTemplate, times(1))
                .update(eq(insertSql), eq(100L), eq(1L));
    }

    @Test
    void removeDirectorFromFilm_shouldExecuteUpdate() {
        String expectedSql = "DELETE FROM film_directors WHERE film_id = ?";

        when(jdbcTemplate.update(eq(expectedSql), eq(100L)))
                .thenReturn(1);

        directorDbStorage.removeDirectorFromFilm(100L);

        verify(jdbcTemplate, times(1))
                .update(eq(expectedSql), eq(100L));
    }

    @Test
    void existsById_shouldReturnTrueWhenDirectorExists() {
        String expectedSql = "SELECT COUNT(*) FROM directors WHERE id = ?";

        when(jdbcTemplate.queryForObject(eq(expectedSql),
                eq(Integer.class), eq(1L)))
                .thenReturn(1);

        boolean result = directorDbStorage.existsById(1L);

        assertTrue(result);
        verify(jdbcTemplate, times(1))
                .queryForObject(eq(expectedSql),
                        eq(Integer.class), eq(1L));
    }

    @Test
    void existsById_shouldReturnFalseWhenDirectorNotExists() {
        String expectedSql = "SELECT COUNT(*) FROM directors WHERE id = ?";

        when(jdbcTemplate.queryForObject(eq(expectedSql),
                eq(Integer.class), eq(999L)))
                .thenReturn(0);

        boolean result = directorDbStorage.existsById(999L);

        assertFalse(result);
        verify(jdbcTemplate, times(1))
                .queryForObject(eq(expectedSql),
                        eq(Integer.class), eq(999L));
    }

    @Test
    void existsById_shouldReturnFalseWhenQueryReturnsNull() {
        String expectedSql = "SELECT COUNT(*) FROM directors WHERE id = ?";

        when(jdbcTemplate.queryForObject(eq(expectedSql),
                eq(Integer.class), eq(999L)))
                .thenReturn(null);

        boolean result = directorDbStorage.existsById(999L);

        assertFalse(result);
        verify(jdbcTemplate, times(1))
                .queryForObject(eq(expectedSql),
                        eq(Integer.class), eq(999L));
    }
}