package ru.yandex.practicum.filmorate.integrationDb;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.managment.db.UserDbStorage;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(UserDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserDbStorageIntegrationTest {

    private final UserDbStorage userDbStorage;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .login("testuser")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    void shouldCreateAndFindUser() {
        User createdUser = userDbStorage.createUser(testUser);

        assertThat(createdUser.getId()).isNotNull();

        Optional<User> foundUser = userDbStorage.getUserById(createdUser.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.get().getLogin()).isEqualTo("testuser");
    }

    @Test
    void shouldUpdateUser() {
        User createdUser = userDbStorage.createUser(testUser);

        User updatedUser = User.builder()
                .id(createdUser.getId())
                .email("updated@example.com")
                .login("updateduser")
                .name("Updated User")
                .birthday(LocalDate.of(1995, 1, 1))
                .build();

        User result = userDbStorage.updateUser(updatedUser);

        assertThat(result.getEmail()).isEqualTo("updated@example.com");
        assertThat(result.getLogin()).isEqualTo("updateduser");
        assertThat(result.getName()).isEqualTo("Updated User");
    }

    @Test
    void shouldFindAllUsers() {
        User user1 = userDbStorage.createUser(testUser);

        User user2 = User.builder()
                .email("user2@example.com")
                .login("user2")
                .name("User Two")
                .birthday(LocalDate.of(1992, 1, 1))
                .build();
        userDbStorage.createUser(user2);

        List<User> users = userDbStorage.getAllUsers();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getLogin)
                .contains("testuser", "user2");
    }

    @Test
    void shouldAddAndGetFriends() {
        User user1 = userDbStorage.createUser(testUser);

        User user2 = User.builder()
                .email("friend@example.com")
                .login("friend")
                .name("Friend User")
                .birthday(LocalDate.of(1992, 1, 1))
                .build();
        User friend = userDbStorage.createUser(user2);

        userDbStorage.addFriend(user1.getId(), friend.getId());

        List<User> friends = userDbStorage.getFriends(user1.getId());

        assertThat(friends).hasSize(1);
        assertThat(friends.getFirst().getLogin()).isEqualTo("friend");
    }
}