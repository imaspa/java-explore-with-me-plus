package ru.practicum.ewm.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import ru.practicum.ewm.EndpointHitDto;
import ru.practicum.ewm.ViewStatsDto;
import ru.practicum.ewm.filter.StatsFilter;
import ru.practicum.ewm.mapper.EndpointHitMapper;
import ru.practicum.ewm.repository.EndpointHitRepository;

import java.util.List;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class EndpointHitService {
    private final EndpointHitMapper mapper;
    private final EndpointHitRepository repository;

    public void createHit(@NotNull(message = "Данные не получены или пустые") @Valid EndpointHitDto dto) {
        var entity = mapper.toEntity(dto);
        repository.save(entity);
    }

    public List<ViewStatsDto> findStats(@Valid StatsFilter filter) {
        return filter.getUnique() ? repository.findStatsByUnique(filter) : repository.findStatsByNonUnique(filter);
    }
}
