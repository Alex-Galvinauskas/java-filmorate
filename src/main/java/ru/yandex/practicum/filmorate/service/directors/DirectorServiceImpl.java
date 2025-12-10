package ru.yandex.practicum.filmorate.service.directors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.DirectorDTO;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.managment.db.DirectorDbStorage;
import ru.yandex.practicum.filmorate.mapper.DirectorMapper;
import ru.yandex.practicum.filmorate.model.Director;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DirectorServiceImpl implements DirectorService {

    private final DirectorDbStorage directorDbStorage;
    private final DirectorMapper directorMapper;

    @Override
    public DirectorDTO create(DirectorDTO directorDTO) {
        log.debug("Создание режиссера: {}", directorDTO.getName());

        Director director = directorMapper.toEntity(directorDTO);
        Director created = directorDbStorage.create(director);

        return directorMapper.toDTO(created);
    }

    @Override
    public DirectorDTO update(DirectorDTO directorDTO) {
        log.debug("Обновление режиссера c ID: {} {}", directorDTO.getName(), directorDTO.getId());

        validateDirectorExists(directorDTO.getId());

        Director director = directorMapper.toEntity(directorDTO);
        Director updated = directorDbStorage.update(director);

        return directorMapper.toDTO(updated);
    }

    @Override
    public DirectorDTO getById(Long id) {
        log.debug("Получение режиссера по ID: {}", id);

        Director director = directorDbStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Режиссер с ID " + id + " не найден"));

        return directorMapper.toDTO(director);
    }

    @Override
    public List<DirectorDTO> getAll() {
        log.debug("Получение всех режиссеров");

        return directorDbStorage.getAll().stream()
                .map(directorMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        log.debug("Удаление режиссера по ID: {}", id);

        validateDirectorExists(id);
        directorDbStorage.delete(id);
    }

    private void validateDirectorExists(Long id) {
        if (!directorDbStorage.existsById(id)) {
            throw new NotFoundException("Режиссер с ID " + id + " не найден");
        }
    }
}
