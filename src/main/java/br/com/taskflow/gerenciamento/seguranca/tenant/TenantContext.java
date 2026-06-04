package br.com.taskflow.gerenciamento.seguranca.tenant;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Optional;


@Slf4j
public final class TenantContext {

    private static final String MDC_TENANT_KEY = "tenantId";

    private static final ThreadLocal<String> TENANT_ATUAL = new ThreadLocal<>();

    private TenantContext() {
        throw new UnsupportedOperationException("Classe utilitária de contexto.");
    }


    public static void setTenant(String tenantId) {
        if (tenantId == null || tenantId.trim().isBlank()) {
            log.warn("TenantContext: Tentativa de definir um Tenant nulo ou vazio. Ignorando.");
            return;
        }

        String sanitizedTenant = tenantId.trim();
        TENANT_ATUAL.set(sanitizedTenant);

        // Injeta o tenantId em todos os logs subsequentes da Thread
        MDC.put(MDC_TENANT_KEY, sanitizedTenant);

        log.debug("TenantContext: Contexto definido para [{}]", sanitizedTenant);
    }

    public static Optional<String> getTenant() {
        return Optional.ofNullable(TENANT_ATUAL.get());
    }

    public static String getTenantObrigatorio() {
        return getTenant().orElseThrow(() -> {
            log.error("SEGURANÇA: Acesso negado. Operação requer um TenantContext ativo.");
            return new TenantNotFoundException("O identificador da empresa (Tenant) é obrigatório para esta operação.");
        });
    }

    public static boolean possuiTenant() {
        return TENANT_ATUAL.get() != null;
    }

    public static void clear() {
        String tenantSaindo = TENANT_ATUAL.get();
        TENANT_ATUAL.remove();
        MDC.remove(MDC_TENANT_KEY);

        if (tenantSaindo != null) {
            log.trace("TenantContext: Contexto [{}] limpo com sucesso.", tenantSaindo);
        }
    }

    public static class TenantNotFoundException extends RuntimeException {
        public TenantNotFoundException(String message) {
            super(message);
        }
    }
}