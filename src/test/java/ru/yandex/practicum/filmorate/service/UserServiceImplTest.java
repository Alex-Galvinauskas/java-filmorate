package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.UserDbStorage;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.user.UserServiceImpl;
import ru.yandex.practicum.filmorate.service.user.validation.UserValidatorRules;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты сервиса управления пользователями")
class UserServiceImplTest {

    @Mock
    private UserDbStorage userDbStorage;

    @Mock
    private UserValidatorRules userValidator;

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

    @Nested
    @DisplayName("Тесты создания пользователей")
    class CreateUserTests {

        @Test
        @DisplayName("Создание пользователя с валидными данными возвращает созданного пользователя")
        void createUser_ValidUser_ReturnsCreatedUserTest() {
            User user = createTestUser();
            user.setId(null);

            doNothing().when(userValidator).validateForCreate(user);
            when(userDbStorage.createUser(any(User.class))).thenReturn(user);

            User result = userService.createUser(user);

            assertNotNull(result);
            assertEquals("test@example.com", result.getEmail());
            assertEquals("Test User", result.getName());
            verify(userDbStorage, times(1)).createUser(any(User.class));
            verify(userValidator, times(1)).validateForCreate(user);
        }

        @Test
        @DisplayName("Создание пользователя с пустым именем устанавливает имя из логина")
        void createUser_EmptyName_SetsNameFromLoginTest() {
            User user = createTestUser();
            user.setId(null);
            user.setName("  ");

            doNothing().when(userValidator).validateForCreate(user);
            when(userDbStorage.createUser(any(User.class))).thenReturn(user);

            User result = userService.createUser(user);

            assertNotNull(result);
            assertEquals("testlogin", result.getName());
        }

        @Test
        @DisplayName("Создание дублирующего пользователя выбрасывает DuplicateException")
        void createUser_DuplicateUser_ThrowsDuplicateExceptionTest() {
            User user = createTestUser();
            user.setId(null);

            doThrow(new DuplicateException("Пользователь с таким email уже существует"))
                    .when(userValidator).validateForCreate(any(User.class));

            assertThrows(DuplicateException.class, () -> userService.createUser(user));
            verify(userDbStorage, never()).createUser(any(User.class));
        }
    }

    @Nested
    @DisplayName("Тесты получения пользователей")
    class GetUserTests {

        @Test
        @DisplayName("Получение всех пользователей возвращает список пользователей")
        void getAllUsers_ReturnsUsersListTest() {
            User user = createTestUser();
            when(userDbStorage.getAllUsers()).thenReturn(List.of(user));

            List<User> result = userService.getAllUsers();

            assertEquals(1, result.size());
            assertEquals("test@example.com", result.getFirst().getEmail());
            verify(userDbStorage, times(1)).getAllUsers();
        }

        @Test
        @DisplayName("Получение пользователя по существующему ID возвращает пользователя")
        void getUserById_ExistingId_ReturnsUserTest() {
            User user = createTestUser();

            when(userValidator.validateUserExist(1L)).thenReturn(user);

            User result = userService.getUserById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(userValidator, times(1)).validateUserExist(1L);
        }

        @Test
        @DisplayName("Получение пользователя по несуществующему ID выбрасывает NotFoundException")
        void getUserById_NonExistingId_ThrowsNotFoundExceptionTest() {
            when(userValidator.validateUserExist(999L))
                    .thenThrow(new NotFoundException("Пользователь не найден"));

            assertThrows(NotFoundException.class, () -> userService.getUserById(999L));
            verify(userValidator, times(1)).validateUserExist(999L);
        }
    }

    @Nested
    @DisplayName("Тесты обновления пользователей")
    class UpdateUserTests {

        @Test
        @DisplayName("Обновление валидного пользователя возвращает обновленного пользователя")
        void updateUser_ValidUser_ReturnsUpdatedUserTest() {
            User updatedUser = createTestUser();
            updatedUser.setName("Updated User");

            doNothing().when(userValidator).validateForUpdate(updatedUser);
            when(userDbStorage.updateUser(any(User.class))).thenReturn(updatedUser);

            User result = userService.updateUser(updatedUser);

            assertNotNull(result);
            assertEquals("Updated User", result.getName());
            verify(userDbStorage, times(1)).
                    updateUser(any(User.class));
            verify(userValidator, times(1)).validateForUpdate(updatedUser);
        }

        @Test
        @DisplayName("Обновление несуществующего пользователя выбрасывает NotFoundException")
        void updateUser_NonExistingUser_ThrowsNotFoundExceptionTest() {
            User user = createTestUser();

            doThrow(new NotFoundException("Пользователь не найден"))
                    .when(userValidator).validateForUpdate(any(User.class));

            assertThrows(NotFoundException.class, () -> userService.updateUser(user));
            verify(userDbStorage, never()).updateUser(any(User.class));
        }
    }

    @Nested
    @DisplayName("Тесты управления друзьями")
    class FriendManagementTests {

        @Test
        @DisplayName("Добавление друга - оба пользователя существуют")
        void addFriend_BothExist_AddsFriendTest() {
            Long userId = 1L;
            Long friendId = 2L;
            User user1 = createTestUser();
            User user2 = createTestUser();
            user2.setId(2L);

            when(userValidator.validateUserExist(userId)).thenReturn(user1);
            when(userValidator.validateUserExist(friendId)).thenReturn(user2);
            doNothing().when(userDbStorage).addFriend(userId, friendId);

            userService.addFriend(userId, friendId);

            verify(userDbStorage, times(1)).addFriend(userId, friendId);
            verify(userValidator, times(1)).validateUserExist(userId);
            verify(userValidator, times(1)).validateUserExist(friendId);
        }

        @Test
        @DisplayName("Добавление друга - пользователь не существует")
        void addFriend_UserNotExist_ThrowsNotFoundExceptionTest() {
            Long userId = 1L;
            Long friendId = 2L;

            when(userValidator.validateUserExist(userId))
                    .thenThrow(new NotFoundException("Пользователь не найден"));

            assertThrows(NotFoundException.class, () ->
                    userService.addFriend(userId, friendId));
            verify(userDbStorage, never()).addFriend(anyLong(), anyLong());
            verify(userValidator, never()).validateUserExist(friendId);
        }

        @Test
        @DisplayName("Добавление друга - друг не существует")
        void addFriend_FriendNotExist_ThrowsNotFoundExceptionTest() {
            Long userId = 1L;
            Long friendId = 2L;
            User user1 = createTestUser();

            when(userValidator.validateUserExist(userId)).thenReturn(user1);
            when(userValidator.validateUserExist(friendId))
                    .thenThrow(new NotFoundException("Пользователь не найден"));

            assertThrows(NotFoundException.class, () ->
                    userService.addFriend(userId, friendId));
            verify(userDbStorage, never()).addFriend(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Добавление себя в друзья выбрасывает IllegalArgumentException")
        void addFriend_SameUser_ThrowsIllegalArgumentExceptionTest() {
            Long userId = 1L;
            User user1 = createTestUser();

            when(userValidator.validateUserExist(userId)).thenReturn(user1);

            assertThrows(IllegalArgumentException.class, () ->
                    userService.addFriend(userId, userId));
            verify(userDbStorage, never()).addFriend(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Удаление друга - оба пользователя существуют")
        void removeFriend_BothExist_RemovesFriendTest() {
            Long userId = 1L;
            Long friendId = 2L;
            User user1 = createTestUser();
            User user2 = createTestUser();
            user2.setId(2L);

            when(userValidator.validateUserExist(userId)).thenReturn(user1);
            when(userValidator.validateUserExist(friendId)).thenReturn(user2);
            doNothing().when(userDbStorage).removeFriend(userId, friendId);

            userService.removeFriend(userId, friendId);

            verify(userDbStorage, times(1)).removeFriend(userId, friendId);
            verify(userValidator, times(1)).validateUserExist(userId);
            verify(userValidator, times(1)).validateUserExist(friendId);
        }

        @Test
        @DisplayName("Удаление друга - пользователь не существует")
        void removeFriend_UserNotExist_ThrowsNotFoundExceptionTest() {
            Long userId = 1L;
            Long friendId = 2L;

            when(userValidator.validateUserExist(userId))
                    .thenThrow(new NotFoundException("Пользователь не найден"));

            assertThrows(NotFoundException.class, () ->
                    userService.removeFriend(userId, friendId));
            verify(userDbStorage, never()).removeFriend(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Получение списка друзей - пользователь существует")
        void getFriends_ValidUserId_ReturnsFriendsListTest() {
            Long userId = 1L;
            User friend = createTestUser();
            friend.setId(2L);
            User user1 = createTestUser();

            when(userValidator.validateUserExist(userId)).thenReturn(user1);
            when(userDbStorage.getFriends(userId)).thenReturn(List.of(friend));

            List<User> result = userService.getFriends(userId);

            assertEquals(1, result.size());
            assertEquals(2L, result.getFirst().getId());
            verify(userDbStorage, times(1)).getFriends(userId);
            verify(userValidator, times(1)).validateUserExist(userId);
        }

        @Test
        @DisplayName("Получение списка друзей - пользователь не существует")
        void getFriends_UserNotExist_ThrowsNotFoundExceptionTest() {
            Long userId = 1L;

            when(userValidator.validateUserExist(userId))
                    .thenThrow(new NotFoundException("Пользователь не найден"));

            assertThrows(NotFoundException.class, () -> userService.getFriends(userId));
            verify(userDbStorage, never()).getFriends(anyLong());
        }

        @Test
        @DisplayName("Получение общих друзей - оба пользователя существуют")
        void getCommonFriends_BothExist_ReturnsCommonFriendsTest() {
            Long userId1 = 1L;
            Long userId2 = 2L;
            User commonFriend = createTestUser();
            commonFriend.setId(3L);
            User user1 = createTestUser();
            User user2 = createTestUser();
            user2.setId(2L);

            when(userValidator.validateUserExist(userId1)).thenReturn(user1);
            when(userValidator.validateUserExist(userId2)).thenReturn(user2);
            when(userDbStorage.getCommonFriends(userId1, userId2)).thenReturn(List.of(commonFriend));

            List<User> result = userService.getCommonFriends(userId1, userId2);

            assertEquals(1, result.size());
            assertEquals(3L, result.getFirst().getId());
            verify(userDbStorage, times(1)).getCommonFriends(userId1, userId2);
            verify(userValidator, times(1)).validateUserExist(userId1);
            verify(userValidator, times(1)).validateUserExist(userId2);
        }

        @Test
        @DisplayName("Получение общих друзей - нет общих друзей")
        void getCommonFriends_NoCommonFriends_ReturnsEmptyListTest() {
            Long userId1 = 1L;
            Long userId2 = 2L;
            User user1 = createTestUser();
            User user2 = createTestUser();
            user2.setId(2L);

            when(userValidator.validateUserExist(userId1)).thenReturn(user1);
            when(userValidator.validateUserExist(userId2)).thenReturn(user2);
            when(userDbStorage.getCommonFriends(userId1, userId2)).thenReturn(List.of());

            List<User> result = userService.getCommonFriends(userId1, userId2);

            assertTrue(result.isEmpty());
            verify(userDbStorage, times(1)).getCommonFriends(userId1, userId2);
        }

        @Test
        @DisplayName("Получение общих друзей - первый пользователь не существует")
        void getCommonFriends_FirstUserNotExist_ThrowsNotFoundExceptionTest() {
            Long userId1 = 1L;
            Long userId2 = 2L;

            when(userValidator.validateUserExist(userId1))
                    .thenThrow(new NotFoundException("Пользователь не найден"));

            assertThrows(NotFoundException.class, () ->
                    userService.getCommonFriends(userId1, userId2));
            verify(userDbStorage, never()).getCommonFriends(anyLong(), anyLong());
        }
    }
}