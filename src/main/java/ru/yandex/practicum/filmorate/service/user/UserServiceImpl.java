/**
 * Реализация сервисного слоя для работы с пользователями.
 * Содержит бизнес-логику приложения для операций с пользователями.
 * Обеспечивает проверку уникальности email и логина, нормализацию данных пользователя.
 *
 * @see ru.yandex.practicum.filmorate.service.user.UserService
 * @see ru.yandex.practicum.filmorate.managment.UserStorage
 * @see User
 */
package ru.yandex.practicum.filmorate.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.UserStorage;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.user.validation.UserValidatorRules;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserStorage userStorage;
    private final UserValidatorRules userValidator;
    private static final boolean DEFAULT_NAME_FROM_LOGIN = true;

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

        userValidator.validateForCreate(user);
        normalizeUser(user);

        return userStorage.createUser(user);
    }

    /**
     * Добавляет обоих пользователей в список друзей.
     *
     * @param userId индентификатор пользователя, который добавляется в друзья
     * @param friendId идентификатор друга, который добавляется в друзья
     *
     *throws NotFoundException если один или оба пользователя не существует
     */
    @Override
    public void addFriend(Long userId, Long friendId) {
        log.info("Добавление пользователя {} в друзья пользователя {}.", friendId, userId);

        User user = userValidator.validateUserExist(userId);
        User friend = userValidator.validateUserExist(friendId);

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        log.debug("Пользователи {} и {} теперь в друзья.", userId, friendId);
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
     * Возвращает список друзей пользователя.
     * @param userId идентификатор пользователя, для которого получаем список друзей
     * @return список друзей пользователя
     *
     * @throws NotFoundException если пользователь с указанным ID не найден
     */
    @Override
    public List<User> getFriends(Long userId) {
        log.info("Получение списка друзей пользователя {}.", userId);

        User user = userValidator.validateUserExist(userId);

        return user.getFriends().stream()
                .map(userStorage::getUserById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }


    /**
     * Возвращает список общих друзей двух пользователей.
     * @param userId1 id первого пользователя
     * @param userId2 id второго пользователя
     * @return список общих друзей двух пользователей
     *
     * @throws NotFoundException если один или оба пользователя не существует
     */
    @Override
    public List<User> getCommonFriends(Long userId1, Long userId2) {
        log.info("Получение общих друзей пользователей {}, {}.", userId1, userId2);

        User user1 = userValidator.validateUserExist(userId1);
        User user2 = userValidator.validateUserExist(userId2);

        Set<Long> commonFriendsIds = new HashSet<>(user1.getFriends());
        commonFriendsIds.retainAll(user2.getFriends());

        return commonFriendsIds.stream()
                .map(userStorage::getUserById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
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


    /**
     * Удаляет пользователей из друзей друг у друга.
     *
     * @param userId идентификатор пользователя, у которого удаляем друга
     * @param friendId идентификатор друга, которого удаляем
     *
     * @throws NotFoundException если один или оба пользователя не существует
     */
    @Override
    public void removeFriend(Long userId, Long friendId) {
        log.info("Удаление пользователя {} из друзей пользователя {}.", friendId, userId);

        User user = userValidator.validateUserExist(userId);
        User friend = userValidator.validateUserExist(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        log.debug("Пользователи {} и {} больше не друзья.", userId, friendId);
    }

    private void normalizeUser(User user) {
        if (DEFAULT_NAME_FROM_LOGIN && (user.getName() == null || user.getName().isBlank())) {
            user.setName(user.getLogin());
            log.debug("Для пользователя {} установлено имя из логина: {}", user.getLogin(), user.getName());
        }
    }
}