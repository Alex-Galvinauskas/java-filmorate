package ru.yandex.practicum.filmorate.managment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты хранилища пользователей")
class InMemoryUserStorageTest {

    private InMemoryUserStorage userStorage;

    @BeforeEach
    void setUp() {
        userStorage = new InMemoryUserStorage(1L);
    }

    private User createTestUser() {
        return User.builder()
                .email("test@example.com")
                .login("test-login")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Nested
    @DisplayName("Тесты создания пользователей")
    class CreateUserTests {

        @Test
        @DisplayName("Создание пользователя с валидными данными присваивает ID и сохраняет пользователя")
        void createUser_ValidUser_AssignsIdAndStoresUserTest() {
            User user = createTestUser();

            User result = userStorage.createUser(user);

            assertNotNull(result.getId());
            assertEquals("test@example.com", result.getEmail());
            assertTrue(userStorage.existsById(result.getId()));
        }

        @Test
        @DisplayName("Создание нескольких пользователей присваивает уникальные ID")
        void createUser_MultipleUsers_AssignsUniqueIdsTest() {
            User user1 = userStorage.createUser(createTestUser());
            User user2 = userStorage.createUser(createTestUser());

            assertNotNull(user1.getId());
            assertNotNull(user2.getId());
            assertNotEquals(user1.getId(), user2.getId());
        }

        @Test
        @DisplayName("Создание пользователя с установленным ID перезаписывает ID")
        void createUser_WithPresetId_OverwritesIdTest() {
            User user = createTestUser();
            user.setId(999L);

            User result = userStorage.createUser(user);

            assertNotEquals(999L, result.getId());
            assertTrue(userStorage.existsById(result.getId()));
        }
    }

    @Nested
    @DisplayName("Тесты получения пользователей")
    class GetUserTests {

        @Test
        @DisplayName("Получение всех пользователей возвращает всех сохраненных пользователей")
        void getAllUsers_ReturnsAllStoredUsersTest() {
            User user1 = userStorage.createUser(createTestUser());
            User user2 = userStorage.createUser(createTestUser());

            List<User> result = userStorage.getAllUsers();

            assertEquals(2, result.size());
            assertTrue(result.contains(user1));
            assertTrue(result.contains(user2));
        }

        @Test
        @DisplayName("Получение всех пользователей при пустом хранилище возвращает пустой список")
        void getAllUsers_EmptyStorage_ReturnsEmptyListTest() {
            List<User> result = userStorage.getAllUsers();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Получение пользователя по существующему ID возвращает пользователя")
        void getUserById_ExistingId_ReturnsUserTest() {
            User user = userStorage.createUser(createTestUser());
            Long userId = user.getId();

            Optional<User> result = userStorage.getUserById(userId);

            assertTrue(result.isPresent());
            assertEquals(userId, result.get().getId());
        }

        @Test
        @DisplayName("Получение пользователя по несуществующему ID возвращает пустой Optional")
        void getUserById_NonExistingId_ReturnsEmptyTest() {
            Optional<User> result = userStorage.getUserById(999L);

            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("Тесты обновления пользователей")
    class UpdateUserTests {

        @Test
        @DisplayName("Обновление существующего пользователя обновляет его данные")
        void updateUser_ExistingUser_UpdatesUserTest() {
            User user = userStorage.createUser(createTestUser());
            User updatedUser = User.builder()
                    .id(user.getId())
                    .email("updated@example.com")
                    .login("updated-login")
                    .name("Updated User")
                    .birthday(LocalDate.of(1991, 1, 1))
                    .build();

            User result = userStorage.updateUser(updatedUser);

            assertEquals("updated@example.com", result.getEmail());
            assertEquals("updated-login", result.getLogin());
            assertEquals("Updated User", result.getName());
        }

        @Test
        @DisplayName("Обновление несуществующего пользователя выбрасывает NotFoundException")
        void updateUser_NonExistingUser_ThrowsNotFoundExceptionTest() {
            User user = createTestUser();
            user.setId(999L);

            assertThrows(NotFoundException.class, () -> userStorage.updateUser(user));
        }

        @Test
        @DisplayName("Обновление пользователя сохраняет связь в хранилище")
        void updateUser_ExistingUser_MaintainsStorageConsistencyTest() {
            User user = userStorage.createUser(createTestUser());
            User updatedUser = User.builder()
                    .id(user.getId())
                    .email("updated@example.com")
                    .login("updated-login")
                    .name("Updated User")
                    .birthday(LocalDate.of(1991, 1, 1))
                    .build();

            userStorage.updateUser(updatedUser);
            Optional<User> retrievedUser = userStorage.getUserById(user.getId());

            assertTrue(retrievedUser.isPresent());
            assertEquals("updated@example.com", retrievedUser.get().getEmail());
        }

        @Test
        @DisplayName("Обновление пользователя с изменением email обновляет индекс")
        void updateUser_ChangeEmail_UpdatesEmailIndexTest() {
            User user = userStorage.createUser(createTestUser());
            User updatedUser = User.builder()
                    .id(user.getId())
                    .email("new@example.com")
                    .login(user.getLogin())
                    .name(user.getName())
                    .birthday(user.getBirthday())
                    .build();

            userStorage.updateUser(updatedUser);

            assertFalse(userStorage.existsByEmail("test@example.com"));
            assertTrue(userStorage.existsByEmail("new@example.com"));
        }

        @Test
        @DisplayName("Обновление пользователя с изменением логина обновляет индекс")
        void updateUser_ChangeLogin_UpdatesLoginIndexTest() {
            User user = userStorage.createUser(createTestUser());
            User updatedUser = User.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .login("new-login")
                    .name(user.getName())
                    .birthday(user.getBirthday())
                    .build();

            userStorage.updateUser(updatedUser);

            assertFalse(userStorage.existsByLogin("test-login"));
            assertTrue(userStorage.existsByLogin("new-login"));
        }
    }

    @Nested
    @DisplayName("Тесты проверки существования пользователей")
    class ExistenceTests {

        @Test
        @DisplayName("Проверка существования пользователя по ID для существующего пользователя возвращает true")
        void existsById_ExistingUser_ReturnsTrueTest() {
            User user = userStorage.createUser(createTestUser());

            boolean result = userStorage.existsById(user.getId());

            assertTrue(result);
        }

        @Test
        @DisplayName("Проверка существования пользователя по ID для несуществующего пользователя возвращает false")
        void existsById_NonExistingUser_ReturnsFalseTest() {
            boolean result = userStorage.existsById(999L);

            assertFalse(result);
        }

        @Test
        @DisplayName("Проверка существования пользователя по email для существующего email возвращает true")
        void existsByEmail_ExistingEmail_ReturnsTrueTest() {
            User user = userStorage.createUser(createTestUser());

            boolean result = userStorage.existsByEmail("test@example.com");

            assertTrue(result);
        }

        @Test
        @DisplayName("Проверка существования пользователя по email для несуществующего email возвращает false")
        void existsByEmail_NonExistingEmail_ReturnsFalseTest() {
            boolean result = userStorage.existsByEmail("nonexisting@example.com");

            assertFalse(result);
        }

        @Test
        @DisplayName("Проверка существования пользователя по логину для существующего логина возвращает true")
        void existsByLogin_ExistingLogin_ReturnsTrueTest() {
            User user = userStorage.createUser(createTestUser());

            boolean result = userStorage.existsByLogin("test-login");

            assertTrue(result);
        }

        @Test
        @DisplayName("Проверка существования пользователя по логину для несуществующего логина возвращает false")
        void existsByLogin_NonExistingLogin_ReturnsFalseTest() {
            boolean result = userStorage.existsByLogin("nonexisting-login");

            assertFalse(result);
        }

        @Test
        @DisplayName("Проверка существования пользователя по email регистронезависима")
        void existsByEmail_CaseSensitive_ReturnsFalseForDifferentCaseTest() {
            User user = userStorage.createUser(createTestUser());

            boolean result = userStorage.existsByEmail("TEST@example.com");

            assertTrue(result);
        }

        @Test
        @DisplayName("Проверка существования пользователя по логину регистронезависима")
        void existsByLogin_CaseSensitive_ReturnsFalseForDifferentCaseTest() {
            User user = userStorage.createUser(createTestUser());

            boolean result = userStorage.existsByLogin("TEST-LOGIN");

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Тесты поиска пользователей по email и логину")
    class FindUserTests {

        @Test
        @DisplayName("Поиск пользователя по существующему email возвращает пользователя")
        void getUserByEmail_ExistingEmail_ReturnsUserTest() {
            User user = userStorage.createUser(createTestUser());

            Optional<User> result = userStorage.getUserByEmail("test@example.com");

            assertTrue(result.isPresent());
            assertEquals(user.getId(), result.get().getId());
        }

        @Test
        @DisplayName("Поиск пользователя по несуществующему email возвращает пустой Optional")
        void getUserByEmail_NonExistingEmail_ReturnsEmptyTest() {
            Optional<User> result = userStorage.getUserByEmail("nonexisting@example.com");

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Поиск пользователя по существующему логину возвращает пользователя")
        void getUserByLogin_ExistingLogin_ReturnsUserTest() {
            User user = userStorage.createUser(createTestUser());

            Optional<User> result = userStorage.getUserByLogin("test-login");

            assertTrue(result.isPresent());
            assertEquals(user.getId(), result.get().getId());
        }

        @Test
        @DisplayName("Поиск пользователя по несуществующему логину возвращает пустой Optional")
        void getUserByLogin_NonExistingLogin_ReturnsEmptyTest() {
            Optional<User> result = userStorage.getUserByLogin("nonexisting-login");

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Поиск пользователя по email регистронезависим")
        void getUserByEmail_CaseSensitive_ReturnsEmptyForDifferentCaseTest() {
            userStorage.createUser(createTestUser());

            Optional<User> result = userStorage.getUserByEmail("TEST@example.com");

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("Поиск пользователя по логину регистронезависим")
        void getUserByLogin_CaseSensitive_ReturnsEmptyForDifferentCaseTest() {
            userStorage.createUser(createTestUser());

            Optional<User> result = userStorage.getUserByLogin("TEST-LOGIN");

            assertTrue(result.isPresent());
        }
    }
}