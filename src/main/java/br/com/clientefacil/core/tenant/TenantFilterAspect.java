package br.com.clientefacil.core.tenant;

import br.com.clientefacil.core.security.util.SecurityUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect responsável por aplicar automaticamente o filtro de tenant
 * nas chamadas da camada de service.
 * <p>
 * Antes da execução do méto-do, verifica se existe um usuário autenticado
 * e, caso exista, habilita o filtro Hibernate "tenantFilter" com o
 * company_id correspondente.
 * <p>
 * Também respeita a annotation @SkipTenantFilter, permitindo ignorar
 * explicitamente o isolamento por tenant em cenários específicos.
 * <p>
 * Ao final da execução, o filtro é removido para evitar impacto em
 * outras operações executadas na mesma thread.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class TenantFilterAspect {

    private static final long GLOBAL_ONLY_COMPANY_SENTINEL = -1L;

    private final EntityManager entityManager;

    @SuppressWarnings("unused")
    @Around("execution(public * br.com.clientefacil.service..*(..))")
    public Object applyTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        if (shouldSkipTenantFilter(joinPoint)) {
            TenantIsolationContext.enableBypass();
            try {
                return joinPoint.proceed();
            } finally {
                TenantIsolationContext.disableBypass();
            }
        }

        if (SecurityUtil.getAuthenticatedUser().isEmpty()) {
            return joinPoint.proceed();
        }

        Session session = entityManager.unwrap(Session.class);
        Filter existingFilter = session.getEnabledFilter("tenantFilter");
        if (existingFilter != null) {
            return joinPoint.proceed();
        }

        long companyId = SecurityUtil.getAuthenticatedCompanyId().orElse(GLOBAL_ONLY_COMPANY_SENTINEL);
        Filter tenantFilter = session.enableFilter("tenantFilter");
        tenantFilter.setParameter("companyId", companyId);

        try {
            return joinPoint.proceed();
        } finally {
            session.disableFilter("tenantFilter");
        }
    }

    private boolean shouldSkipTenantFilter(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object target = joinPoint.getTarget();
        Class<?> targetClass = target != null ? target.getClass() : method.getDeclaringClass();

        return AnnotatedElementUtils.hasAnnotation(method, SkipTenantFilter.class)
                || AnnotatedElementUtils.hasAnnotation(targetClass, SkipTenantFilter.class);
    }
}
