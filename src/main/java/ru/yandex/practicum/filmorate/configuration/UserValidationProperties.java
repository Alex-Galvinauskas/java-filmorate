package ru.yandex.practicum.filmorate.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.validation.user")
@Data
public class UserValidationProperties {
    private Login login = new Login();
    private Name name = new Name();
    private Messages messages = new Messages();

    @Data
    public static class Login {
        private Integer minLength = 4;
        private Integer maxLength = 20;
        private String pattern = "^[\\w\\p{IsCyrillic}]+$";
    }

    @Data
    public static class Name {
        private Boolean defaultFromLogin = true;
    }

    @Data
    public static class Messages {
        private String emailDuplicate = "Пользователь с таким email {0} уже существует";
        private String loginDuplicate = "Пользователь с таким логином {0} уже существует";
        private String notFound = "Пользователь с id {0} не найден";
    }
}