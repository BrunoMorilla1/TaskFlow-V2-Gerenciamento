package br.com.taskflow.gerenciamento.seguranca.tenant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TenantContext {

    private static final ThreadLocal<String> TENANT_ATUAL = new ThreadLocal<>();

    private TenantContext() {
        throw new IllegalStateException("Classe utilitária não deve ser instanciada.");
    }

    public static void setTenant(String tenantId) {

        if (tenantId == null || tenantId.isBlank()) {
            log.warn("TenantContext - Tentativa de definir tenant inválido.");
            return;
        }

        String tenantNormalizado = tenantId.trim();

        TENANT_ATUAL.set(tenantNormalizado);

        log.debug("TenantContext - Tenant definido {}", tenantNormalizado);
    }

    public static String getTenant() {
        return TENANT_ATUAL.get();
    }

    public static String getTenantObrigatorio() {
        String tenant = TENANT_ATUAL.get();

        if (tenant == null || tenant.isBlank()) {
            log.warn("TenantContext - Tenant não encontrado no contexto.");
            throw new IllegalStateException("Tenant não definido no contexto da requisição.");
        }

        return tenant;
    }

    public static boolean possuiTenant() {
        String tenant = TENANT_ATUAL.get();
        return tenant != null && !tenant.isBlank();
    }

    public static void clear() {
        TENANT_ATUAL.remove();
        log.debug("TenantContext - Tenant removido do contexto");
    }
}