package ru.yandex.practicum.filmorate.utils.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.UserStorage;
import ru.yandex.practicum.filmorate.model.User;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserValidator {

    private final UserStorage userStorage;

    /**
     * Выполняет все проверки для создания нового пользователя.
     *
     * @param user пользователь для создания
     */
    public void validateForCreation(User user) {
        validateUniqueEmailForCreation(user.getEmail());
        validateUniqueLoginForCreation(user.getLogin());
    }

    /**
     * Проверяет уникальность email при создании нового пользователя.
     *
     * @param email email для проверки
     *
     * @throws DuplicateException если email уже используется другим пользователем
     */
    public void validateUniqueEmailForCreation(String email) {
        if (userStorage.getUserByEmail(email).isPresent()) {
            throw new DuplicateException("Пользователь с таким email " + email + " уже существует");
        }
    }

    /**
     * Проверяет уникальность логина при создании нового пользователя.
     *
     * @param login логин для проверки
     *
     * @throws DuplicateException если логин уже используется другим пользователем
     */
    public void validateUniqueLoginForCreation(String login) {
        if (userStorage.getUserByLogin(login).isPresent()) {
            throw new DuplicateException("Пользователь с таким логином " + login + " уже существует");
        }
    }

    /**
     * Выполняет все проверки для обновления пользователя.
     *
     * @param user пользователь для обновления
     */
    public void validateForUpdate(User user) {
        User existingUser = validateUserExist(user.getId());
        validateUniqueEmailForUpdate(user, existingUser);
        validateUniqueLoginForUpdate(user, existingUser);
    }

    /**
     * Проверяет существование пользователя по идентификатору.
     *
     * @param id идентификатор пользователя
     *
     * @return найденный пользователь
     *
     * @throws NotFoundException если пользователь с указанным ID не найден
     */
    public User validateUserExist(Long id) {
        return userStorage.getUserById(id)
                .orElseThrow(() -> new NotFoundException("User with id " + id + " not found"));
    }

    /**
     * Проверяет уникальность email пользователя при обновлении.
     *
     * @param updatedUser  обновляемый пользователь
     * @param existingUser существующий пользователь
     *
     * @throws DuplicateException если email уже используется другим пользователем
     */
    public void validateUniqueEmailForUpdate(User updatedUser, User existingUser) {
        if (!existingUser.getEmail().equals(updatedUser.getEmail())) {
            if (userStorage.getUserByEmail(updatedUser.getEmail()).isPresent()) {
                throw new DuplicateException("Пользователь с таким email " + updatedUser.getEmail() + " уже " +
                        "существует");
            }
        }
    }

    /**
     * Проверяет уникальность логина пользователя при обновлении.
     *
     * @param updatedUser  обновляемый пользователь
     * @param existingUser существующий пользователь
     *
     * @throws DuplicateException если логин уже используется другим пользователем
     */
    public void validateUniqueLoginForUpdate(User updatedUser, User existingUser) {
        if (!existingUser.getLogin().equals(updatedUser.getLogin())) {
            if (userStorage.getUserByLogin(updatedUser.getLogin()).isPresent()) {
                throw new DuplicateException("Пользователь с таким логином " + updatedUser.getLogin() + " уже " +
                        "существует");
            }
        }
    }
}