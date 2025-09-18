package ru.practicum.ewm.service.compilation;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto create(NewCompilationDto dto);

    void delete(Long compId);

    CompilationDto update(Long compId, UpdateCompilationRequest dto);

    CompilationDto getById(Long compId);

    List<CompilationDto> getAll(Boolean pinned, Pageable pageable);
}
