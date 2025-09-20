package ru.practicum.ewm.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.core.exception.ValidateException;
import ru.practicum.ewm.dto.user.NewUserDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.mapper.UserMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.UserRepository;

import java.util.List;


@Slf4j
@Service
@Transactional(readOnly = true)
@Validated
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper mapper;

    @Override
    @Transactional
    public UserDto create(NewUserDto newUserDto) {
        checkEmailOrThrow(newUserDto.getEmail());
        User user = userRepository.save(mapper.toEntity(newUserDto));
        log.info("Создание пользователя OK, id = {}", user.getId());
        return mapper.toUserDto(user);
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));
        userRepository.delete(user);
        log.info("Удален пользователь id = {}", userId);
    }

    @Override
    public List<UserDto> findUsers(List<Long> ids, Pageable pageable) {
        if (ids == null || ids.isEmpty()) {
            return userRepository.findAll(pageable)
                    .stream()
                    .map(mapper::toUserDto)
                    .toList();
        } else {
            return userRepository.findAllById(ids)
                    .stream()
                    .map(mapper::toUserDto)
                    .toList();
        }
    }

    private void checkEmailOrThrow(String email) {
        log.info("Проверка неповторяемости email");
        if (userRepository.findAll().stream()
                .anyMatch(user -> user.getEmail().equals(email))) {
            throw new ValidateException("Пользователь с email " + email + " уже существует");
        }
        log.info("email " + email + " OK");
    }

}
