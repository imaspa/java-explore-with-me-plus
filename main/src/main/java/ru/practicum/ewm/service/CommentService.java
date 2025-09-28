package ru.practicum.ewm.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.dto.comment.UpdateCommentDto;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Transactional(readOnly = true)
    public CommentDto findById(long id) throws ConditionsException {
        return commentMapper.toDto(getCommentOrThrow(id));
    }

    @Transactional
    public CommentDto create(NewCommentDto entity, Long userId) throws ConditionsException {
        var comment = Comment.builder()
                .author(getUserOrThrow(userId))
                .event(getEventOrThrow(entity.getEventId()))
                .text(entity.getText())
                .deleted(false)
                .created(LocalDateTime.now())
                .updated(LocalDateTime.now())
                .build();

        log.info("Создание нового комментария к событию с id={} пользователем с id={}", entity.getEventId(), userId);
        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Transactional
    public CommentDto update(UpdateCommentDto entity, Long userId, Long id) throws ConditionsException {
        var comment = getCommentOrThrow(id);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConditionsException("Вы не можете редактировать данный комментарий.");
        }

        comment.setText(entity.getText());
        comment.setUpdated(LocalDateTime.now());

        log.info("Обновление комментария к событию с id={} пользователем с id {}", comment.getEvent().getId(), userId);
        return commentMapper.toDto(comment);
    }

    @Transactional
    public void deleteById(Long id, Long userId) throws ConditionsException {
        var comment = getCommentOrThrow(id);

        if (!Objects.equals(comment.getAuthor().getId(), userId)) {
            throw new ConditionsException("Вы не можете удалить данный комментарий.");
        }

        comment.setDeleted(true);
        log.info("Комментарий с id={} удалён пользователем с id={}", id, userId);
    }

    @Transactional
    public void deleteCommentAsAdmin(Long id) {
        var comment = getCommentOrThrow(id);

        comment.setDeleted(true);
        log.info("Комментарий с id={} удалён администратором.", id);
    }


    @Transactional(readOnly = true)
    public List<CommentDto> findAllCommentsForEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Отсутствует событие с id=" + eventId);
        }

        log.info("Получаем комментарии по событию с id={}", eventId);
        var comments = commentRepository.findCommentsByEvent(eventId).orElse(new ArrayList<>());

        log.info("Возвращаем {} комментариев события с id={}", comments.size(), eventId);
        return comments.stream()
                .map(commentMapper::toDto)
                .toList();
    }

    private Comment getCommentOrThrow(Long commentId) {
        return commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден."));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }
}
