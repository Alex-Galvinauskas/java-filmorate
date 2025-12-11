package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.dto.FilmDTO;
import ru.yandex.practicum.filmorate.dto.UserDTO;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.UserDbStorage;
import ru.yandex.practicum.filmorate.mapper.UserMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.user.UserServiceImpl;
import ru.yandex.practicum.filmorate.service.user.validation.UserValidatorRules;

import java.time.LocalDate;
import java.util.*;

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

    @Mock
    private UserMapper userMapper;

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

    private UserDTO createTestUserDTO() {
        return UserDTO.builder()
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
            UserDTO inputDTO = createTestUserDTO();
            inputDTO.setId(null);
            User inputUser = createTestUser();
            inputUser.setId(null);
            User createdUser = createTestUser();
            UserDTO expectedDTO = createTestUserDTO();

            when(userMapper.toEntity(inputDTO)).thenReturn(inputUser);
            when(userMapper.toDTO(createdUser)).thenReturn(expectedDTO);
            doNothing().when(userValidator).validateForCreate(inputUser);
            when(userDbStorage.createUser(inputUser)).thenReturn(createdUser);

            UserDTO result = userService.createUser(inputDTO);

            assertNotNull(result);
            assertEquals(expectedDTO.getId(), result.getId());
            assertEquals("test@example.com", result.getEmail());
            assertEquals("Test User", result.getName());
            verify(userDbStorage, times(1)).createUser(inputUser);
            verify(userValidator, times(1)).validateForCreate(inputUser);
        }

        @Test
        @DisplayName("Создание пользователя с пустым именем устанавливает имя из логина")
        void createUser_EmptyName_SetsNameFromLoginTest() {
            UserDTO inputDTO = createTestUserDTO();
            inputDTO.setId(null);
            inputDTO.setName("  ");
            User inputUser = createTestUser();
            inputUser.setId(null);
            inputUser.setName("  ");
            User createdUser = createTestUser();
            createdUser.setName("testlogin");
            UserDTO expectedDTO = createTestUserDTO();
            expectedDTO.setName("testlogin");

            when(userMapper.toEntity(inputDTO)).thenReturn(inputUser);
            when(userMapper.toDTO(createdUser)).thenReturn(expectedDTO);
            doNothing().when(userValidator).validateForCreate(inputUser);
            when(userDbStorage.createUser(inputUser)).thenReturn(createdUser);

            UserDTO result = userService.createUser(inputDTO);

            assertNotNull(result);
            assertEquals("testlogin", result.getName());
            verify(userDbStorage, times(1)).createUser(inputUser);
        }

        @Test
        @DisplayName("Создание дублирующего пользователя выбрасывает DuplicateException")
        void createUser_DuplicateUser_ThrowsDuplicateExceptionTest() {
            UserDTO userDTO = createTestUserDTO();
            userDTO.setId(null);
            User user = createTestUser();
            user.setId(null);

            when(userMapper.toEntity(userDTO)).thenReturn(user);
            doThrow(new DuplicateException("Пользователь с таким email уже существует"))
                    .when(userValidator).validateForCreate(user);

            assertThrows(DuplicateException.class, () -> userService.createUser(userDTO));
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
            UserDTO userDTO = createTestUserDTO();

            when(userDbStorage.getAllUsers()).thenReturn(List.of(user));
            when(userMapper.toDTO(user)).thenReturn(userDTO);

            List<UserDTO> result = userService.getAllUsers();

            assertEquals(1, result.size());
            assertEquals("test@example.com", result.getFirst().getEmail());
            verify(userDbStorage, times(1)).getAllUsers();
            verify(userMapper, times(1)).toDTO(user);
        }

        @Test
        @DisplayName("Получение пользователя по существующему ID возвращает пользователя")
        void getUserById_ExistingId_ReturnsUserTest() {
            User user = createTestUser();
            UserDTO userDTO = createTestUserDTO();

            when(userValidator.validateUserExist(1L)).thenReturn(user);
            when(userMapper.toDTO(user)).thenReturn(userDTO);

            UserDTO result = userService.getUserById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(userValidator, times(1)).validateUserExist(1L);
            verify(userMapper, times(1)).toDTO(user);
        }

        @Test
        @DisplayName("Получение пользователя по несуществующему ID выбрасывает NotFoundException")
        void getUserById_NonExistingId_ThrowsNotFoundExceptionTest() {
            when(userValidator.validateUserExist(999L))
                    .thenThrow(new NotFoundException("Пользователь не найден"));

            assertThrows(NotFoundException.class, () -> userService.getUserById(999L));
            verify(userValidator, times(1)).validateUserExist(999L);
            verify(userMapper, never()).toDTO(any(User.class));
        }
    }

    @Nested
    @DisplayName("Тесты обновления пользователей")
    class UpdateUserTests {

        @Test
        @DisplayName("Обновление валидного пользователя возвращает обновленного пользователя")
        void updateUser_ValidUser_ReturnsUpdatedUserTest() {
            UserDTO updatedUserDTO = createTestUserDTO();
            updatedUserDTO.setName("Updated User");
            User updatedUser = createTestUser();
            updatedUser.setName("Updated User");
            UserDTO expectedDTO = createTestUserDTO();
            expectedDTO.setName("Updated User");

            when(userMapper.toEntity(updatedUserDTO)).thenReturn(updatedUser);
            when(userMapper.toDTO(updatedUser)).thenReturn(expectedDTO);
            doNothing().when(userValidator).validateForUpdate(updatedUser);
            when(userDbStorage.updateUser(updatedUser)).thenReturn(updatedUser);

            UserDTO result = userService.updateUser(updatedUserDTO);

            assertNotNull(result);
            assertEquals("Updated User", result.getName());
            verify(userDbStorage, times(1)).updateUser(updatedUser);
            verify(userValidator, times(1)).validateForUpdate(updatedUser);
        }

        @Test
        @DisplayName("Обновление несуществующего пользователя выбрасывает NotFoundException")
        void updateUser_NonExistingUser_ThrowsNotFoundExceptionTest() {
            UserDTO userDTO = createTestUserDTO();
            User user = createTestUser();

            when(userMapper.toEntity(userDTO)).thenReturn(user);
            doThrow(new NotFoundException("Пользователь не найден"))
                    .when(userValidator).validateForUpdate(user);

            assertThrows(NotFoundException.class, () -> userService.updateUser(userDTO));
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
            UserDTO friendDTO = createTestUserDTO();
            friendDTO.setId(2L);
            User user1 = createTestUser();

            when(userValidator.validateUserExist(userId)).thenReturn(user1);
            when(userDbStorage.getFriends(userId)).thenReturn(List.of(friend));
            when(userMapper.toDTO(friend)).thenReturn(friendDTO);

            List<UserDTO> result = userService.getFriends(userId);

            assertEquals(1, result.size());
            assertEquals(2L, result.getFirst().getId());
            verify(userDbStorage, times(1)).getFriends(userId);
            verify(userValidator, times(1)).validateUserExist(userId);
            verify(userMapper, times(1)).toDTO(friend);
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
            UserDTO commonFriendDTO = createTestUserDTO();
            commonFriendDTO.setId(3L);
            User user1 = createTestUser();
            User user2 = createTestUser();
            user2.setId(2L);

            when(userValidator.validateUserExist(userId1)).thenReturn(user1);
            when(userValidator.validateUserExist(userId2)).thenReturn(user2);
            when(userDbStorage.getCommonFriends(userId1, userId2)).thenReturn(List.of(commonFriend));
            when(userMapper.toDTO(commonFriend)).thenReturn(commonFriendDTO);

            List<UserDTO> result = userService.getCommonFriends(userId1, userId2);

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

            List<UserDTO> result = userService.getCommonFriends(userId1, userId2);

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