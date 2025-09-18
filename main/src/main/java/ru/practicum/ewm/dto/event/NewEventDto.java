package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.model.Category;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {

    @NotBlank(message = "Не заполнено поле annotation")
    @Size(min = 20, max = 2000, message = "Количество символов в annotation от 20 до 2000")
    private String annotation;

    @NotBlank
    private Category category;

    @NotBlank(message = "Не заполнено поле description")
    @Size(min = 20, max = 7000, message = "Количество символов в description от 20 до 7000")
    private String description;

    @NotBlank(message = "Не заполнено поле eventDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Europe/Moscow")
    @Future(message = "eventDate не может быть раньше текущего времени")
    private LocalDateTime eventDate;

    @NotBlank
    private LocationDto location;

    private Boolean paid = false;

    @PositiveOrZero(message = "participantLimit >= 0")
    private Long participantLimit = 0L;

    private Boolean requestModeration = true;

    @NotBlank(message = "Не заполнено поле title")
    @Size(min = 3, max = 120, message = "Количество символов в title от 3 до 120")
    private String title;

}
