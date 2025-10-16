/**
 * Контроллер для управления операциями с пользователями.
 * Обрабатывает HTTP-запросы для создания, получения, обновления пользователей.
 * Предоставляет REST API для работы с сущностью User.
 *
 * @see User
 * @see ru.yandex.practicum.filmorate.service.user.UserService
 */
package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.user.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Создает нового пользователя.
     *
     * @param user объект пользователя для создания
     *
     * @return созданный пользователь с присвоенным идентификатором
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@Valid @RequestBody User user) {
        return userService.createUser(user);
    }

    /**
     * Возвращает список всех пользователей.
     *
     * @return список всех пользователей в системе
     */
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    /**
     * Возвращает пользователя по его идентификатору.
     *
     * @param id идентификатор пользователя
     *
     * @return найденный пользователь
     *
     * @throws ru.yandex.practicum.filmorate.exception.NotFoundException если пользователь с указанным ID не найден
     */
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    /**
     * Обновляет существующего пользователя.
     *
     * @param user объект пользователя с обновленными данными
     *
     * @return обновленный пользователь
     *
     * @throws ru.yandex.practicum.filmorate.exception.NotFoundException если пользователь с указанным ID не найден
     */
    @PutMapping
    public User updateUser(@Valid @RequestBody User user) {
        return userService.updateUser(user);
    }
}