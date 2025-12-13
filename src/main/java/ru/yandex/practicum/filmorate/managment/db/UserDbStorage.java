package ru.yandex.practicum.filmorate.managment.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.managment.inMemory.UserStorage;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Primary
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public User createUser(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getName());
            stmt.setDate(4, user.getBirthday() != null ? Date.valueOf(user.getBirthday()) : null);
            return stmt;
        }, keyHolder);

        Long userId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        user.setId(userId);

        user.setFriends(new HashSet<>());

        log.info("Создан новый пользователь в БД: {} (ID: {})", user.getLogin(), userId);
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users ORDER BY id";
        log.debug("Получение всех пользователей из БД");
        List<User> users = jdbcTemplate.query(sql, new UserRowMapper());
        loadFriendsForUsers(users);
        return users;
    }

    @Override
    public Optional<User> getUserById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        log.debug("Поиск пользователя по ID: {}", id);
        List<User> result = jdbcTemplate.query(sql, new UserRowMapper(), id);

        if (result.isEmpty()) {
            return Optional.empty();
        }

        User user = result.getFirst();
        loadFriendsForUsers(Collections.singletonList(user));
        return Optional.of(user);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        log.debug("Поиск пользователя по email: {}", email);
        List<User> result = jdbcTemplate.query(sql, new UserRowMapper(), email);

        if (result.isEmpty()) {
            return Optional.empty();
        }

        User user = result.getFirst();
        loadFriendsForUsers(Collections.singletonList(user));
        return Optional.of(user);
    }

    @Override
    public Optional<User> getUserByLogin(String login) {
        String sql = "SELECT * FROM users WHERE login = ?";
        log.debug("Поиск пользователя по логину: {}", login);
        List<User> result = jdbcTemplate.query(sql, new UserRowMapper(), login);

        if (result.isEmpty()) {
            return Optional.empty();
        }

        User user = result.getFirst();
        loadFriendsForUsers(Collections.singletonList(user));
        return Optional.of(user);
    }

    @Override
    public User updateUser(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";

        int updated = jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday() != null ? Date.valueOf(user.getBirthday()) : null,
                user.getId());

        if (updated == 0) {
            throw new RuntimeException("Пользователь с ID " + user.getId() + " не найден");
        }

        log.info("Обновлен пользователь в БД: {} (ID: {})", user.getLogin(), user.getId());
        return user;
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByLogin(String login) {
        String sql = "SELECT COUNT(*) FROM users WHERE login = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, login);
        return count != null && count > 0;
    }

    public void addFriend(Long userId, Long friendId) {
        String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'PENDING')";
        jdbcTemplate.update(sql, userId, friendId);
        log.debug("Отправлена заявка в друзья от пользователя {} пользователю {}", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
        log.debug("Удалена дружба между пользователями {} и {}", userId, friendId);
    }

    public List<User> getFriends(Long userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendships f ON u.id = f.friend_id " +
                "WHERE f.user_id = ?";
        log.debug("Получение друзей пользователя {}", userId);
        return jdbcTemplate.query(sql, new UserRowMapper(), userId);
    }

    public List<User> getCommonFriends(Long userId1, Long userId2) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendships f1 ON u.id = f1.friend_id " +
                "JOIN friendships f2 ON u.id = f2.friend_id " +
                "WHERE f1.user_id = ? AND f2.user_id = ?";
        log.debug("Получение общих друзей пользователей {} и {}", userId1, userId2);
        return jdbcTemplate.query(sql, new UserRowMapper(), userId1, userId2);
    }

    /**
     * Загружает друзей для списка пользователей
     */
    private void loadFriendsForUsers(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();

        String sql = "SELECT user_id, friend_id FROM friendships WHERE user_id IN (" +
                userIds.stream().map(id -> "?")
                        .collect(Collectors.joining(",")) + ")";

        Map<Long, Set<Long>> friendsMap = new HashMap<>();

        jdbcTemplate.query(sql,
                new ArgumentPreparedStatementSetter(userIds.toArray()),
                (rs) -> {
                    Long userId = rs.getLong("user_id");
                    Long friendId = rs.getLong("friend_id");
                    friendsMap.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
                });

        for (User user : users) {
            Set<Long> friends = friendsMap.getOrDefault(user.getId(), new HashSet<>());
            user.setFriends(friends);
            log.debug("Загружено {} друзей для пользователя {}", friends.size(), user.getId());
        }
    }

    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            return User.builder()
                    .id(rs.getLong("id"))
                    .email(rs.getString("email"))
                    .login(rs.getString("login"))
                    .name(rs.getString("name"))
                    .birthday(rs.getDate("birthday") != null ?
                            rs.getDate("birthday").toLocalDate() : null)
                    .friends(new HashSet<>())
                    .build();
        }
    }

    @Override
    public void deleteUser(Long userId) {
        log.debug("Удаление пользователя с ID: {} из БД", userId);

        if (!existsById(userId)) {
            log.warn("Попытка удаления несуществующего пользователя с ID: {}", userId);
            throw new RuntimeException("Пользователь с ID " + userId + " не найден");
        }


        removeAllLikesByUserId(userId);

        removeAllFriendshipsByUserId(userId);

        String deleteUserSql = "DELETE FROM users WHERE id = ?";
        int rowsDeleted = jdbcTemplate.update(deleteUserSql, userId);

        if (rowsDeleted > 0) {
            log.info("Пользователь с ID {} успешно удален", userId);
        } else {
            log.warn("Пользователь с ID {} не был удален", userId);
        }
    }

    public void removeAllFriendshipsByUserId(Long userId) {
        String sql = "DELETE FROM friendships WHERE user_id = ? OR friend_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, userId, userId);
        log.debug("Удалено {} дружеских связей для пользователя с ID: {}", rowsDeleted, userId);
    }

    public void removeAllLikesByUserId(Long userId) {
        String sql = "DELETE FROM likes WHERE user_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, userId);
        log.debug("Удалено {} лайков для пользователя с ID: {}", rowsDeleted, userId);
    }

    public void removeUserFromAllFriends(Long userId) {
        String sql = "DELETE FROM friendships WHERE friend_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, userId);
        log.debug("Пользователь с ID {} удален из друзей у {} других пользователей",
                userId, rowsDeleted);
    }
}
    public boolean hasLikes(Long userId) {
        // Проверяем, есть ли лайки у пользователя
        String sql = "SELECT COUNT(*) FROM likes WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null && count > 0;
    }
}
