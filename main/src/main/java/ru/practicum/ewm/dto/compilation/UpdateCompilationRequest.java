package ru.practicum.ewm.dto.compilation;

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
public class UpdateCompilationRequest {
    @Size(max = 50, message = "Количество символов заголовка должно быть в диапазоне от 1 до 50")
    private String title;
    private Boolean pinned;
    private List<Long> events;
}
