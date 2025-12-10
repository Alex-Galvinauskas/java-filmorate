package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.DirectorDTO;
import ru.yandex.practicum.filmorate.service.directors.DirectorService;

import java.util.List;

@RestController
@RequestMapping("/directors")
@RequiredArgsConstructor
public class DirectorController {

    private final DirectorService directorService;

    @GetMapping
    public ResponseEntity<List<DirectorDTO>> getAll() {
        List<DirectorDTO> directors = directorService.getAll();
        return ResponseEntity.ok(directors);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DirectorDTO> getById(@PathVariable Long id) {
        DirectorDTO director = directorService.getById(id);
        return ResponseEntity.ok(director);
    }

    @PostMapping
    public ResponseEntity<DirectorDTO> create(@Valid @RequestBody DirectorDTO directorDTO) {
        DirectorDTO created = directorService.createDirector(directorDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping
    public ResponseEntity<DirectorDTO> update(@Valid @RequestBody DirectorDTO directorDTO) {
        DirectorDTO updated = directorService.updateDirector(directorDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        directorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
