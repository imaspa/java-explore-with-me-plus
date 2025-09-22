package ru.practicum.ewm.core.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class FutureByValidator implements ConstraintValidator<FutureBy, Instant> {

    private long amount;
    private ChronoUnit unit;

    @Override
    public void initialize(FutureBy constraintAnnotation) {
        this.amount = constraintAnnotation.amount();
        this.unit = constraintAnnotation.unit();
    }

    @Override
    public boolean isValid(Instant value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null обычно считается валидным, если не указано @NotNull
        }
        Instant now = Instant.now();
        Instant minFuture = now.plus(amount, unit);
        return !value.isBefore(minFuture);
    }
}