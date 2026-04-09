package br.com.clientefacil.core.tenant;

/**
 * Contexto interno utilizado para controlar, por thread, se o isolamento
 * por tenant deve ser temporariamente ignorado.
 * <p>
 * Esse mecanismo é usado em conjunto com o TenantFilterAspect e a
 * annotation @SkipTenantFilter para permitir execuções específicas
 * sem aplicação do filtro de company_id.
 * <p>
 * O uso de ThreadLocal garante que o bypass fique restrito à execução
 * atual, evitando interferência em outras requisições.
 */
public final class TenantIsolationContext {

    private static final ThreadLocal<Boolean> BYPASS_TENANT_CHECK = ThreadLocal.withInitial(() -> false);

    private TenantIsolationContext() {
    }

    public static void enableBypass() {
        BYPASS_TENANT_CHECK.set(true);
    }

    public static void disableBypass() {
        BYPASS_TENANT_CHECK.remove();
    }

    public static boolean isBypassEnabled() {
        return BYPASS_TENANT_CHECK.get();
    }
}
