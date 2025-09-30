package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.dto.comment.UpdateCommentDto;
import ru.practicum.ewm.service.CommentService;

@RestController
@RequestMapping(path = "/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto findById(@PathVariable Long id) throws ConditionsException {
        return commentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto create(@RequestBody NewCommentDto entity,
                             @RequestHeader("X-User-Id") Long userId) throws ConditionsException {
        return commentService.create(entity, userId);
    }

    @PatchMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto update(@RequestBody UpdateCommentDto entity,
                             @RequestHeader("X-User-Id") Long userId,
                             @PathVariable Long id) throws ConditionsException {
        return commentService.update(entity, userId, id);
    }

    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) throws ConditionsException {
        commentService.deleteById(id, userId);
    }
}
