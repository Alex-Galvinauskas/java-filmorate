package ru.yandex.practicum.filmorate.service.user.validation;

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
public class UserValidatorImpl implements UserValidatorRules {

    private final UserStorage userStorage;

    private static final String EMAIL_DUPLICATE_MESSAGE = "Пользователь с таким email {0} уже существует";
    private static final String LOGIN_DUPLICATE_MESSAGE = "Пользователь с таким логином {0} уже существует";
    private static final String USER_NOT_FOUND_MESSAGE = "Пользователь с id {0} не найден";

    /**
     * Выполняет все проверки для создания нового пользователя.
     *
     * @param user пользователь для создания
     */
    public void validateForCreate(User user) {
        validateUniqueEmailForCreate(user.getEmail());
        validateUniqueLoginForCreate(user.getLogin());
    }

    /**
     * Проверяет уникальность email при создании нового пользователя.
     *
     * @param email email для проверки
     *
     * @throws DuplicateException если email уже используется другим пользователем
     */
    public void validateUniqueEmailForCreate(String email) {
        if (userStorage.existsByEmail(email)) {
            String message = EMAIL_DUPLICATE_MESSAGE.replace("{0}", email);
            throw new DuplicateException(message);
        }
    }

    /**
     * Проверяет уникальность логина при создании нового пользователя.
     *
     * @param login логин для проверки
     *
     * @throws DuplicateException если логин уже используется другим пользователем
     */
    public void validateUniqueLoginForCreate(String login) {
        if (userStorage.existsByLogin(login)) {
            String message = LOGIN_DUPLICATE_MESSAGE.replace("{0}", login);
            throw new DuplicateException(message);
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
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE.replace("{0}",
                String.valueOf(id))));
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
            if (userStorage.existsByEmail(updatedUser.getEmail())) {
                String message = EMAIL_DUPLICATE_MESSAGE.replace("{0}", updatedUser.getEmail());
                throw new DuplicateException(message);
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
            if (userStorage.existsByLogin(updatedUser.getLogin())) {
                String message = LOGIN_DUPLICATE_MESSAGE.replace("{0}", updatedUser.getLogin());
                throw new DuplicateException(message);
            }
        }
    }
}