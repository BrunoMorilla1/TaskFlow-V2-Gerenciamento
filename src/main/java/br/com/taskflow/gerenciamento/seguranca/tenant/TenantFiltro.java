package br.com.taskflow.gerenciamento.seguranca.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class TenantFiltro extends OncePerRequestFilter {

    public static final String HEADER_TENANT = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String tenantHeader = request.getHeader(HEADER_TENANT);
            String tenantId = normalizarTenant(tenantHeader);

            if (!TenantContext.possuiTenant() && tenantId != null) {
                TenantContext.setTenant(tenantId);
                log.debug("TenantFiltro - Tenant identificado via header. tenantId={}", tenantId);

            } else if (TenantContext.possuiTenant()) {
                log.debug("TenantFiltro - Tenant já presente no contexto. tenantId={}", TenantContext.getTenant());

            } else {
                log.debug("TenantFiltro - Requisição sem tenant definido.");
            }

            filterChain.doFilter(request, response);
        } finally {
        }
    }

    private String normalizarTenant(String tenantId) {
        if (tenantId == null) {
            return null;
        }

        String tenantNormalizado = tenantId.trim();
        return tenantNormalizado.isBlank() ? null : tenantNormalizado;
    }
}