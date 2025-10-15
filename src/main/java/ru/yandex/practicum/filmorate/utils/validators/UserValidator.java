package ru.yandex.practicum.filmorate.utils.validators;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

@Component
public interface UserValidator {
    void validateForCreation(User user);

    void validateForUpdate(User user);

    User validateUserExist(Long id);
}
