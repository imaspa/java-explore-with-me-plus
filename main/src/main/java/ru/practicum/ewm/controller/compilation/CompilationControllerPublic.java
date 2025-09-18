package ru.practicum.ewm.controller.compilation;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.service.compilation.CompilationService;

import java.util.List;

@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CompilationControllerPublic {
    private final CompilationService compilationService;

    @GetMapping
    public List<CompilationDto> getAll(
            @RequestParam(required = false) Boolean pinned,
            @PositiveOrZero @RequestParam(defaultValue = "0") int from,
            @Positive @RequestParam(defaultValue = "10") int size) {
        log.info("Получение подборок событий: pinned = {}, from = {}, size = {}", pinned, from, size);
        Pageable pageable = PageRequest.of(from / size, size);
        return compilationService.getAll(pinned, pageable);
    }

    @GetMapping("/{compId}")
    public CompilationDto getById(@Positive @PathVariable Long compId) {
        log.info("Получение Подборки событий по id = {}", compId);
        return compilationService.getById(compId);
    }
}
