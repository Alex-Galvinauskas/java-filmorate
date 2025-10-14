package ru.yandex.practicum.filmorate.managment;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;

public interface UserStorage {
    User createUser(User user);

    List<User> getAllUsers();

    Optional<User> getUserById(Long id);

    Optional<User> getUserByEmail(String email);

    Optional<User> getUserByLogin(String login);

    User updateUser(User user);

    boolean existsById(Long id);

    boolean existsByEmail(String email);

    boolean existsByLogin(String login);
}
