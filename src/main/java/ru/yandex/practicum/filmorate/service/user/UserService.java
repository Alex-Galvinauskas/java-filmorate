package ru.yandex.practicum.filmorate.service.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;

public interface UserService {
    User createUser(User user);

    void addFriend(Long userId, Long friendId);

    List<User> getAllUsers();

    User getUserById(Long id);

    List<User> getFriends(Long userId);

    List<User> getCommonFriends(Long userId1, Long otherUserId);

    User updateUser(User user);

    void removeFriend(Long userId, Long friendId);
}