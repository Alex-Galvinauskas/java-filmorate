package ru.yandex.practicum.filmorate.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Тестирование глобального обработчика исключений")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Обработка ValidationException возвращает статус 400 Bad Request")
    void testHandleValidationException_ReturnsBadRequest() {
        ValidationException exception = new ValidationException("Validation error");

        Map<String, String> response = exceptionHandler.handleValidationException(exception);

        assertNotNull(response);
        assertEquals("Ошибка валидации", response.get("error"));
        assertEquals("Validation error", response.get("message"));
    }

    @Test
    @DisplayName("Обработка NotFoundException возвращает статус 404 Not Found")
    void testHandleNotFoundException_ReturnsNotFound() {
        NotFoundException exception = new NotFoundException("Not found error");

        Map<String, String> response = exceptionHandler.handleNotFoundException(exception);

        assertNotNull(response);
        assertEquals("Объект не найден", response.get("error"));
        assertEquals("Not found error", response.get("message"));
    }

    @Test
    @DisplayName("Обработка DuplicateException возвращает статус 409 Conflict")
    void testHandleDuplicateException_ReturnsConflict() {
        DuplicateException exception = new DuplicateException("Duplicate error");

        Map<String, String> response = exceptionHandler.handleDuplicateException(exception);

        assertNotNull(response);
        assertEquals("Конфликт данных", response.get("error"));
        assertEquals("Duplicate error", response.get("message"));
    }

    @Test
    @DisplayName("Обработка общего Exception возвращает статус 500 Internal Server Error")
    void testHandleException_ReturnsInternalServerError() {
        Exception exception = new Exception("Internal error");

        Map<String, String> response = exceptionHandler.handleException(exception);

        assertNotNull(response);
        assertEquals("Внутренняя ошибка сервера", response.get("error"));
        assertEquals("Произошла непредвиденная ошибка", response.get("message"));
    }

    @Test
    @DisplayName("Обработка MethodArgumentNotValidException с ошибками полей возвращает детали валидации")
    void testHandleMethodArgumentNotValidException_ReturnsBadRequest() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "default message");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        Map<String, String> response = exceptionHandler.handleMethodArgumentNotValidException(exception);

        assertNotNull(response);
        assertEquals("Ошибка валидации", response.get("error"));
        assertEquals("field: default message", response.get("message"));
    }

    @Test
    @DisplayName("Обработка MethodArgumentNotValidException без ошибок полей возвращает сообщение по умолчанию")
    void testHandleMethodArgumentNotValidException_NoFieldErrors_ReturnsDefaultMessage() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        Map<String, String> response = exceptionHandler.handleMethodArgumentNotValidException(exception);

        assertNotNull(response);
        assertEquals("Ошибка валидации", response.get("error"));
        assertEquals("Неизвестная ошибка валидации", response.get("message"));
    }
}