package ru.yandex.practicum.filmorate.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                .login("validlogin")
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
                .login("validlogin")
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
                .anyMatch(v -> v.getMessage().contains("Логин не может быть пустым")));
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
                .anyMatch(v -> v.getMessage().contains("Логин должен быть от 4 до 20 символов")));
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
                .anyMatch(v -> v.getMessage().contains("Логин может содержать только буквы")));
    }

    @Test
    @DisplayName("Создание пользователя с датой рождения в будущем вызывает нарушение валидации")
    void createUser_FutureBirthday_Violation() {
        User user = User.builder()
                .email("valid@example.com")
                .login("validlogin")
                .name("Valid User")
                .birthday(LocalDate.now().plusDays(1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Дата рождения не может быть в будущем")));
    }
}