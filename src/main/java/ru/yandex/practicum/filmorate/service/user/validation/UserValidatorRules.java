package ru.yandex.practicum.filmorate.service.user.validation;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

@Component
public interface UserValidatorRules {
    void validateForCreate(User user);

    void validateForUpdate(User user);

    User validateUserExist(Long id);
}
