/**
 * Реализация хранилища пользователей в памяти.
 * Хранит данные о пользователях в ConcurrentHashMap и обеспечивает потокобезопасные операции.
 * Поддерживает дополнительные индексы для быстрого поиска по email и логину.
 * Генерирует уникальные идентификаторы для новых пользователей с помощью AtomicLong.
 *
 * @see ru.yandex.practicum.filmorate.managment.UserStorage
 * @see User
 */
package ru.yandex.practicum.filmorate.managment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final Map<String, Long> emailToUserId = new ConcurrentHashMap<>();
    private final Map<String, Long> loginToUserId = new ConcurrentHashMap<>();
    private final AtomicLong nextUserId = new AtomicLong(1);

    /**
     * Создает нового пользователя в хранилище.
     * Присваивает пользователю уникальный идентификатор и обновляет индексы.
     *
     * @param user пользователь для создания
     * @return созданный пользователь с присвоенным идентификатором
     */
    public User createUser(User user) {
        Long userId = nextUserId.getAndIncrement();
        User userToSave = User.copyWithId(user, userId);

        users.put(userId, userToSave);
        emailToUserId.put(userToSave.getEmail(), userId);
        loginToUserId.put(userToSave.getLogin(), userId);

        log.info("Создан новый пользователь {}", userToSave);
        return userToSave;
    }

    /**
     * Возвращает список всех пользователей в хранилище.
     *
     * @return неизменяемый список всех пользователей
     */
    public List<User> getAllUsers() {
        log.debug("Получение списка всех пользователей");
        return List.copyOf(users.values());
    }

    /**
     * Находит пользователя по его идентификатору.
     *
     * @param id идентификатор пользователя
     * @return Optional с найденным пользователем или пустой Optional если пользователь не найден
     */
    public Optional<User> getUserById(Long id) {
        User user = users.get(id);
        log.debug("Поиск пользователя по ID: {}. Найден: {}", id, user != null);
        return Optional.ofNullable(user);
    }

    /**
     * Находит пользователя по email.
     * Поиск выполняется без учета регистра.
     *
     * @param email email пользователя
     * @return Optional с найденным пользователем или пустой Optional если пользователь не найден
     */
    public Optional<User> getUserByEmail(String email) {
        Long userId = emailToUserId.get(email.toLowerCase());
        User user = userId != null ? users.get(userId) : null;
        log.debug("Поиск пользователя по email: {}. Найден: {}", email, user != null);
        return Optional.ofNullable(user);
    }

    /**
     * Находит пользователя по логину.
     *
     * @param login логин пользователя
     * @return Optional с найденным пользователем или пустой Optional если пользователь не найден
     */
    public Optional<User> getUserByLogin(String login) {
        Long userId = loginToUserId.get(login);
        User user = userId != null ? users.get(userId) : null;
        log.debug("Поиск пользователя по логину: {}. Найден: {}", login, user != null);
        return Optional.ofNullable(user);
    }

    /**
     * Обновляет существующего пользователя в хранилище.
     * Обновляет индексы email и логина при их изменении.
     *
     * @param user пользователь с обновленными данными
     * @return обновленный пользователь
     * @throws NotFoundException если пользователь с указанным ID не найден
     */
    public User updateUser(User user) {
        Long userId = user.getId();
        if (!users.containsKey(userId)) {
            log.warn("Попытка обновления несуществующего пользователя с ID: {}", userId);
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        User userToUpdate = User.copyWithId(user, userId);
        User existingUser = users.get(userId);

        if (!existingUser.getEmail().equals(userToUpdate.getEmail())) {
            emailToUserId.remove(existingUser.getEmail());
            emailToUserId.put(userToUpdate.getEmail(), userId);
        }

        if (!existingUser.getLogin().equals(userToUpdate.getLogin())) {
            loginToUserId.remove(existingUser.getLogin());
            loginToUserId.put(userToUpdate.getLogin(), userId);
        }

        users.put(userId, userToUpdate);
        log.info("Обновлен пользователь: {} (ID: {})", user.getLogin(), userId);
        return userToUpdate;
    }

    /**
     * Проверяет существование пользователя по идентификатору.
     *
     * @param id идентификатор пользователя
     * @return true если пользователь существует, false в противном случае
     */
    public boolean existsById(Long id) {
        return users.containsKey(id);
    }

    /**
     * Проверяет существование пользователя по email.
     * Поиск выполняется без учета регистра.
     *
     * @param email email пользователя
     * @return true если пользователь с таким email существует, false в противном случае
     */
    public boolean existsByEmail(String email) {
        return emailToUserId.containsKey(email);
    }

    /**
     * Проверяет существование пользователя по логину.
     *
     * @param login логин пользователя
     * @return true если пользователь с таким логином существует, false в противном случае
     */
    public boolean existsByLogin(String login) {
        return loginToUserId.containsKey(login);
    }
}
