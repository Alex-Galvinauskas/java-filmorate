package ru.yandex.practicum.filmorate.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.UserStorage;
import ru.yandex.practicum.filmorate.model.Friendship;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
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
     * @return созданный пользователь
     */
    public User createUser(User user) {
        log.info("Создание нового пользователя: {}", user.getLogin());

        userValidator.validateForCreate(user);
        normalizeUser(user);

        User createdUser = userStorage.createUser(user);
        log.info("Пользователь создан с ID: {}", createdUser.getId());
        return createdUser;
    }

    /**
     * Добавляет дружбу между пользователями.
     * Создает неподтвержденную дружбу, где userId инициирует запрос, а friendId получает его.
     *
     * @param userId   идентификатор пользователя, который отправляет запрос дружбы
     * @param friendId идентификатор друга, который получает запрос дружбы
     * @throws NotFoundException если один или оба пользователя не существует
     */
    @Override
    public void addFriend(Long userId, Long friendId) {
        log.info("Добавление пользователя {} в друзья пользователя {}.", friendId, userId);

        if (userId.equals(friendId)) {
            log.warn("Попытка добавить самого себя в друзья: {}", userId);
            return;
        }

        User user = userValidator.validateUserExist(userId);
        User friend = userValidator.validateUserExist(friendId);

        if (isFriend(user, friendId)) {
            log.warn("Пользователи {} и {} уже являются друзьями.", userId, friendId);
            return;
        }

        Friendship friendship = Friendship.builder()
                .userId(userId)
                .friendId(friendId)
                .status(FriendshipStatus.UNCONFIRMED)
                .build();

        user.getFriendships().add(friendship);
        userStorage.updateUser(user);

        log.debug("Запрос дружбы отправлен от пользователя {} пользователю {}.", userId, friendId);
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
     * @return найденный пользователь
     * @throws NotFoundException если пользователь с указанным ID не найден
     */
    public User getUserById(Long id) {
        return userValidator.validateUserExist(id);
    }

    /**
     * Возвращает список друзей пользователя.
     * @param userId идентификатор пользователя, для которого получаем список друзей
     * @return список друзей пользователя
     * @throws NotFoundException если пользователь с указанным ID не найден
     */
    @Override
    public List<User> getFriends(Long userId) {
        log.info("Получение списка друзей пользователя {}.", userId);

        User user = userValidator.validateUserExist(userId);

        return user.getFriendships().stream()
                .map(Friendship::getFriendId)
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
     * @throws NotFoundException если один или оба пользователя не существует
     */
    @Override
    public List<User> getCommonFriends(Long userId1, Long userId2) {
        log.info("Получение общих друзей пользователей {}, {}.", userId1, userId2);

        User user1 = userValidator.validateUserExist(userId1);
        User user2 = userValidator.validateUserExist(userId2);

        Set<Long> friends1 = user1.getFriendships().stream()
                .map(Friendship::getFriendId)
                .collect(Collectors.toSet());

        Set<Long> friends2 = user2.getFriendships().stream()
                .map(Friendship::getFriendId)
                .collect(Collectors.toSet());

        Set<Long> commonFriendsIds = new HashSet<>(friends1);
        commonFriendsIds.retainAll(friends2);

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
     * @return обновленный пользователь
     * @throws NotFoundException если пользователь с указанным ID не найден
     */
    public User updateUser(User user) {
        log.info("Обновление данных пользователя с ID: {}", user.getId());

        userValidator.validateForUpdate(user);
        normalizeUser(user);

        User updatedUser = userStorage.updateUser(user);
        log.info("Пользователь с ID: {} успешно обновлен", updatedUser.getId());
        return updatedUser;
    }

    /**
     * Удаляет дружбу между пользователями.
     *
     * @param userId   идентификатор пользователя, у которого удаляем друга
     * @param friendId идентификатор друга, которого удаляем
     * @throws NotFoundException если один или оба пользователя не существует
     */
    @Override
    public void removeFriend(Long userId, Long friendId) {
        log.info("Удаление пользователя {} из друзей пользователя {}.", friendId, userId);

        User user = userValidator.validateUserExist(userId);
        User friend = userValidator.validateUserExist(friendId);

        boolean removedFromUser = user.getFriendships().removeIf(friendship ->
                friendship.getFriendId().equals(friendId));

        boolean removedFromFriend = friend.getFriendships().removeIf(friendship ->
                friendship.getFriendId().equals(userId));

        if (removedFromUser) {
            userStorage.updateUser(user);
        }
        if (removedFromFriend) {
            userStorage.updateUser(friend);
        }

        log.debug("Пользователи {} и {} больше не друзья.", userId, friendId);
    }

    /**
     * Подтверждает дружбу между пользователями.
     *
     * @param userId   идентификатор пользователя, который подтверждает дружбу
     * @param friendId идентификатор друга
     * @throws NotFoundException если один или оба пользователя не существует
     */
    public void confirmFriendship(Long userId, Long friendId) {
        log.info("Подтверждение дружбы пользователем {} с пользователем {}.", userId, friendId);

        User user = userValidator.validateUserExist(userId);
        User friend = userValidator.validateUserExist(friendId);

        Optional<Friendship> userFriendship = user.getFriendships().stream()
                .filter(f -> f.getFriendId().equals(friendId))
                .findFirst();

        Optional<Friendship> friendFriendship = friend.getFriendships().stream()
                .filter(f -> f.getFriendId().equals(userId))
                .findFirst();

        if (userFriendship.isPresent() && friendFriendship.isPresent()) {
            userFriendship.get().setStatus(FriendshipStatus.CONFIRMED);
            friendFriendship.get().setStatus(FriendshipStatus.CONFIRMED);

            userStorage.updateUser(user);
            userStorage.updateUser(friend);

            log.debug("Дружба между пользователями {} и {} подтверждена.", userId, friendId);
        } else {
            throw new NotFoundException("Запрос дружбы между пользователями " + userId + " и " + friendId + " не найден");
        }
    }

    private void normalizeUser(User user) {
        if (DEFAULT_NAME_FROM_LOGIN && (user.getName() == null || user.getName().isBlank())) {
            user.setName(user.getLogin());
            log.debug("Для пользователя {} установлено имя из логина: {}", user.getLogin(), user.getName());
        }

        if (user.getFriendships() == null) {
            user.setFriendships(java.util.concurrent.ConcurrentHashMap.newKeySet());
        }
    }

    /**
     * Проверяет, является ли пользователь другом.
     *
     * @param user     пользователь для проверки
     * @param friendId идентификатор потенциального друга
     * @return true если пользователь является другом
     */
    private boolean isFriend(User user, Long friendId) {
        return user.getFriendships().stream()
                .anyMatch(friendship -> friendship.getFriendId().equals(friendId));
    }
}