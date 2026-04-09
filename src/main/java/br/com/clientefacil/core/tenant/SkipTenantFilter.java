package br.com.clientefacil.core.tenant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation utilizada para ignorar a aplicação automática do filtro de tenant.
 * <p>
 * Quando presente em uma classe ou méto-do, indica que a operação não deve
 * restringir os dados pelo contexto de tenant (company_id).
 * <p>
 * Usado principalmente em cenários como:
 * - operações administrativas globais
 * - consultas que precisam acessar múltiplos tenants
 * - processos internos do sistema
 * <p>
 * Deve ser utilizado com cautela, pois ignora o isolamento de dados entre empresas.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipTenantFilter {
}