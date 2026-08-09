package br.com.clientefacil.core.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regra única de força de senha, reaproveitada em UserRequest/ChangePasswordRequest/
 * ResetPasswordRequest — testada uma vez aqui em vez de repetir o mesmo teste nos três DTOs.
 */
class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @Test
    void acceptsAPasswordWithLettersAndDigits_atOrAboveTheMinimumLength() {
        assertThat(validator.isValid("senha123", null)).isTrue();
    }

    @Test
    void rejectsPasswordsShorterThanTheMinimumLength() {
        assertThat(validator.isValid("abc123", null)).isFalse();
    }

    @Test
    void rejectsPasswordsWithOnlyDigits() {
        assertThat(validator.isValid("12345678", null)).isFalse();
    }

    @Test
    void rejectsPasswordsWithOnlyLetters() {
        assertThat(validator.isValid("somenteletras", null)).isFalse();
    }

    @Test
    void treatsNullAndBlank_asValid_leavingObrigatoriedadeTo_NotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }
}
