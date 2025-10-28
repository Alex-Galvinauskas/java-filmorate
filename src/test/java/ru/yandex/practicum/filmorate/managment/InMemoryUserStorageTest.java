package ru.yandex.practicum.filmorate.managment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserStorageTest {

    private InMemoryUserStorage userStorage;

    @BeforeEach
    void setUp() {
        userStorage = new InMemoryUserStorage(1L);
    }

    @Test
    @DisplayName("Создание пользователя с валидными данными присваивает ID и сохраняет пользователя")
    void createUser_ValidUser_AssignsIdAndStoresUser() {
        User user = createTestUser();

        User result = userStorage.createUser(user);

        assertNotNull(result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertTrue(userStorage.existsById(result.getId()));
    }

    private User createTestUser() {
        return User.builder()
                .email("test@example.com")
                .login("testlogin")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    @DisplayName("Получение всех пользователей возвращает всех сохраненных пользователей")
    void getAllUsers_ReturnsAllStoredUsers() {
        User user1 = userStorage.createUser(createTestUser());
        User user2 = userStorage.createUser(createTestUser());

        List<User> result = userStorage.getAllUsers();

        assertEquals(2, result.size());
        assertTrue(result.contains(user1));
        assertTrue(result.contains(user2));
    }

    @Test
    @DisplayName("Получение пользователя по существующему ID возвращает пользователя")
    void getUserById_ExistingId_ReturnsUser() {
        User user = userStorage.createUser(createTestUser());
        Long userId = user.getId();

        Optional<User> result = userStorage.getUserById(userId);

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getId());
    }

    @Test
    @DisplayName("Получение пользователя по несуществующему ID возвращает пустой Optional")
    void getUserById_NonExistingId_ReturnsEmpty() {
        Optional<User> result = userStorage.getUserById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Обновление существующего пользователя обновляет его данные")
    void updateUser_ExistingUser_UpdatesUser() {
        User user = userStorage.createUser(createTestUser());
        User updatedUser = User.builder()
                .id(user.getId())
                .email("updated@example.com")
                .login("updatedlogin")
                .name("Updated User")
                .birthday(LocalDate.of(1991, 1, 1))
                .build();

        User result = userStorage.updateUser(updatedUser);

        assertEquals("updated@example.com", result.getEmail());
        assertEquals("updatedlogin", result.getLogin());
        assertEquals("Updated User", result.getName());
    }

    @Test
    @DisplayName("Обновление несуществующего пользователя выбрасывает NotFoundException")
    void updateUser_NonExistingUser_ThrowsNotFoundException() {
        User user = createTestUser();
        user.setId(999L);

        assertThrows(NotFoundException.class, () -> userStorage.updateUser(user));
    }

    @Test
    @DisplayName("Проверка существования пользователя по email для существующего email возвращает true")
    void existsByEmail_ExistingEmail_ReturnsTrue() {
        User user = userStorage.createUser(createTestUser());

        boolean result = userStorage.existsByEmail("test@example.com");

        assertTrue(result);
    }

    @Test
    @DisplayName("Проверка существования пользователя по логину для существующего логина возвращает true")
    void existsByLogin_ExistingLogin_ReturnsTrue() {
        User user = userStorage.createUser(createTestUser());

        boolean result = userStorage.existsByLogin("testlogin");

        assertTrue(result);
    }
}