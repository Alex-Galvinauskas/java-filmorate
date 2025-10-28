package ru.yandex.practicum.filmorate.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты валидации модели User")
class UserValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Создание пользователя с валидными данными не вызывает нарушений валидации")
    void createUser_ValidUser_NoViolations() {
        User user = User.builder()
                .email("valid@example.com")
                .login("valid_login")
                .name("Valid User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Создание пользователя с некорректным email вызывает нарушение валидации")
    void createUser_InvalidEmail_Violation() {
        User user = User.builder()
                .email("invalid-email")
                .login("valid-login")
                .name("Valid User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Некорректный email")));
    }

    @Test
    @DisplayName("Создание пользователя с пустым логином вызывает нарушение валидации")
    void createUser_EmptyLogin_Violation() {
        User user = User.builder()
                .email("valid@example.com")
                .login("")
                .name("Valid User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v ->
                        v.getMessage().contains("Логин не может быть пустым")));
    }

    @Test
    @DisplayName("Создание пользователя с слишком коротким логином вызывает нарушение валидации")
    void createUser_TooShortLogin_Violation() {
        User user = User.builder()
                .email("valid@example.com")
                .login("abc")
                .name("Valid User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v ->
                        v.getMessage().contains("Логин должен быть от 4 до 20 символов")));
    }

    @Test
    @DisplayName("Создание пользователя с недопустимыми символами в логине вызывает нарушение валидации")
    void createUser_InvalidLoginCharacters_Violation() {
        User user = User.builder()
                .email("valid@example.com")
                .login("invalid login!")
                .name("Valid User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v ->
                        v.getMessage().contains("Логин может содержать только буквы")));
    }

    @Test
    @DisplayName("Создание пользователя с датой рождения в будущем вызывает нарушение валидации")
    void createUser_FutureBirthday_Violation() {
        User user = User.builder()
                .email("valid@example.com")
                .login("valid-login")
                .name("Valid User")
                .birthday(LocalDate.now().plusDays(1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v ->
                        v.getMessage().contains("Дата рождения не может быть в будущем")));
    }

    @Test
    @DisplayName("Создание пользователя с null email вызывает нарушение валидации")
    void createUser_NullEmail_Violation() {
        User user = User.builder()
                .email(null)
                .login("valid-login")
                .name("Valid User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v ->
                        v.getMessage().contains("Email не может быть пустым")));
    }

    @Test
    @DisplayName("Создание пользователя с null логином вызывает нарушение валидации")
    void createUser_NullLogin_Violation() {
        User user = User.builder()
                .email("valid@example.com")
                .login(null)
                .name("Valid User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v ->
                        v.getMessage().contains("Логин не может быть пустым")));
    }

    @Test
    @DisplayName("Создание пользователя с слишком длинным логином вызывает нарушение валидации")
    void createUser_TooLongLogin_Violation() {
        User user = User.builder()
                .email("valid@example.com")
                .login("thisloginistoolongforthevalidation")
                .name("Valid User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v ->
                        v.getMessage().contains("Логин должен быть от 4 до 20 символов")));
    }

    @Test
    @DisplayName("Создание пользователя с кириллическими символами в логине - валидно")
    void createUser_CyrillicLogin_Valid() {
        User user = User.builder()
                .email("valid@example.com")
                .login("логин")
                .name("Valid User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Создание пользователя с подчеркиванием в логине - валидно")
    void createUser_LoginWithUnderscore_Valid() {
        User user = User.builder()
                .email("valid@example.com")
                .login("user_name")
                .name("Valid User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Создание пользователя с цифрами в логине - валидно")
    void createUser_LoginWithNumbers_Valid() {
        User user = User.builder()
                .email("valid@example.com")
                .login("user123")
                .name("Valid User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Создание пользователя с null датой рождения - валидно")
    void createUser_NullBirthday_Valid() {
        User user = User.builder()
                .email("valid@example.com")
                .login("valid_login")
                .name("Valid User")
                .birthday(null)
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertTrue(violations.isEmpty());
    }

    @Nested
    @DisplayName("Тесты метода getName")
    class GetNameTests {

        @Test
        @DisplayName("getName возвращает login когда name null")
        void getName_NameNull_ReturnsLoginTest() {
            User user = User.builder()
                    .email("test@example.com")
                    .login("test-login")
                    .name(null)
                    .build();

            assertEquals("test-login", user.getName());
        }

        @Test
        @DisplayName("getName возвращает login когда name пустое")
        void getName_NameEmpty_ReturnsLoginTest() {
            User user = User.builder()
                    .email("test@example.com")
                    .login("test-login")
                    .name("")
                    .build();

            assertEquals("test-login", user.getName());
        }

        @Test
        @DisplayName("getName возвращает login когда name состоит из пробелов")
        void getName_NameBlank_ReturnsLoginTest() {
            User user = User.builder()
                    .email("test@example.com")
                    .login("test-login")
                    .name("   ")
                    .build();

            assertEquals("test-login", user.getName());
        }

        @Test
        @DisplayName("getName возвращает name когда name не пустое")
        void getName_NameNotEmpty_ReturnsNameTest() {
            User user = User.builder()
                    .email("test@example.com")
                    .login("test-login")
                    .name("Real Name")
                    .build();

            assertEquals("Real Name", user.getName());
        }
    }

    @Nested
    @DisplayName("Тесты метода copyWithId")
    class UserCopyWithIdTests {

        @Test
        @DisplayName("copyWithId корректно копирует все поля")
        void copyWithId_CopiesAllFieldsTest() {
            User original = User.builder()
                    .id(1L)
                    .email("original@example.com")
                    .login("original-login")
                    .name("Original Name")
                    .birthday(LocalDate.of(1990, 1, 1))
                    .build();

            User copy = User.copyWithId(original, 999L);

            assertEquals(999L, copy.getId());
            assertEquals("original@example.com", copy.getEmail());
            assertEquals("original-login", copy.getLogin());
            assertEquals("Original Name", copy.getName());
            assertEquals(LocalDate.of(1990, 1, 1), copy.getBirthday());
            assertTrue(copy.getFriends().isEmpty());
        }

        @Test
        @DisplayName("copyWithId с null полями корректно обрабатывается")
        void copyWithId_NullFields_HandledCorrectlyTest() {
            User original = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .login("test-login")
                    .name(null)
                    .birthday(null)
                    .build();

            User copy = User.copyWithId(original, 999L);

            assertEquals(999L, copy.getId());
            assertEquals("test@example.com", copy.getEmail());
            assertEquals("test-login", copy.getLogin());
            assertEquals("test-login", copy.getName());
            assertNull(copy.getBirthday());
            assertTrue(copy.getFriends().isEmpty());
        }

        @Test
        @DisplayName("copyWithId с пустым именем корректно обрабатывается")
        void copyWithId_EmptyName_HandledCorrectlyTest() {
            User original = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .login("test-login")
                    .name("")
                    .birthday(null)
                    .build();

            User copy = User.copyWithId(original, 999L);

            assertEquals(999L, copy.getId());
            assertEquals("test@example.com", copy.getEmail());
            assertEquals("test-login", copy.getLogin());
            assertEquals("test-login", copy.getName());
            assertNull(copy.getBirthday());
            assertTrue(copy.getFriends().isEmpty());
        }

        @Test
        @DisplayName("copyWithId с именем из пробелов корректно обрабатывается")
        void copyWithId_BlankName_HandledCorrectlyTest() {
            User original = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .login("test-login")
                    .name("   ")
                    .birthday(null)
                    .build();

            User copy = User.copyWithId(original, 999L);

            assertEquals(999L, copy.getId());
            assertEquals("test@example.com", copy.getEmail());
            assertEquals("test-login", copy.getLogin());
            assertEquals("test-login", copy.getName());
            assertNull(copy.getBirthday());
            assertTrue(copy.getFriends().isEmpty());
        }
    }

    @Nested
    @DisplayName("Тесты equals и hashCode для User")
    class UserEqualsHashCodeTests {

        @Test
        @DisplayName("Пользователи с одинаковыми всеми полями равны")
        void usersWithSameFields_AreEqualTest() {
            User user1 = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .login("login")
                    .name("User Name")
                    .birthday(LocalDate.of(1990, 1, 1))
                    .build();

            User user2 = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .login("login")
                    .name("User Name")
                    .birthday(LocalDate.of(1990, 1, 1))
                    .build();

            assertEquals(user1, user2);
            assertEquals(user1.hashCode(), user2.hashCode());
        }

        @Test
        @DisplayName("Пользователи с разным ID не равны")
        void usersWithDifferentId_AreNotEqualTest() {
            User user1 = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .login("login")
                    .name("User Name")
                    .build();

            User user2 = User.builder()
                    .id(2L)
                    .email("test@example.com")
                    .login("login")
                    .name("User Name")
                    .build();

            assertNotEquals(user1, user2);
        }

        @Test
        @DisplayName("Пользователи с разным email не равны")
        void usersWithDifferentEmail_AreNotEqualTest() {
            User user1 = User.builder()
                    .id(1L)
                    .email("test1@example.com")
                    .login("login")
                    .name("User Name")
                    .build();

            User user2 = User.builder()
                    .id(1L)
                    .email("test2@example.com")
                    .login("login")
                    .name("User Name")
                    .build();

            assertNotEquals(user1, user2);
        }

        @Test
        @DisplayName("Пользователи с разным логином не равны")
        void usersWithDifferentLogin_AreNotEqualTest() {
            User user1 = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .login("login1")
                    .name("User Name")
                    .build();

            User user2 = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .login("login2")
                    .name("User Name")
                    .build();

            assertNotEquals(user1, user2);
        }

        @Test
        @DisplayName("Пользователи с разным именем не равны")
        void usersWithDifferentName_AreNotEqualTest() {
            User user1 = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .login("login")
                    .name("User Name 1")
                    .build();

            User user2 = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .login("login")
                    .name("User Name 2")
                    .build();

            assertNotEquals(user1, user2);
        }

        @Test
        @DisplayName("Пользователи с разной датой рождения не равны")
        void usersWithDifferentBirthday_AreNotEqualTest() {
            User user1 = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .login("login")
                    .name("User Name")
                    .birthday(LocalDate.of(1990, 1, 1))
                    .build();

            User user2 = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .login("login")
                    .name("User Name")
                    .birthday(LocalDate.of(1991, 1, 1))
                    .build();

            assertNotEquals(user1, user2);
        }

        @Test
        @DisplayName("Пользователь не равен null")
        void userEquals_Null_ReturnsFalseTest() {
            User user = User.builder().id(1L).email("test@example.com").login("login").build();
            assertNotEquals(null, user);
        }

        @Test
        @DisplayName("Пользователь с null ID не равен пользователю с установленным ID")
        void userWithNullId_NotEqualToUserWithIdTest() {
            User userWithNullId = User.builder().id(null).email("test@example.com").login("login").build();
            User userWithId = User.builder().id(1L).email("test@example.com").login("login").build();

            assertNotEquals(userWithNullId, userWithId);
            assertNotEquals(userWithId, userWithNullId);
        }

        @Test
        @DisplayName("Два пользователя с null ID не равны")
        void usersWithBothNullId_AreNotEqualTest() {
            User user1 = User.builder().id(null).email("test1@example.com").login("login1").build();
            User user2 = User.builder().id(null).email("test2@example.com").login("login2").build();

            assertNotEquals(user1, user2);
        }
    }
}