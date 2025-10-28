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
import ru.yandex.practicum.filmorate.managment.UserStorage;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.user.UserServiceImpl;
import ru.yandex.practicum.filmorate.service.user.validation.UserValidatorImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты сервиса управления пользователями")
class UserServiceImplTest {

    @Mock
    private UserStorage userStorage;

    @Mock
    private UserValidatorImpl userValidator;

    @InjectMocks
    private UserServiceImpl userService;

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
    @DisplayName("Тесты создания пользователя")
    class CreateUserTests {

        @Test
        @DisplayName("Создание пользователя с дублирующим логином выбрасывает DuplicateException")
        void createUser_DuplicateLogin_ThrowsDuplicateExceptionTest() {
            User user = createTestUser();
            user.setId(null);

            doThrow(new DuplicateException("Пользователь с таким логином test-login уже существует"))
                    .when(userValidator).validateForCreate(user);

            assertThrows(DuplicateException.class, () -> userService.createUser(user));
            verify(userStorage, never()).createUser(any(User.class));
        }

        @Test
        @DisplayName("Создание пользователя с дублирующим email выбрасывает DuplicateException")
        void createUser_DuplicateEmail_ThrowsDuplicateExceptionTest() {
            User user = createTestUser();
            user.setId(null);

            doThrow(new DuplicateException("Пользователь с таким email test@example.com уже существует"))
                    .when(userValidator).validateForCreate(user);

            assertThrows(DuplicateException.class, () -> userService.createUser(user));
            verify(userStorage, never()).createUser(any(User.class));
        }

        @Test
        @DisplayName("Создание пользователя с валидными данными возвращает созданного пользователя")
        void createUser_ValidUser_ReturnsCreatedUserTest() {
            User user = createTestUser();
            user.setId(null);

            when(userStorage.createUser(any(User.class))).thenReturn(user);

            User result = userService.createUser(user);

            assertNotNull(result);
            assertEquals("test@example.com", result.getEmail());
            verify(userStorage, times(1)).createUser(any(User.class));
        }
    }

    @Nested
    @DisplayName("Тесты нормализации имени пользователя")
    class UserNameNormalizationTests {

        @Test
        @DisplayName("Создание пользователя с пустым именем устанавливает имя из логина")
        void createUser_EmptyName_SetsNameFromLoginTest() {
            User user = User.builder()
                    .email("test@example.com")
                    .login("test-login")
                    .name("")
                    .birthday(LocalDate.of(1990, 1, 1))
                    .build();

            when(userStorage.createUser(any(User.class))).thenReturn(user);

            User result = userService.createUser(user);

            assertEquals("test-login", result.getName());
        }

        @Test
        @DisplayName("Нормализация пользователя - имя null устанавливается из логина")
        void normalizeUser_NameNull_SetsNameFromLoginTest() {
            User user = User.builder()
                    .email("test@example.com")
                    .login("test-login")
                    .name(null)
                    .birthday(LocalDate.of(1990, 1, 1))
                    .build();

            when(userStorage.createUser(any(User.class))).thenReturn(user);

            User result = userService.createUser(user);

            assertEquals("test-login", result.getName());
        }

        @Test
        @DisplayName("Нормализация пользователя - имя пустое устанавливается из логина")
        void normalizeUser_NameEmpty_SetsNameFromLoginTest() {
            User user = User.builder()
                    .email("test@example.com")
                    .login("test-login")
                    .name("   ")
                    .birthday(LocalDate.of(1990, 1, 1))
                    .build();

            when(userStorage.createUser(any(User.class))).thenReturn(user);

            User result = userService.createUser(user);

            assertEquals("test-login", result.getName());
        }

        @Test
        @DisplayName("Нормализация пользователя - имя указано явно, не заменяется логином")
        void normalizeUser_NonEmptyName_NotReplacedTest() {
            User user = User.builder()
                    .email("test@example.com")
                    .login("test-login")
                    .name("Real Name")
                    .birthday(LocalDate.of(1990, 1, 1))
                    .build();

            when(userStorage.createUser(any(User.class))).thenReturn(user);

            User result = userService.createUser(user);

            assertEquals("Real Name", result.getName());
            assertNotEquals("test-login", result.getName());
        }

        @Test
        @DisplayName("Создание пользователя - нормализация не изменяет валидное имя")
        void createUser_ValidName_NormalizationNotAppliedTest() {
            User user = User.builder()
                    .email("valid@example.com")
                    .login("valid-login")
                    .name("Valid Name")
                    .birthday(LocalDate.of(1990, 1, 1))
                    .build();

            when(userStorage.createUser(any(User.class))).thenReturn(user);

            User result = userService.createUser(user);

            assertEquals("Valid Name", result.getName());
        }
    }

    @Nested
    @DisplayName("Тесты управления друзьями")
    class FriendsManagementTests {

        @Test
        @DisplayName("Добавление друзей - оба пользователя существуют")
        void addFriend_BothUsersExist_AddsFriendsTest() {
            User user = createTestUser();
            User friend = createTestUser();
            friend.setId(2L);
            friend.setEmail("friend@example.com");
            friend.setLogin("friend-login");

            when(userValidator.validateUserExist(1L)).thenReturn(user);
            when(userValidator.validateUserExist(2L)).thenReturn(friend);

            userService.addFriend(1L, 2L);

            assertTrue(user.getFriends().contains(2L));
            assertTrue(friend.getFriends().contains(1L));
            verify(userValidator, times(1)).validateUserExist(1L);
            verify(userValidator, times(1)).validateUserExist(2L);
        }

        @Test
        @DisplayName("Добавление друзей - первый пользователь не существует")
        void addFriend_FirstUserNotExist_ThrowsNotFoundExceptionTest() {
            when(userValidator.validateUserExist(1L))
                    .thenThrow(new NotFoundException("Пользователь с id 1 не найден"));

            assertThrows(NotFoundException.class, () -> userService.addFriend(1L, 2L));
            verify(userValidator, never()).validateUserExist(2L);
        }

        @Test
        @DisplayName("Добавление друзей - второй пользователь не существует")
        void addFriend_SecondUserNotExist_ThrowsNotFoundExceptionTest() {
            User user = createTestUser();

            when(userValidator.validateUserExist(1L)).thenReturn(user);
            when(userValidator.validateUserExist(2L))
                    .thenThrow(new NotFoundException("Пользователь с id 2 не найден"));

            assertThrows(NotFoundException.class, () -> userService.addFriend(1L,
                    2L));
            verify(userValidator, times(1)).validateUserExist(1L);
            verify(userValidator, times(1)).validateUserExist(2L);
        }

        @Test
        @DisplayName("Добавление друга - пользователи уже друзья, дубликаты не создаются")
        void addFriend_AlreadyFriends_NoDuplicateTest() {
            User user = createTestUser();
            user.getFriends().add(2L);

            User friend = createTestUser();
            friend.setId(2L);
            friend.getFriends().add(1L);

            when(userValidator.validateUserExist(1L)).thenReturn(user);
            when(userValidator.validateUserExist(2L)).thenReturn(friend);

            userService.addFriend(1L, 2L);

            assertEquals(1, user.getFriends().size());
            assertEquals(1, friend.getFriends().size());
            assertTrue(user.getFriends().contains(2L));
            assertTrue(friend.getFriends().contains(1L));
        }

        @Test
        @DisplayName("Удаление друзей - оба пользователя существуют")
        void removeFriend_BothUsersExist_RemovesFriendsTest() {
            User user = createTestUser();
            user.getFriends().add(2L);

            User friend = createTestUser();
            friend.setId(2L);
            friend.getFriends().add(1L);

            when(userValidator.validateUserExist(1L)).thenReturn(user);
            when(userValidator.validateUserExist(2L)).thenReturn(friend);

            userService.removeFriend(1L, 2L);

            assertFalse(user.getFriends().contains(2L));
            assertFalse(friend.getFriends().contains(1L));
        }

        @Test
        @DisplayName("Удаление друзей - пользователь не существует")
        void removeFriend_UserNotExist_ThrowsNotFoundExceptionTest() {
            when(userValidator.validateUserExist(1L))
                    .thenThrow(new NotFoundException("Пользователь с id 1 не найден"));

            assertThrows(NotFoundException.class, () -> userService.removeFriend(1L,
                    2L));
        }

        @Test
        @DisplayName("Удаление друга - пользователи не друзья, исключений нет")
        void removeFriend_NotFriends_NoExceptionTest() {
            User user = createTestUser();
            User friend = createTestUser();
            friend.setId(2L);

            when(userValidator.validateUserExist(1L)).thenReturn(user);
            when(userValidator.validateUserExist(2L)).thenReturn(friend);

            assertDoesNotThrow(() -> userService.removeFriend(1L, 2L));

            assertFalse(user.getFriends().contains(2L));
            assertFalse(friend.getFriends().contains(1L));
        }
    }

    @Nested
    @DisplayName("Тесты получения друзей")
    class GetFriendsTests {

        @Test
        @DisplayName("Получение списка друзей - пользователь существует")
        void getFriends_UserExists_ReturnsFriendsListTest() {
            User user = createTestUser();
            user.getFriends().add(2L);
            user.getFriends().add(3L);

            User friend1 = createTestUser();
            friend1.setId(2L);
            friend1.setEmail("friend1@example.com");

            User friend2 = createTestUser();
            friend2.setId(3L);
            friend2.setEmail("friend2@example.com");

            when(userValidator.validateUserExist(1L)).thenReturn(user);
            when(userStorage.getUserById(2L)).thenReturn(Optional.of(friend1));
            when(userStorage.getUserById(3L)).thenReturn(Optional.of(friend2));

            List<User> friends = userService.getFriends(1L);

            assertEquals(2, friends.size());
            assertTrue(friends.stream().anyMatch(f -> f.getId().equals(2L)));
            assertTrue(friends.stream().anyMatch(f -> f.getId().equals(3L)));
        }

        @Test
        @DisplayName("Получение списка друзей - пользователь не существует")
        void getFriends_UserNotExist_ThrowsNotFoundExceptionTest() {
            when(userValidator.validateUserExist(1L))
                    .thenThrow(new NotFoundException("Пользователь с id 1 не найден"));

            assertThrows(NotFoundException.class, () -> userService.getFriends(1L));
        }

        @Test
        @DisplayName("Получение списка друзей - когда друг не найден в хранилище")
        void getFriends_FriendNotFoundInStorage_ExcludesFromResultTest() {
            User user = createTestUser();
            user.getFriends().add(2L);
            user.getFriends().add(3L);

            when(userValidator.validateUserExist(1L)).thenReturn(user);
            when(userStorage.getUserById(2L)).thenReturn(Optional.empty());
            when(userStorage.getUserById(3L)).thenReturn(Optional.of(createTestUser()));

            List<User> friends = userService.getFriends(1L);

            assertEquals(1, friends.size());
            assertFalse(friends.stream().anyMatch(f -> f.getId().equals(2L)));
        }
    }

    @Nested
    @DisplayName("Тесты получения общих друзей")
    class CommonFriendsTests {

        @Test
        @DisplayName("Получение общих друзей - пользователи существуют и имеют общих друзей")
        void getCommonFriends_UsersExistWithCommonFriends_ReturnsCommonFriendsTest() {
            User user1 = createTestUser();
            user1.getFriends().addAll(Set.of(2L, 3L, 4L));

            User user2 = createTestUser();
            user2.setId(2L);
            user2.getFriends().addAll(Set.of(1L, 3L, 5L));

            User commonFriend = createTestUser();
            commonFriend.setId(3L);
            commonFriend.setEmail("common@example.com");

            when(userValidator.validateUserExist(1L)).thenReturn(user1);
            when(userValidator.validateUserExist(2L)).thenReturn(user2);
            when(userStorage.getUserById(3L)).thenReturn(Optional.of(commonFriend));

            List<User> commonFriends = userService.getCommonFriends(1L, 2L);

            assertEquals(1, commonFriends.size());
            assertEquals(3L, commonFriends.getFirst().getId());
        }

        @Test
        @DisplayName("Получение общих друзей - нет общих друзей")
        void getCommonFriends_NoCommonFriends_ReturnsEmptyListTest() {
            User user1 = createTestUser();
            user1.getFriends().add(2L);

            User user2 = createTestUser();
            user2.setId(2L);
            user2.getFriends().add(3L);

            when(userValidator.validateUserExist(1L)).thenReturn(user1);
            when(userValidator.validateUserExist(2L)).thenReturn(user2);

            List<User> commonFriends = userService.getCommonFriends(1L, 2L);

            assertTrue(commonFriends.isEmpty());
        }

        @Test
        @DisplayName("Получение общих друзей - общий друг не найден в хранилище")
        void getCommonFriends_CommonFriendNotFound_ExcludesFromResultTest() {
            User user1 = createTestUser();
            user1.getFriends().add(3L);

            User user2 = createTestUser();
            user2.setId(2L);
            user2.getFriends().add(3L);

            when(userValidator.validateUserExist(1L)).thenReturn(user1);
            when(userValidator.validateUserExist(2L)).thenReturn(user2);
            when(userStorage.getUserById(3L)).thenReturn(Optional.empty());

            List<User> commonFriends = userService.getCommonFriends(1L, 2L);

            assertTrue(commonFriends.isEmpty());
        }

        @Test
        @DisplayName("Получение общих друзей - множественные общие друзья")
        void getCommonFriends_MultipleCommonFriends_ReturnsAllTest() {
            User user1 = createTestUser();
            user1.getFriends().addAll(Set.of(3L, 4L, 5L));

            User user2 = createTestUser();
            user2.setId(2L);
            user2.getFriends().addAll(Set.of(3L, 4L, 6L));

            User friend3 = createTestUser();
            friend3.setId(3L);
            friend3.setEmail("friend3@example.com");

            User friend4 = createTestUser();
            friend4.setId(4L);
            friend4.setEmail("friend4@example.com");

            when(userValidator.validateUserExist(1L)).thenReturn(user1);
            when(userValidator.validateUserExist(2L)).thenReturn(user2);
            when(userStorage.getUserById(3L)).thenReturn(Optional.of(friend3));
            when(userStorage.getUserById(4L)).thenReturn(Optional.of(friend4));

            List<User> commonFriends = userService.getCommonFriends(1L, 2L);

            assertEquals(2, commonFriends.size());
            assertTrue(commonFriends.stream().anyMatch(f -> f.getId().equals(3L)));
            assertTrue(commonFriends.stream().anyMatch(f -> f.getId().equals(4L)));
        }
    }

    @Nested
    @DisplayName("Тесты получения пользователей")
    class GetUsersTests {

        @Test
        @DisplayName("Получение всех пользователей возвращает список пользователей")
        void getAllUsers_ReturnsUsersListTest() {
            User user = createTestUser();
            when(userStorage.getAllUsers()).thenReturn(List.of(user));

            List<User> result = userService.getAllUsers();

            assertEquals(1, result.size());
            assertEquals("test@example.com", result.getFirst().getEmail());
            verify(userStorage, times(1)).getAllUsers();
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
                    .thenThrow(new NotFoundException("Пользователь с id 999 не найден"));

            assertThrows(NotFoundException.class, () -> userService.getUserById(999L));
            verify(userValidator, times(1)).validateUserExist(999L);
        }
    }

    @Nested
    @DisplayName("Тесты обновления пользователя")
    class UpdateUserTests {

        @Test
        @DisplayName("Обновление валидного пользователя возвращает обновленного пользователя")
        void updateUser_ValidUser_ReturnsUpdatedUserTest() {
            User existingUser = createTestUser();
            User updatedUser = createTestUser();
            updatedUser.setName("Updated User");

            when(userStorage.updateUser(any(User.class))).thenReturn(updatedUser);

            User result = userService.updateUser(updatedUser);

            assertNotNull(result);
            assertEquals("Updated User", result.getName());
            verify(userStorage, times(1)).updateUser(any(User.class));
        }

        @Test
        @DisplayName("Обновление несуществующего пользователя выбрасывает NotFoundException")
        void updateUser_NonExistingUser_ThrowsNotFoundExceptionTest() {
            User user = createTestUser();

            doThrow(new NotFoundException("Пользователь с id 1 не найден"))
                    .when(userValidator).validateForUpdate(user);

            assertThrows(NotFoundException.class, () -> userService.updateUser(user));
            verify(userStorage, never()).updateUser(any(User.class));
        }

        @Test
        @DisplayName("Обновление пользователя с дублирующим email выбрасывает DuplicateException")
        void updateUser_DuplicateEmail_ThrowsDuplicateExceptionTest() {
            User updatedUser = createTestUser();
            updatedUser.setEmail("new@example.com");

            doThrow(new DuplicateException("Email уже используется"))
                    .when(userValidator).validateForUpdate(updatedUser);

            assertThrows(DuplicateException.class, () -> userService.updateUser(updatedUser));
            verify(userStorage, never()).updateUser(any(User.class));
        }

        @Test
        @DisplayName("Обновление пользователя - нормализация имени при обновлении")
        void updateUser_EmptyName_NormalizesNameTest() {
            User user = createTestUser();
            user.setName("");

            when(userStorage.updateUser(any(User.class))).thenReturn(user);

            User result = userService.updateUser(user);

            assertEquals(user.getLogin(), result.getName());
        }
    }
}