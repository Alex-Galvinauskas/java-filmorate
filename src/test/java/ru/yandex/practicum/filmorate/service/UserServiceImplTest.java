package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.DisplayName;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserStorage userStorage;

    @InjectMocks
    private UserServiceImpl userService;

    private User createTestUser() {
        return User.builder()
                .id(1L)
                .email("test@example.com")
                .login("testlogin")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    @DisplayName("Создание пользователя с валидными данными возвращает созданного пользователя")
    void createUser_ValidUser_ReturnsCreatedUser() {
        User user = createTestUser();
        user.setId(null);

        when(userStorage.existsByEmail(any())).thenReturn(false);
        when(userStorage.existsByLogin(any())).thenReturn(false);
        when(userStorage.createUser(any(User.class))).thenReturn(user);

        User result = userService.createUser(user);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userStorage, times(1)).createUser(any(User.class));
    }

    @Test
    @DisplayName("Создание пользователя с дублирующим email выбрасывает DuplicateException")
    void createUser_DuplicateEmail_ThrowsDuplicateException() {
        User user = createTestUser();
        user.setId(null);

        when(userStorage.existsByEmail(any())).thenReturn(true);

        assertThrows(DuplicateException.class, () -> userService.createUser(user));
        verify(userStorage, never()).createUser(any(User.class));
    }

    @Test
    @DisplayName("Создание пользователя с дублирующим логином выбрасывает DuplicateException")
    void createUser_DuplicateLogin_ThrowsDuplicateException() {
        User user = createTestUser();
        user.setId(null);

        when(userStorage.existsByEmail(any())).thenReturn(false);
        when(userStorage.existsByLogin(any())).thenReturn(true);

        assertThrows(DuplicateException.class, () -> userService.createUser(user));
        verify(userStorage, never()).createUser(any(User.class));
    }

    @Test
    @DisplayName("Создание пользователя с пустым именем устанавливает имя из логина")
    void createUser_EmptyName_SetsNameFromLogin() {
        User user = User.builder()
                .email("test@example.com")
                .login("testlogin")
                .name("")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        when(userStorage.existsByEmail(any())).thenReturn(false);
        when(userStorage.existsByLogin(any())).thenReturn(false);
        when(userStorage.createUser(any(User.class))).thenReturn(user);

        User result = userService.createUser(user);

        assertEquals("testlogin", result.getName());
    }

    @Test
    @DisplayName("Получение всех пользователей возвращает список пользователей")
    void getAllUsers_ReturnsUsersList() {
        User user = createTestUser();
        when(userStorage.getAllUsers()).thenReturn(List.of(user));

        List<User> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
        verify(userStorage, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("Получение пользователя по существующему ID возвращает пользователя")
    void getUserById_ExistingId_ReturnsUser() {
        User user = createTestUser();
        when(userStorage.getUserById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(userStorage, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName("Получение пользователя по несуществующему ID выбрасывает NotFoundException")
    void getUserById_NonExistingId_ThrowsNotFoundException() {
        when(userStorage.getUserById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getUserById(999L));
        verify(userStorage, times(1)).getUserById(999L);
    }

    @Test
    @DisplayName("Обновление валидного пользователя возвращает обновленного пользователя")
    void updateUser_ValidUser_ReturnsUpdatedUser() {
        User existingUser = createTestUser();
        User updatedUser = createTestUser();
        updatedUser.setName("Updated User");

        when(userStorage.getUserById(1L)).thenReturn(Optional.of(existingUser));
        when(userStorage.updateUser(any(User.class))).thenReturn(updatedUser);

        User result = userService.updateUser(updatedUser);

        assertNotNull(result);
        assertEquals("Updated User", result.getName());
        verify(userStorage, times(1)).updateUser(any(User.class));
    }

    @Test
    @DisplayName("Обновление несуществующего пользователя выбрасывает NotFoundException")
    void updateUser_NonExistingUser_ThrowsNotFoundException() {
        User user = createTestUser();
        when(userStorage.getUserById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.updateUser(user));
        verify(userStorage, never()).updateUser(any(User.class));
    }

    @Test
    @DisplayName("Обновление пользователя с дублирующим email выбрасывает DuplicateException")
    void updateUser_DuplicateEmail_ThrowsDuplicateException() {
        User existingUser = createTestUser();
        User updatedUser = createTestUser();
        updatedUser.setEmail("new@example.com");

        when(userStorage.getUserById(1L)).thenReturn(Optional.of(existingUser));
        when(userStorage.existsByEmail(any())).thenReturn(true);

        assertThrows(DuplicateException.class, () -> userService.updateUser(updatedUser));
        verify(userStorage, never()).updateUser(any(User.class));
    }
}