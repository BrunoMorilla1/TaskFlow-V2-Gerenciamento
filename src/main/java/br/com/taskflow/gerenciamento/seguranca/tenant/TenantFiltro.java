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

        String path = request.getRequestURI();
        if (path.contains("/api/v1/auth") || path.contains("/consultar-cnpj")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String tenantId = request.getHeader(HEADER_TENANT);

            if (tenantId != null && !tenantId.isBlank()) {
                TenantContext.setTenant(tenantId.trim());
            } else {
                log.debug("Requisição sem header de Tenant: {}", path);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}