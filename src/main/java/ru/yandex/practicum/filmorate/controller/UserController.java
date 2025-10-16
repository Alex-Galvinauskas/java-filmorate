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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}
