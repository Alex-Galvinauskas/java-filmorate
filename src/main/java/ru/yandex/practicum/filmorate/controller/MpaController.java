package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.service.film.MpaService;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@Slf4j
@RequiredArgsConstructor
public class MpaController {

    private final MpaService mpaService;

    /**
     * Получает список всех рейтингов MPA
     * @return список всех рейтингов MPA
     */
    @GetMapping
    public List<Mpa> getAllMpa() {
        log.info("Получен запрос на получение всех рейтингов MPA");

        List<Mpa> mpaRatings = mpaService.getAllMpa();

        log.info("Возвращено {} рейтингов MPA", mpaRatings.size());
        return mpaRatings;
    }

    /**
     * Получает рейтинг MPA по идентификатору
     * @param id идентификатор рейтинга MPA
     * @return найденный рейтинг MPA
     */
    @GetMapping("/{id}")
    public Mpa getMpaById(@PathVariable Integer id) {
        log.info("Получен запрос на получение рейтинга MPA с id={}", id);

        Mpa mpa = mpaService.getMpaById(id);

        log.info("Рейтинг MPA с id {} найден: {}", id, mpa.getName());
        return mpa;
    }
}