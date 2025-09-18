package ru.practicum.ewm.dto.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {
    @NotBlank(message = "Заголовок должен быть заполнен")
    @Size(max = 50, min = 1, message = "Количество символов заголовка должно быть в диапазоне от 1 до 50")
    private String title;
    private Boolean pinned = false;
    private List<Long> events;
}
