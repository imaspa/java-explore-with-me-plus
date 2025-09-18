package ru.practicum.ewm.service.compilation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private CompilationMapper compilationMapper;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private Compilation compilation;
    private CompilationDto compilationDto;
    private NewCompilationDto newCompilationDto;
    private UpdateCompilationRequest updateRequest;

    @BeforeEach
    void setUp() {
        compilation = new Compilation();
        compilation.setId(1L);
        compilation.setTitle("Test");
        compilation.setPinned(true);
        compilation.setEvents(new HashSet<>());

        compilationDto = new CompilationDto();
        compilationDto.setId(1L);
        compilationDto.setTitle("Test");
        compilationDto.setPinned(true);
        compilationDto.setEvents(List.of());

        newCompilationDto = new NewCompilationDto();
        newCompilationDto.setTitle("Test");
        newCompilationDto.setPinned(true);
        newCompilationDto.setEvents(List.of());

        updateRequest = new UpdateCompilationRequest();
        updateRequest.setTitle("Updated title");
        updateRequest.setPinned(false);
        updateRequest.setEvents(List.of());
    }

    @Test
    void createCompilation() {
        when(compilationMapper.toEntity(any(NewCompilationDto.class), any()))
                .thenReturn(compilation);
        when(compilationRepository.save(any(Compilation.class)))
                .thenReturn(compilation);
        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        CompilationDto result = compilationService.create(newCompilationDto);

        assertNotNull(result);
        assertEquals("Test", result.getTitle());
        verify(compilationRepository).save(any(Compilation.class));
    }

    @Test
    void deleteCompilation() {
        when(compilationRepository.existsById(1L)).thenReturn(true);

        compilationService.delete(1L);

        verify(compilationRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteNotFoundCompilation() {
        when(compilationRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> compilationService.delete(1L));
    }

    @Test
    void updateCompilation() {
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);
        when(compilationMapper.toDto(any(Compilation.class))).thenReturn(compilationDto);

        CompilationDto result = compilationService.update(1L, updateRequest);

        assertEquals("Test", result.getTitle());
        verify(compilationRepository).save(compilation);
    }

    @Test
    void getById() {
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

        CompilationDto result = compilationService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getAllWhenPinnedNull() {
        Page<Compilation> page = new PageImpl<>(List.of(compilation));
        when(compilationRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

        List<CompilationDto> result = compilationService.getAll(null, PageRequest.of(0, 10));

        assertEquals(1, result.size());
        verify(compilationRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAllWhenPinnedTrue() {
        Page<Compilation> page = new PageImpl<>(List.of(compilation));
        when(compilationRepository.findAllByPinned(eq(true), any(Pageable.class))).thenReturn(page);
        when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

        List<CompilationDto> result = compilationService.getAll(true, PageRequest.of(0, 10));

        assertEquals(1, result.size());
        verify(compilationRepository).findAllByPinned(eq(true), any(Pageable.class));
    }
}