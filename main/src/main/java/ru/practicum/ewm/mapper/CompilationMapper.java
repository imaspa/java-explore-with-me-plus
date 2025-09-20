package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.core.config.CommonMapperConfiguration;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;

import java.util.Set;

@Mapper(config = CommonMapperConfiguration.class, uses = {EventMapper.class}) //проверить
public interface CompilationMapper {
    @Mapping(target = "events", ignore = true)
    Compilation toEntity(NewCompilationDto dto, Set<Event> events);

    CompilationDto toDto(Compilation entity);
}

