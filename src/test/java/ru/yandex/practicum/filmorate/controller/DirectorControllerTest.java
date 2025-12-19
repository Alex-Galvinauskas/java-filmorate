package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.filmorate.dto.DirectorDTO;
import ru.yandex.practicum.filmorate.exception.GlobalExceptionHandler;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.service.directors.DirectorService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class DirectorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DirectorService directorService;

    @InjectMocks
    private DirectorController directorController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DirectorDTO testDirector;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(directorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testDirector = new DirectorDTO();
        testDirector.setId(1L);
        testDirector.setName("Christopher Nolan");
    }

    @Test
    void getAll_shouldReturnListOfDirectors() throws Exception {
        List<DirectorDTO> directors = List.of(testDirector);
        when(directorService.getAll()).thenReturn(directors);

        mockMvc.perform(get("/directors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Christopher Nolan"));

        verify(directorService, times(1)).getAll();
    }

    @Test
    void getById_shouldReturnDirector() throws Exception {
        when(directorService.getById(1L)).thenReturn(testDirector);

        mockMvc.perform(get("/directors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Christopher Nolan"));

        verify(directorService, times(1)).getById(1L);
    }

    @Test
    void getById_whenDirectorNotFound_shouldReturnNotFound() throws Exception {
        when(directorService.getById(999L))
                .thenThrow(new NotFoundException("Режиссер с id=999 не найден"));

        mockMvc.perform(get("/directors/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Объект не найден"))
                .andExpect(jsonPath("$.message")
                        .value("Режиссер с id=999 не найден"));

        verify(directorService, times(1)).getById(999L);
    }

    @Test
    void create_shouldReturnCreatedDirector() throws Exception {
        DirectorDTO newDirector = new DirectorDTO();
        newDirector.setName("Quentin Tarantino");

        DirectorDTO createdDirector = new DirectorDTO();
        createdDirector.setId(2L);
        createdDirector.setName("Quentin Tarantino");

        when(directorService.createDirector(any(DirectorDTO.class)))
                .thenReturn(createdDirector);

        mockMvc.perform(post("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDirector)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Quentin Tarantino"));

        verify(directorService, times(1))
                .createDirector(any(DirectorDTO.class));
    }

    @Test
    void update_shouldReturnUpdatedDirector() throws Exception {
        DirectorDTO updatedDirector = new DirectorDTO();
        updatedDirector.setId(1L);
        updatedDirector.setName("Christopher Nolan Updated");

        when(directorService.updateDirector(any(DirectorDTO.class)))
                .thenReturn(updatedDirector);

        mockMvc.perform(put("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDirector)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Christopher Nolan Updated"));

        verify(directorService, times(1))
                .updateDirector(any(DirectorDTO.class));
    }

    @Test
    void update_whenDirectorNotFound_shouldReturnNotFound() throws Exception {
        DirectorDTO updatedDirector = new DirectorDTO();
        updatedDirector.setId(999L);
        updatedDirector.setName("Not Found Director");

        when(directorService.updateDirector(any(DirectorDTO.class)))
                .thenThrow(new NotFoundException("Режиссер с id=999 не найден"));

        mockMvc.perform(put("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDirector)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Объект не найден"))
                .andExpect(jsonPath("$.message")
                        .value("Режиссер с id=999 не найден"));

        verify(directorService, times(1))
                .updateDirector(any(DirectorDTO.class));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        doNothing().when(directorService).delete(1L);

        mockMvc.perform(delete("/directors/1"))
                .andExpect(status().isNoContent());

        verify(directorService, times(1)).delete(1L);
    }

    @Test
    void delete_whenDirectorNotFound_shouldReturnNotFound() throws Exception {
        doThrow(new NotFoundException("Режиссер с id=999 не найден"))
                .when(directorService).delete(999L);

        mockMvc.perform(delete("/directors/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Объект не найден"))
                .andExpect(jsonPath("$.message")
                        .value("Режиссер с id=999 не найден"));

        verify(directorService, times(1)).delete(999L);
    }

    @Test
    void create_withInvalidData_shouldReturnBadRequest() throws Exception {
        DirectorDTO invalidDirector = new DirectorDTO();

        mockMvc.perform(post("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDirector)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_withInvalidData_shouldReturnBadRequest() throws Exception {
        DirectorDTO invalidDirector = new DirectorDTO(); // name is null

        mockMvc.perform(put("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDirector)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenDuplicate_shouldReturnConflict() throws Exception {
        DirectorDTO duplicateDirector = new DirectorDTO();
        duplicateDirector.setName("Existing Director");

        when(directorService.createDirector(any(DirectorDTO.class)))
                .thenThrow(new ru.yandex.practicum.filmorate.exception
                        .DuplicateException("Режиссер уже существует"));

        mockMvc.perform(post("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDirector)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Конфликт данных"))
                .andExpect(jsonPath("$.message").value("Режиссер уже существует"));

        verify(directorService, times(1))
                .createDirector(any(DirectorDTO.class));
    }
}