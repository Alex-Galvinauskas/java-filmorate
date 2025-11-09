package ru.yandex.practicum.filmorate.managment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final AtomicLong nextUserId;
    private static final long INITIAL_USER_ID = 1L;

    public InMemoryUserStorage(@Value("${app.storage.user.id.initial:1}") long initialId) {
        this.nextUserId = new AtomicLong(initialId);
    }

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
        emailToUserId.put(userToSave.getEmail().toLowerCase(), userId);
        loginToUserId.put(userToSave.getLogin().toLowerCase(), userId);

        log.info("Создан новый пользователь: {} (ID: {})", userToSave.getLogin(), userId);
        return userToSave;
    }

    /**
     * Возвращает список всех пользователей в хранилище.
     *
     * @return неизменяемый список всех пользователей
     */
    public List<User> getAllUsers() {
        log.debug("Получение списка всех пользователей. Текущее количество: {}", users.size());
        return List.copyOf(users.values());
    }

    /**
     * Находит пользователя по его идентификатору.
     *
     * @param id идентификатор пользователя
     * @return Optional с найденным пользователем или пустой Optional если пользователь не найден
     */
    public Optional<User> getUserById(Long id) {
        if (id == null) {
            log.debug("Попытка поиска пользователя с null ID");
            return Optional.empty();
        }

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
        if (email == null) {
            log.debug("Попытка поиска пользователя с null email");
            return Optional.empty();
        }

        Long userId = emailToUserId.get(email.toLowerCase());
        User user = userId != null ? users.get(userId) : null;
        log.debug("Поиск пользователя по email: {}. Найден: {}", email, user != null);
        return Optional.ofNullable(user);
    }

    /**
     * Находит пользователя по логину.
     * Поиск выполняется без учета регистра.
     *
     * @param login логин пользователя
     * @return Optional с найденным пользователем или пустой Optional если пользователь не найден
     */
    public Optional<User> getUserByLogin(String login) {
        if (login == null) {
            log.debug("Попытка поиска пользователя с null логином");
            return Optional.empty();
        }

        Long userId = loginToUserId.get(login.toLowerCase());
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
        User existingUser = users.get(userId);

        if (existingUser == null) {
            log.warn("Попытка обновления несуществующего пользователя с ID: {}", userId);
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
        
        User userToUpdate = User.builder()
                .id(userId)
                .email(user.getEmail())
                .login(user.getLogin())
                .name(user.getName())
                .birthday(user.getBirthday())
                .friendships(user.getFriendships() != null ? user.getFriendships() : existingUser.getFriendships())
                .build();

        if (!existingUser.getEmail().equalsIgnoreCase(userToUpdate.getEmail())) {
            emailToUserId.remove(existingUser.getEmail().toLowerCase());
            emailToUserId.put(userToUpdate.getEmail().toLowerCase(), userId);
            log.debug("Обновлен индекс email для пользователя ID: {}", userId);
        }

        if (!existingUser.getLogin().equalsIgnoreCase(userToUpdate.getLogin())) {
            loginToUserId.remove(existingUser.getLogin().toLowerCase());
            loginToUserId.put(userToUpdate.getLogin().toLowerCase(), userId);
            log.debug("Обновлен индекс логина для пользователя ID: {}", userId);
        }

        users.put(userId, userToUpdate);
        log.info("Обновлен пользователь: {} (ID: {})", userToUpdate.getLogin(), userId);
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
        return email != null && emailToUserId.containsKey(email.toLowerCase());
    }

    /**
     * Проверяет существование пользователя по логину.
     * Поиск выполняется без учета регистра.
     *
     * @param login логин пользователя
     * @return true если пользователь с таким логином существует, false в противном случае
     */
    public boolean existsByLogin(String login) {
        return login != null && loginToUserId.containsKey(login.toLowerCase());
    }
}