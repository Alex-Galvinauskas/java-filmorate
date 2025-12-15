/**
 * Реализация сервисного слоя для работы с пользователями.
 * Содержит бизнес-логику приложения для операций с пользователями.
 * Обеспечивает проверку уникальности email и логина, нормализацию данных пользователя.
 *
 * @see ru.yandex.practicum.filmorate.service.user.UserService
 * @see ru.yandex.practicum.filmorate.managment.inMemory.UserStorage
 * @see User
 */
package ru.yandex.practicum.filmorate.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.UserDTO;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.UserDbStorage;
import ru.yandex.practicum.filmorate.mapper.UserMapper;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.user.validation.UserValidatorRules;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserDbStorage userDbStorage;
    private final UserValidatorRules userValidator;
    private final UserMapper userMapper;
    private static final boolean DEFAULT_NAME_FROM_LOGIN = true;

    /**
     * Создает нового пользователя с проверкой уникальности.
     * Проверяет уникальность email и логина, устанавливает имя из логина если имя не указано.
     * Выполняет нормализацию данных пользователя.
     *
     * @param userDTO пользователь для создания
     * @return созданный пользователь
     * @throws DuplicateException если пользователь с таким email или логином уже существует
     */
    @Override
    public UserDTO createUser(UserDTO userDTO) {
        log.debug("Создание нового пользователя");

        userValidator.validateForCreate(userMapper.toEntity(userDTO));
        User user = userMapper.toEntity(userDTO);
        normalizeUser(user);

        User createdUser = userDbStorage.createUser(user);
        return userMapper.toDTO(createdUser);
    }

    /**
     * Добавляет обоих пользователей в список друзей.
     *
     * @param userId   индентификатор пользователя, который добавляется в друзья
     * @param friendId идентификатор друга, который добавляется в друзья
     *                 <p>
     *                 throws NotFoundException если один или оба пользователя не существует
     */
    @Override
    public void addFriend(Long userId, Long friendId) {
        log.debug("Добавление пользователя {} в друзья пользователя {}", friendId, userId);

        userValidator.validateUserExist(userId);
        userValidator.validateUserExist(friendId);

        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Нельзя добавить себя в друзья");
        }

        userDbStorage.addFriend(userId, friendId);
    }

    /**
     * Возвращает список всех пользователей.
     *
     * @return список всех пользователей
     */
    @Override
    public List<UserDTO> getAllUsers() {
        log.debug("Получение списка всех пользователей");
        return userDbStorage.getAllUsers().stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Находит пользователя по идентификатору.
     *
     * @param id идентификатор пользователя
     * @return найденный пользователь
     * @throws NotFoundException если пользователь с указанным ID не найден
     */
    @Override
    public UserDTO getUserById(Long id) {
        User user = userValidator.validateUserExist(id);
        return userMapper.toDTO(user);
    }


    /**
     * Возвращает список друзей пользователя.
     *
     * @param userId идентификатор пользователя, для которого получаем список друзей
     * @return список друзей пользователя
     * @throws NotFoundException если пользователь с указанным ID не найден
     */
    @Override
    public List<UserDTO> getFriends(Long userId) {
        log.debug("Получение списка друзей пользователя {}", userId);

        userValidator.validateUserExist(userId);

        return userDbStorage.getFriends(userId).stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }


    /**
     * Возвращает список общих друзей двух пользователей.
     *
     * @param userId1 id первого пользователя
     * @param userId2 id второго пользователя
     * @return список общих друзей двух пользователей
     * @throws NotFoundException если один или оба пользователя не существует
     */
    @Override
    public List<UserDTO> getCommonFriends(Long userId1, Long userId2) {
        log.debug("Получение общих друзей пользователей {}, {}", userId1, userId2);

        userValidator.validateUserExist(userId1);
        userValidator.validateUserExist(userId2);

        return userDbStorage.getCommonFriends(userId1, userId2).stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Обновляет существующего пользователя.
     * Проверяет существование пользователя, уникальность новых email и логина.
     * Устанавливает имя из логина если имя не указано, выполняет нормализацию данных.
     *
     * @param userDTO пользователь с обновленными данными
     * @return обновленный пользователь
     * @throws NotFoundException  если пользователь с указанным ID не найден
     * @throws DuplicateException если пользователь с новым email или логином уже существует
     */
    @Override
    public UserDTO updateUser(UserDTO userDTO) {
        log.debug("Обновление данных пользователя");

        userValidator.validateForUpdate(userMapper.toEntity(userDTO));
        User user = userMapper.toEntity(userDTO);
        normalizeUser(user);

        User updatedUser = userDbStorage.updateUser(user);
        return userMapper.toDTO(updatedUser);
    }


    /**
     * Удаляет пользователей из друзей друг у друга.
     *
     * @param userId   идентификатор пользователя, у которого удаляем друга
     * @param friendId идентификатор друга, которого удаляем
     * @throws NotFoundException если один или оба пользователя не существует
     */
    @Override
    public void removeFriend(Long userId, Long friendId) {
        log.debug("Удаление пользователя {} из друзей пользователя {}", friendId, userId);

        userValidator.validateUserExist(userId);
        userValidator.validateUserExist(friendId);

        userDbStorage.removeFriend(userId, friendId);
    }

    private void normalizeUser(User user) {
        if (DEFAULT_NAME_FROM_LOGIN && (user.getName() == null || user.getName().isBlank())) {
            user.setName(user.getLogin());
            log.debug("Для пользователя {} установлено имя из логина: {}", user.getLogin(), user.getName());
        }
    }

    @Override
    public void deleteUser(Long userId) {
        log.debug("Начало удаления пользователя с ID: {}", userId);

        // Проверяем существование пользователя
        UserDTO user = getUserById(userId);
        log.debug("Пользователь найден: '{}' (ID: {})", user.getLogin(), userId);

        try {
            // Удаляем пользователя через storage (там уже удаляются зависимости)
            userDbStorage.deleteUser(userId);
            log.info("Пользователь '{}' (ID: {}) успешно удален", user.getLogin(), userId);
        } catch (Exception e) {
            log.error("Ошибка при удалении пользователя с ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Не удалось удалить пользователя", e);
        }
    }
}
