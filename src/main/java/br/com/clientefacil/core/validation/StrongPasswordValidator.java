package br.com.clientefacil.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final int MIN_LENGTH = 8;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Obrigatoriedade é responsabilidade do @NotBlank já presente em todo campo que usa essa
        // anotação — null/vazio não é problema desse validator (evita duplicar a mensagem de "campo
        // obrigatório" e a de "senha fraca" pro mesmo valor ausente).
        if (value == null || value.isBlank()) {
            return true;
        }

        if (value.length() < MIN_LENGTH) {
            return false;
        }

        boolean hasLetter = value.chars().anyMatch(Character::isLetter);
        boolean hasDigit = value.chars().anyMatch(Character::isDigit);

        return hasLetter && hasDigit;
    }
}
