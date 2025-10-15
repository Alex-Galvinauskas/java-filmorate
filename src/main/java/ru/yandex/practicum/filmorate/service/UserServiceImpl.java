/**
 * Реализация сервисного слоя для работы с пользователями.
 * Содержит бизнес-логику приложения для операций с пользователями.
 * Обеспечивает проверку уникальности email и логина, нормализацию данных пользователя.
 *
 * @see ru.yandex.practicum.filmorate.service.UserService
 * @see ru.yandex.practicum.filmorate.managment.UserStorage
 * @see User
 */
package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.UserStorage;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.utils.validators.UserValidator;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserStorage userStorage;
    private final UserValidator userValidator;

    /**
     * Создает нового пользователя с проверкой уникальности.
     * Проверяет уникальность email и логина, устанавливает имя из логина если имя не указано.
     * Выполняет нормализацию данных пользователя.
     *
     * @param user пользователь для создания
     *
     * @return созданный пользователь
     *
     * @throws DuplicateException если пользователь с таким email или логином уже существует
     */
    public User createUser(User user) {
        log.info("Создание нового пользователя.");

       userValidator.validateForCreation(user);
        normalizeUser(user);

        return userStorage.createUser(user);
    }

    /**
     * Возвращает список всех пользователей.
     *
     * @return список всех пользователей
     */
    public List<User> getAllUsers() {
        log.info("Получение списка всех пользователей.");
        return userStorage.getAllUsers();
    }

    /**
     * Находит пользователя по идентификатору.
     *
     * @param id идентификатор пользователя
     *
     * @return найденный пользователь
     *
     * @throws NotFoundException если пользователь с указанным ID не найден
     */
    public User getUserById(Long id) {
        return userValidator.validateUserExist(id);
    }

    /**
     * Обновляет существующего пользователя.
     * Проверяет существование пользователя, уникальность новых email и логина.
     * Устанавливает имя из логина если имя не указано, выполняет нормализацию данных.
     *
     * @param user пользователь с обновленными данными
     *
     * @return обновленный пользователь
     *
     * @throws NotFoundException  если пользователь с указанным ID не найден
     * @throws DuplicateException если пользователь с новым email или логином уже существует
     */
    public User updateUser(User user) {
        log.info("Обновление данных пользователя.");

        userValidator.validateForUpdate(user);
        normalizeUser(user);

        return userStorage.updateUser(user);
    }

    private void normalizeUser(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Для пользователя {} установлено имя из логина: {}", user.getLogin(), user.getName());
        }
    }
}