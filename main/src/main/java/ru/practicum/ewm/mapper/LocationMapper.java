package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import ru.practicum.ewm.core.config.CommonMapperConfiguration;
import ru.practicum.ewm.dto.event.LocationDto;
import ru.practicum.ewm.model.Location;

@Mapper(config = CommonMapperConfiguration.class)
public interface LocationMapper {
    Location toEntity(LocationDto dto);

    LocationDto toDto(Location entity);

}
