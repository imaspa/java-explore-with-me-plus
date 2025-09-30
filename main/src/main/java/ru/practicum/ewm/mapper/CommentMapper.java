package ru.practicum.ewm.mapper;

import org.mapstruct.*;
import ru.practicum.ewm.core.config.CommonMapperConfiguration;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.UpdateCommentDto;
import ru.practicum.ewm.dto.comment.CommentStatusDto;
import ru.practicum.ewm.model.Comment;

@Mapper(config = CommonMapperConfiguration.class)
public interface CommentMapper {

    @Mapping(target = "authorName", source = "author.name")
    @Mapping(target = "event", source = "event.id")
    CommentDto toDto(Comment entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", expression = "java(java.time.LocalDateTime.now())")
    void updateEntityFromDto(UpdateCommentDto dto, @MappingTarget Comment entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateStatus(@MappingTarget Comment target, CommentStatusDto source);
}
