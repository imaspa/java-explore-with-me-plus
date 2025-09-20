package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;
import ru.practicum.ewm.model.EventStateAction;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventDto {
    private Long id;
    private Long userId;

    @Nullable
    @Size(min = 20, max = 2000, message = "Количество символов в annotation от 20 до 2000")
    private String annotation;

    private Long category;

    @Nullable
    @Size(min = 20, max = 7000, message = "Количество символов в description от 20 до 7000")
    private String description;

    @Nullable
    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Europe/Moscow")
    @Future(message = "eventDate не может быть раньше текущего времени")
    private LocalDateTime eventDate;

    private LocationDto location;
    private Boolean paid;

    @Nullable
    @PositiveOrZero(message = "participantLimit >= 0")
    private Long participantLimit;

    private Boolean requestModeration;
    private EventStateAction stateAction;

    @Nullable
    @Size(min = 3, max = 120, message = "Количество символов в title от 3 до 120")
    private String title;
}
