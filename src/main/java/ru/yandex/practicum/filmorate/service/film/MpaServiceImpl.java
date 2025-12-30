package ru.yandex.practicum.filmorate.service.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.MpaDTO;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.MpaDbStorage;
import ru.yandex.practicum.filmorate.mapper.MpaMapper;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MpaServiceImpl implements MpaService {

    private final MpaDbStorage mpaDbStorage;
    private final MpaMapper mpaMapper;

    @Override
    public List<MpaDTO> getAllMpa() {
        log.debug("Получение всех рейтингов MPA");
        List<Mpa> mpaList = mpaDbStorage.getAllMpa();
        return mpaList.stream()
                .map(mpaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public MpaDTO getMpaById(Long id) {
        log.debug("Получение рейтинга MPA по ID: {}", id);
        Mpa mpa = mpaDbStorage.getMpaById(id)
                .orElseThrow(() -> new NotFoundException("Рейтинг MPA с ID " + id + " не найден"));
        return mpaMapper.toDTO(mpa);
    }
}