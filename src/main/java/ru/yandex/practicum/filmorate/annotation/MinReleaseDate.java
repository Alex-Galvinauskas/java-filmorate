/**
 * Аннотация для валидации даты выхода фильма.
 * Проверяет, что дата выхода фильма не раньше указанной минимальной даты.
 * По умолчанию минимальная дата установлена на 28 декабря 1895 года -
 * день первого публичного показа фильма братьями Люмьер.
 *
 * @author Автор
 * @version 1.0
 * @see ru.yandex.practicum.filmorate.annotation.MinReleaseDateValidator
 */
package ru.yandex.practicum.filmorate.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinReleaseDateValidator.class)
@Documented
public @interface MinReleaseDate {
    String message() default "Дата выхода фильма не может быть раньше 28 декабря 1895 года";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String value() default "1895-12-28";
}