package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.user.UserService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тестирование API контроллера пользователей")
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;
    private User testUser;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

        testUser = User.builder()
                .id(1L)
                .email("user@test.com")
                .login("user1")
                .name("User One")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        testUser2 = User.builder()
                .id(2L)
                .email("user2@test.com")
                .login("user2")
                .name("User Two")
                .birthday(LocalDate.of(1995, 1, 1))
                .build();

        testUser3 = User.builder()
                .id(3L)
                .email("user3@test.com")
                .login("user3")
                .name("User Three")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();
    }

    @Nested
    @DisplayName("Тесты операций с пользователями")
    class UserOperationsTests {

        @Test
        @DisplayName("Создание пользователя возвращает созданного пользователя")
        void createUser_ShouldReturnCreatedUserTest() throws Exception {
            when(userService.createUser(any(User.class))).thenReturn(testUser);

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testUser)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.email", is("user@test.com")))
                    .andExpect(jsonPath("$.login", is("user1")))
                    .andExpect(jsonPath("$.name", is("User One")));

            verify(userService, times(1)).createUser(any(User.class));
        }

        @Test
        @DisplayName("Получение всех пользователей возвращает список пользователей")
        void getAllUsers_ShouldReturnListOfUsersTest() throws Exception {
            List<User> users = Arrays.asList(testUser, testUser2);
            when(userService.getAllUsers()).thenReturn(users);

            mockMvc.perform(get("/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].login", is("user1")))
                    .andExpect(jsonPath("$[1].login", is("user2")));

            verify(userService, times(1)).getAllUsers();
        }

        @Test
        @DisplayName("Получение пользователя по ID возвращает пользователя")
        void getUserById_ShouldReturnUserTest() throws Exception {
            when(userService.getUserById(1L)).thenReturn(testUser);

            mockMvc.perform(get("/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.login", is("user1")));

            verify(userService, times(1)).getUserById(1L);
        }

        @Test
        @DisplayName("Обновление пользователя возвращает обновленного пользователя")
        void updateUser_ShouldReturnUpdatedUserTest() throws Exception {
            User updatedUser = User.builder()
                    .id(1L)
                    .email("updated@test.com")
                    .login("updatedUser")
                    .name("Updated User")
                    .birthday(LocalDate.of(1990, 1, 1))
                    .build();

            when(userService.updateUser(any(User.class))).thenReturn(updatedUser);

            mockMvc.perform(put("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatedUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is("updated@test.com")))
                    .andExpect(jsonPath("$.login", is("updatedUser")));

            verify(userService, times(1)).updateUser(any(User.class));
        }
    }

    @Nested
    @DisplayName("Тесты операций с друзьями")
    class FriendOperationsTests {

        @Test
        @DisplayName("Добавление друга вызывает сервис")
        void addFriend_ShouldCallServiceTest() throws Exception {
            doNothing().when(userService).addFriend(1L, 2L);

            mockMvc.perform(put("/users/1/friends/2"))
                    .andExpect(status().isOk());

            verify(userService, times(1)).addFriend(1L, 2L);
        }

        @Test
        @DisplayName("Удаление друга вызывает сервис")
        void removeFriend_ShouldCallServiceTest() throws Exception {
            doNothing().when(userService).removeFriend(1L, 2L);

            mockMvc.perform(delete("/users/1/friends/2"))
                    .andExpect(status().isOk());

            verify(userService, times(1)).removeFriend(1L, 2L);
        }

        @Test
        @DisplayName("Получение списка друзей возвращает список друзей")
        void getFriends_ShouldReturnFriendsListTest() throws Exception {
            List<User> friends = Collections.singletonList(testUser2);
            when(userService.getFriends(1L)).thenReturn(friends);

            mockMvc.perform(get("/users/1/friends"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].login", is("user2")));

            verify(userService, times(1)).getFriends(1L);
        }

        @Test
        @DisplayName("Получение общих друзей возвращает список общих друзей")
        void getCommonFriends_ShouldReturnCommonFriendsTest() throws Exception {
            List<User> commonFriends = Collections.singletonList(testUser3);
            when(userService.getCommonFriends(1L, 2L)).thenReturn(commonFriends);

            mockMvc.perform(get("/users/1/friends/common/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].login", is("user3")));

            verify(userService, times(1))
                    .getCommonFriends(1L, 2L);
        }
    }

    @Nested
    @DisplayName("Тесты валидации endpoints")
    class EndpointValidationTests {

        @Test
        @DisplayName("Некорректный ID пользователя в пути возвращает ошибку")
        void getUserById_InvalidId_ReturnsBadRequestTest() throws Exception {
            mockMvc.perform(get("/users/invalid"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Некорректные ID для операций с друзьями возвращают ошибку")
        void addFriend_InvalidIds_ReturnsBadRequestTest() throws Exception {
            mockMvc.perform(put("/users/invalid/friends/invalid"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Некорректные ID для получения общих друзей возвращают ошибку")
        void getCommonFriends_InvalidIds_ReturnsBadRequestTest() throws Exception {
            mockMvc.perform(get("/users/invalid/friends/common/invalid"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Пустой список друзей возвращает пустой массив")
        void getFriends_EmptyFriendsList_ReturnsEmptyArrayTest() throws Exception {
            when(userService.getFriends(1L)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/users/1/friends"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(userService, times(1)).getFriends(1L);
        }

        @Test
        @DisplayName("Отсутствие общих друзей возвращает пустой массив")
        void getCommonFriends_NoCommonFriends_ReturnsEmptyArrayTest() throws Exception {
            when(userService.getCommonFriends(1L, 2L))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/users/1/friends/common/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(userService, times(1))
                    .getCommonFriends(1L, 2L);
        }
    }
}