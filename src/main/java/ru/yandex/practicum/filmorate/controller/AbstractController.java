package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


public abstract class AbstractController<D, S> {

    protected final S service;
    protected final String entityName;

    protected AbstractController(S service, String entityName) {
        this.service = service;
        this.entityName = entityName;
    }

    @PostMapping
    public ResponseEntity<D> create(@Valid @RequestBody D dto) {
        D createdDto = createEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDto);
    }

    @GetMapping
    public ResponseEntity<List<D>> getAll() {
        List<D> dtos = getAllEntities();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<D> getById(@PathVariable Long id) {
        D dto = getEntityById(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping
    public ResponseEntity<D> update(@Valid @RequestBody D dto) {
        D updatedDto = updateEntity(dto);
        return ResponseEntity.ok(updatedDto);
    }

    protected abstract D createEntity(D dto);

    protected abstract List<D> getAllEntities();

    protected abstract D getEntityById(Long id);

    protected abstract D updateEntity(D dto);

    protected abstract Long getEntityId(D dto);
}