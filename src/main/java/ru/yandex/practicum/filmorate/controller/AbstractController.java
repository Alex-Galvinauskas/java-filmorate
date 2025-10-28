package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Абстрактный базовый контроллер для операций CRUD.
 * Предоставляет общую логику для всех контроллеров сущностей.
 *
 * @param <T> тип сущности
 * @param <S> тип сервиса
 */
@Slf4j
public abstract class AbstractController<T, S> {

    protected final S service;
    protected final String entityName;

    protected AbstractController(S service, String entityName) {
        this.service = service;
        this.entityName = entityName;
    }

    /**
     * Создает новую сущность.
     *
     * @param entity объект сущности для создания
     * @return созданная сущность с присвоенным идентификатором
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public T create(@Valid @RequestBody T entity) {
        log.info("Получен запрос на создание {}: {}", entityName, entity);
        T createdEntity = createEntity(entity);
        log.info("{} успешно создан: {}", entityName, createdEntity);
        return createdEntity;
    }

    /**
     * Возвращает список всех сущностей.
     *
     * @return список всех сущностей в системе
     */
    @GetMapping
    public List<T> getAll() {
        log.info("Получен запрос на получение всех {}", entityName);
        List<T> entities = getAllEntities();
        log.info("Возвращено {} {}", entities.size(), entityName);
        return entities;
    }

    /**
     * Возвращает сущность по её идентификатору.
     *
     * @param id идентификатор сущности
     * @return найденная сущность
     */
    @GetMapping("/{id}")
    public T getById(@PathVariable Long id) {
        log.info("Получен запрос на получение {} с id={}", entityName, id);
        T entity = getEntityById(id);
        log.info("{} c id {} найден: {}", entityName, id, entity);
        return entity;
    }

    /**
     * Обновляет существующую сущность.
     *
     * @param entity объект сущности с обновленными данными
     * @return обновленная сущность
     */
    @PutMapping
    public T update(@Valid @RequestBody T entity) {
        log.info("Получен запрос на обновление {}: {}", entityName, entity);
        T updatedEntity = updateEntity(entity);
        log.info("{} успешно обновлен: {}", entityName, updatedEntity);
        return updatedEntity;
    }

    protected abstract T createEntity(T entity);

    protected abstract List<T> getAllEntities();

    protected abstract T getEntityById(Long id);

    protected abstract T updateEntity(T entity);
}
