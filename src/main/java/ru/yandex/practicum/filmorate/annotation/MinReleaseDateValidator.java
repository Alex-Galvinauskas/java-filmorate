/**
 * Реализация валидатора для аннотации {@link ru.yandex.practicum.filmorate.annotation.MinReleaseDate}.
 * Проверяет, что переданная дата не раньше установленной минимальной даты.
 *
 * @see ru.yandex.practicum.filmorate.annotation.MinReleaseDate
 */
package ru.yandex.practicum.filmorate.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class MinReleaseDateValidator implements ConstraintValidator<MinReleaseDate, LocalDate> {

    private LocalDate minDate;

    @Override
    public void initialize(MinReleaseDate constraintAnnotation) {
        this.minDate = LocalDate.parse(constraintAnnotation.value());
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return !value.isBefore(minDate);
    }
}