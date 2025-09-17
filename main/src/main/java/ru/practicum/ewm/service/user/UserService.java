package ru.practicum.ewm.service.user;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.dto.user.NewUserDto;
import ru.practicum.ewm.dto.user.UserDto;

import java.util.List;

public interface UserService {
    UserDto create(NewUserDto userDto);
    void delete(Long userId);

    List<UserDto> findUsers(List<Long> ids, Pageable pageable);
}
