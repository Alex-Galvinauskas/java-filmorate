package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectorDTO {
    private Long id;

    @NotBlank(message = "Имя режиссера не может быть пустым")
    @Size(min = 1, max = 100, message = "Имя режиссера должно быть от 1 до 100 символов")
    private String name;
}
