package ru.yandex.practicum.filmorate.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.UserDTO;
import ru.yandex.practicum.filmorate.service.user.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController extends AbstractController<UserDTO, UserService> {

    public UserController(UserService userService) {
        super(userService, "пользователь");
    }

    @Override
    protected UserDTO createEntity(UserDTO userDTO) {
        return service.createUser(userDTO);
    }

    @Override
    protected List<UserDTO> getAllEntities() {
        return service.getAllUsers();
    }

    @Override
    protected UserDTO getEntityById(Long id) {
        return service.getUserById(id);
    }

    @Override
    protected UserDTO updateEntity(UserDTO userDTO) {
        return service.updateUser(userDTO);
    }

    @Override
    protected Long getEntityId(UserDTO userDTO) {
        return userDTO.getId();
    }

    @PutMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Void> addFriend(@PathVariable Long id, @PathVariable Long friendId) {
        service.addFriend(id, friendId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/friends")
    public ResponseEntity<List<UserDTO>> getFriends(@PathVariable Long id) {
        List<UserDTO> friends = service.getFriends(id);
        return ResponseEntity.ok(friends);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public ResponseEntity<List<UserDTO>> getCommonFriends(@PathVariable Long id, @PathVariable Long otherId) {
        List<UserDTO> commonFriends = service.getCommonFriends(id, otherId);
        return ResponseEntity.ok(commonFriends);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Void> removeFriend(@PathVariable Long id, @PathVariable Long friendId) {
        service.removeFriend(id, friendId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        service.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}