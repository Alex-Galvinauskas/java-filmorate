/**
 * Контроллер для управления операциями с пользователями.
 * Обрабатывает HTTP-запросы для создания, получения, обновления пользователей.
 * Предоставляет REST API для работы с сущностью User.
 *
 * @see User
 * @see ru.yandex.practicum.filmorate.service.user.UserService
 */
package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.user.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
@Slf4j
    public class UserController extends AbstractController<User, UserService> {


    public UserController(UserService userService) {
        super(userService, "пользователя");
    }

    @Override
    protected User createEntity(User user) {
        return service.createUser(user);
    }

    @Override
    protected List<User> getAllEntities() {
        return service.getAllUsers();
    }

    @Override
    protected User getEntityById(Long id) {
        return service.getUserById(id);
    }

    @Override
    protected User updateEntity(User user) {
        return service.updateUser(user);
    }

    /**
     * Добавляет пользователя в друзья
     * @param id - id пользователя
     * @param friendId - id друга для добавления
     */
    @PutMapping("/{id}/friends/{friendId}")
    public void addFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("Получен запрос на добавление друга {} к пользователю {}", friendId, id);

        service.addFriend(id, friendId);

        log.info("Пользователь {} успешно добавлен в друзья {}", friendId, id);
    }

    /**
     * Получает список друзей пользователя
     * @param id - id пользователя
     * @return - список друзей
     */
    @GetMapping("/{id}/friends")
    public List<User> getFriends(@PathVariable Long id) {
        log.info("Получен запрос на получение списка друзей пользователя {}", id);

        List<User> friends = service.getFriends(id);

        log.info("Список друзей {} пользователя {} получен", friends.size(), id);
        return friends;
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable Long id, @PathVariable Long otherId) {
        log.info("Получен запрос на получение общих друзей пользователей {} и {}", id, otherId);

        List<User> commonFriends = service.getCommonFriends(id, otherId);

        log.info("Список общих друзей пользователей {} и {} получен", id, otherId);
        return commonFriends;
    }

    /**
     * Удаляет друга из списка друзей
     * @param id - id пользователя
     * @param friendId - id друга для удаления
     */
    @DeleteMapping("/{id}/friends/{friendId}")
    public void removeFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("Получен запрос на удаление друга {} из списка друзей пользователя {}", friendId, id);

        service.removeFriend(id, friendId);

        log.info("Пользователь {} успешно удален из друзей {}", friendId, id);
    }
}
