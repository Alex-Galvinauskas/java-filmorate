package ru.yandex.practicum.filmorate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.filmorate.dto.UserDTO;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "friends", expression = "java(mapFriendsToDTO(user.getFriends()))")
    UserDTO toDTO(User user);

    @Mapping(target = "friends", expression = "java(mapFriendsToEntity(userDTO.getFriends()))")
    User toEntity(UserDTO userDTO);

    default Set<Long> mapFriendsToDTO(Set<Long> friends) {
        if (friends == null || friends.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(friends);
    }

    default Set<Long> mapFriendsToEntity(Set<Long> friends) {
        if (friends == null || friends.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(friends);
    }
}