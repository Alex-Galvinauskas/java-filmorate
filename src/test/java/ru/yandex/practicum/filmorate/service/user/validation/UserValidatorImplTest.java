package ru.yandex.practicum.filmorate.service.user.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.UserStorage;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты построения ошибок и валидации для сервиса UserServiceImpl")
class UserValidatorImplTest {

    @Mock
    private UserStorage userStorage;

    @InjectMocks
    private UserValidatorImpl userValidator;

    private User createTestUser() {
        return User.builder()
                .id(1L)
                .email("test@example.com")
                .login("test-login")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Nested
    @DisplayName("Тесты валидации для создания пользователя")
    class ValidateForCreateTests {

        @Test
        @DisplayName("Валидация для создания - email уже существует")
        void validateForCreate_DuplicateEmail_ThrowsDuplicateExceptionTest() {
            User user = createTestUser();

            when(userStorage.existsByEmail("test@example.com")).thenReturn(true);

            DuplicateException exception = assertThrows(DuplicateException.class,
                    () -> userValidator.validateForCreate(user));

            assertTrue(exception.getMessage()
                    .contains("Пользователь с таким email test@example.com уже существует"));
            verify(userStorage, never()).existsByLogin(anyString());
        }

        @Test
        @DisplayName("Валидация для создания - логин уже существует")
        void validateForCreate_DuplicateLogin_ThrowsDuplicateExceptionTest() {
            User user = createTestUser();

            when(userStorage.existsByEmail("test@example.com")).thenReturn(false);
            when(userStorage.existsByLogin("test-login")).thenReturn(true);

            DuplicateException exception = assertThrows(DuplicateException.class,
                    () -> userValidator.validateForCreate(user));

            assertTrue(exception.getMessage()
                    .contains("Пользователь с таким логином test-login уже существует"));
        }

        @Test
        @DisplayName("Валидация для создания - валидные данные")
        void validateForCreate_ValidData_NoExceptionTest() {
            User user = createTestUser();

            when(userStorage.existsByEmail("test@example.com")).thenReturn(false);
            when(userStorage.existsByLogin("test-login")).thenReturn(false);

            assertDoesNotThrow(() -> userValidator.validateForCreate(user));

            verify(userStorage, times(1))
                    .existsByEmail("test@example.com");
            verify(userStorage, times(1)).existsByLogin("test-login");
        }
    }

    @Nested
    @DisplayName("Тесты отдельных методов валидации для создания")
    class IndividualCreateValidationTests {

        @Test
        @DisplayName("Валидация уникальности email для создания - дубликат")
        void validateUniqueEmailForCreate_Duplicate_ThrowsExceptionTest() {
            when(userStorage.existsByEmail("test@example.com")).thenReturn(true);

            DuplicateException exception = assertThrows(DuplicateException.class,
                    () -> userValidator.validateUniqueEmailForCreate("test@example.com"));

            assertTrue(exception.getMessage().contains("test@example.com"));
        }

        @Test
        @DisplayName("Валидация уникальности email для создания - уникальный email")
        void validateUniqueEmailForCreate_Unique_NoExceptionTest() {
            when(userStorage.existsByEmail("test@example.com")).thenReturn(false);

            assertDoesNotThrow(() -> userValidator.validateUniqueEmailForCreate("test@example.com"));
        }

        @Test
        @DisplayName("Валидация уникальности логина для создания - дубликат")
        void validateUniqueLoginForCreate_Duplicate_ThrowsExceptionTest() {
            when(userStorage.existsByLogin("test-login")).thenReturn(true);

            DuplicateException exception = assertThrows(DuplicateException.class,
                    () -> userValidator.validateUniqueLoginForCreate("test-login"));

            assertTrue(exception.getMessage().contains("test-login"));
        }

        @Test
        @DisplayName("Валидация уникальности логина для создания - уникальный логин")
        void validateUniqueLoginForCreate_Unique_NoExceptionTest() {
            when(userStorage.existsByLogin("test-login")).thenReturn(false);

            assertDoesNotThrow(() -> userValidator
                    .validateUniqueLoginForCreate("test-login"));
        }
    }

    @Nested
    @DisplayName("Тесты валидации существования пользователя")
    class ValidateUserExistTests {

        @Test
        @DisplayName("Валидация существования пользователя - пользователь существует")
        void validateUserExist_UserExists_ReturnsUserTest() {
            User expectedUser = createTestUser();

            when(userStorage.getUserById(1L)).thenReturn(Optional.of(expectedUser));

            User result = userValidator.validateUserExist(1L);

            assertNotNull(result);
            assertEquals(expectedUser, result);
        }

        @Test
        @DisplayName("Валидация существования пользователя - пользователь не существует")
        void validateUserExist_UserNotExists_ThrowsNotFoundExceptionTest() {
            when(userStorage.getUserById(999L)).thenReturn(Optional.empty());

            NotFoundException exception = assertThrows(NotFoundException.class,
                    () -> userValidator.validateUserExist(999L));

            assertTrue(exception.getMessage().contains("Пользователь с id 999 не найден"));
        }
    }

    @Nested
    @DisplayName("Тесты валидации для обновления пользователя")
    class ValidateForUpdateTests {

        @Test
        @DisplayName("Валидация для обновления - email изменен на существующий")
        void validateForUpdate_EmailChangedToExisting_ThrowsDuplicateExceptionTest() {
            User existingUser = createTestUser();
            User updatedUser = createTestUser();
            updatedUser.setEmail("new@example.com");

            when(userStorage.getUserById(1L)).thenReturn(Optional.of(existingUser));
            when(userStorage.existsByEmail("new@example.com")).thenReturn(true);

            DuplicateException exception = assertThrows(DuplicateException.class,
                    () -> userValidator.validateForUpdate(updatedUser));

            assertTrue(exception.getMessage()
                    .contains("Пользователь с таким email new@example.com уже существует"));
        }

        @Test
        @DisplayName("Валидация для обновления - логин изменен на существующий")
        void validateForUpdate_LoginChangedToExisting_ThrowsDuplicateExceptionTest() {
            User existingUser = createTestUser();
            User updatedUser = createTestUser();
            updatedUser.setLogin("new-login");

            when(userStorage.getUserById(1L)).thenReturn(Optional.of(existingUser));
            when(userStorage.existsByLogin("new-login")).thenReturn(true);

            DuplicateException exception = assertThrows(DuplicateException.class,
                    () -> userValidator.validateForUpdate(updatedUser));

            assertTrue(exception.getMessage()
                    .contains("Пользователь с таким логином new-login уже существует"));
        }

        @Test
        @DisplayName("Валидация для обновления - валидные данные")
        void validateForUpdate_ValidData_NoExceptionTest() {
            User existingUser = createTestUser();
            User updatedUser = createTestUser();
            updatedUser.setName("Updated Name");

            when(userStorage.getUserById(1L)).thenReturn(Optional.of(existingUser));

            assertDoesNotThrow(() -> userValidator.validateForUpdate(updatedUser));
        }

        @Test
        @DisplayName("Валидация для обновления - пользователь не существует")
        void validateForUpdate_UserNotExists_ThrowsNotFoundExceptionTest() {
            User updatedUser = createTestUser();

            when(userStorage.getUserById(1L)).thenReturn(Optional.empty());

            NotFoundException exception = assertThrows(NotFoundException.class,
                    () -> userValidator.validateForUpdate(updatedUser));

            assertTrue(exception.getMessage().contains("Пользователь с id 1 не найден"));
        }

        @Test
        @DisplayName("Валидация для обновления - email не изменен, дубликат не проверяется")
        void validateForUpdate_EmailNotChanged_NoDuplicateCheckTest() {
            User existingUser = createTestUser();
            User updatedUser = createTestUser();
            updatedUser.setName("Updated Name");

            when(userStorage.getUserById(1L)).thenReturn(Optional.of(existingUser));

            assertDoesNotThrow(() -> userValidator.validateForUpdate(updatedUser));

            verify(userStorage, never()).existsByEmail("test@example.com");
        }

        @Test
        @DisplayName("Валидация для обновления - логин не изменен, дубликат не проверяется")
        void validateForUpdate_LoginNotChanged_NoDuplicateCheckTest() {
            User existingUser = createTestUser();
            User updatedUser = createTestUser();
            updatedUser.setEmail("new@example.com");

            when(userStorage.getUserById(1L)).thenReturn(Optional.of(existingUser));
            when(userStorage.existsByEmail("new@example.com")).thenReturn(false);

            assertDoesNotThrow(() -> userValidator.validateForUpdate(updatedUser));

            verify(userStorage, never()).existsByLogin("test-login");
        }
    }

    @Nested
    @DisplayName("Тесты отдельных методов валидации для обновления")
    class IndividualUpdateValidationTests {

        @Test
        @DisplayName("Валидация уникальности email для обновления - email изменен и дублируется")
        void validateUniqueEmailForUpdate_EmailChangedAndDuplicate_ThrowsExceptionTest() {
            User existingUser = createTestUser();
            User updatedUser = createTestUser();
            updatedUser.setEmail("new@example.com");

            when(userStorage.existsByEmail("new@example.com")).thenReturn(true);

            DuplicateException exception = assertThrows(DuplicateException.class,
                    () -> userValidator.validateUniqueEmailForUpdate(updatedUser, existingUser));

            assertTrue(exception.getMessage().contains("new@example.com"));
        }

        @Test
        @DisplayName("Валидация уникальности email для обновления - email не изменен")
        void validateUniqueEmailForUpdate_EmailNotChanged_NoExceptionTest() {
            User existingUser = createTestUser();
            User updatedUser = createTestUser();

            assertDoesNotThrow(() ->
                    userValidator.validateUniqueEmailForUpdate(updatedUser, existingUser));

            verify(userStorage, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("Валидация уникальности логина для обновления - логин изменен и дублируется")
        void validateUniqueLoginForUpdate_LoginChangedAndDuplicate_ThrowsExceptionTest() {
            User existingUser = createTestUser();
            User updatedUser = createTestUser();
            updatedUser.setLogin("new-login");

            when(userStorage.existsByLogin("new-login")).thenReturn(true);

            DuplicateException exception = assertThrows(DuplicateException.class,
                    () -> userValidator.validateUniqueLoginForUpdate(updatedUser, existingUser));

            assertTrue(exception.getMessage().contains("new-login"));
        }

        @Test
        @DisplayName("Валидация уникальности логина для обновления - логин не изменен")
        void validateUniqueLoginForUpdate_LoginNotChanged_NoExceptionTest() {
            User existingUser = createTestUser();
            User updatedUser = createTestUser();

            assertDoesNotThrow(() ->
                    userValidator.validateUniqueLoginForUpdate(updatedUser, existingUser));

            verify(userStorage, never()).existsByLogin(anyString());
        }
    }
}