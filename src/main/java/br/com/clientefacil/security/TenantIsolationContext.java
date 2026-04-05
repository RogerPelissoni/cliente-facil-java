package br.com.clientefacil.security;

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
