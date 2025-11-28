package ru.yandex.practicum.filmorate.service.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.MpaDbStorage;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MpaServiceImpl implements MpaService {

    private final MpaDbStorage mpaDbStorage;

    @Override
    public List<Mpa> getAllMpa() {
        log.info("Получение всех рейтингов MPA");
        return mpaDbStorage.getAllMpa();
    }

    @Override
    public Mpa getMpaById(Integer id) {
        log.info("Получение рейтинга MPA по ID: {}", id);
        return mpaDbStorage.getMpaById(id)
                .orElseThrow(() -> new NotFoundException("Рейтинг MPA с ID " + id + " не найден"));
    }
}