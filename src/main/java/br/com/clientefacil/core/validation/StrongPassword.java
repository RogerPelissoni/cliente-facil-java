package br.com.clientefacil.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Regra única de força de senha, reaproveitada em todo lugar que aceita uma senha nova
 * ({@code UserRequest}, {@code ChangePasswordRequest}, {@code ResetPasswordRequest}) — evita
 * duplicar a mesma regra em cada DTO (e divergir sem querer no dia em que uma delas mudar).
 * <p>
 * Deliberadamente não exige maiúscula/minúscula/caractere especial — esse tipo de regra de
 * composição é o que a maioria dos guias de segurança atuais (ex: NIST 800-63B) recomenda evitar:
 * incomoda o usuário sem ganho real (as pessoas só trocam "a" por "@" de forma previsível).
 * Comprimento mínimo maior + mistura de letra/número é o equilíbrio adotado aqui.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {

    String message() default "Senha deve ter no mínimo 8 caracteres, com letras e números";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
