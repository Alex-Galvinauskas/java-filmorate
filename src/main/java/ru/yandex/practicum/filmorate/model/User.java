package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный email")
    private String email;

    @NotBlank(message = "Логин не может быть пустым")
    @Size(min = 4, max = 20, message = "Логин должен быть от 4 до 20 символов")
    @Pattern(regexp = "^[\\w\\p{IsCyrillic}]+$",
            message = "Логин может содержать только буквы (латинские и русские), цифры и символ подчеркивания")
    private String login;

    private String name;

    @Past(message = "Дата рождения не может быть в будущем")
    private LocalDate birthday;

    public static User copyWithId(User source, Long newId) {
        return User.builder()
                .id(newId)
                .email(source.getEmail())
                .login(source.getLogin())
                .name(source.getName())
                .birthday(source.getBirthday())
                .build();
    }

    public String getName() {
        return name == null || name.isBlank() ? login : name;
    }

}