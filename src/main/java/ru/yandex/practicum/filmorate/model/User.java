package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    public static final String LOGIN_PATTERN = "^[\\w\\p{IsCyrillic}]+$";
    public static final int LOGIN_MIN_LENGTH = 4;
    public static final int LOGIN_MAX_LENGTH = 20;

    private Long id;

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный email")
    private String email;

    @NotBlank(message = "Логин не может быть пустым")
    @Size(min = LOGIN_MIN_LENGTH, max = LOGIN_MAX_LENGTH, message = "Логин должен быть от 4 до 20 символов")
    @Pattern(regexp = LOGIN_PATTERN,
            message = "Логин может содержать только буквы (латинские и русские), цифры и символ подчеркивания")
    private String login;

    private String name;

    @Past(message = "Дата рождения не может быть в будущем")
    private LocalDate birthday;

    @Builder.Default
    private Set<Long> friends = new HashSet<>();

    public static User copyWithId(User source, Long newId) {
        return User.builder()
                .id(newId)
                .email(source.getEmail())
                .login(source.getLogin())
                .name(source.getName())
                .birthday(source.getBirthday())
                .friends(source.getFriends() != null ? new HashSet<>(source.getFriends()) : new HashSet<>())
                .build();
    }

    public String getName() {
        return name == null || name.isBlank() ? login : name;
    }
}