package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.dto.MpaDTO;
import ru.yandex.practicum.filmorate.service.film.MpaService;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
public class MpaController {

    private final MpaService mpaService;

    @GetMapping
    public ResponseEntity<List<MpaDTO>> getAllMpa() {
        List<MpaDTO> mpaRatings = mpaService.getAllMpa();
        return ResponseEntity.ok(mpaRatings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MpaDTO> getMpaById(@PathVariable Long id) {
        MpaDTO mpa = mpaService.getMpaById(id);
        return ResponseEntity.ok(mpa);
    }
}