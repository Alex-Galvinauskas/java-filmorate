package ru.yandex.practicum.filmorate.service.user;

import ru.yandex.practicum.filmorate.dto.UserDTO;

import java.util.List;

public interface UserService {
    UserDTO createUser(UserDTO userDTO);

    UserDTO updateUser(UserDTO userDTO);

    UserDTO getUserById(Long id);

    List<UserDTO> getAllUsers();

    List<UserDTO> getFriends(Long userId);

    List<UserDTO> getCommonFriends(Long userId1, Long userId2);

    void addFriend(Long userId, Long friendId);

    void removeFriend(Long userId, Long friendId);

    void deleteUser(Long userId);
}