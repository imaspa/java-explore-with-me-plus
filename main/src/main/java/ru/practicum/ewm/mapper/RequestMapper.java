package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.core.config.CommonMapperConfiguration;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.model.ParticipationRequest;

@Mapper(config = CommonMapperConfiguration.class)
public interface RequestMapper {

    @Mapping(target = "event", source = "entity.event.id")
    @Mapping(target = "requester", source = "entity.requester.id")
    ParticipationRequestDto toDto(ParticipationRequest entity);
}
