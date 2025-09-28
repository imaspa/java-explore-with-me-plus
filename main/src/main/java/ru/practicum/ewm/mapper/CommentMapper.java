package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.core.config.CommonMapperConfiguration;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.model.Comment;

@Mapper(config = CommonMapperConfiguration.class)
public interface CommentMapper {

    @Mapping(target = "authorName", source = "author.name")
    @Mapping(target = "event", source = "event.id")
    CommentDto toDto(Comment entity);
}
