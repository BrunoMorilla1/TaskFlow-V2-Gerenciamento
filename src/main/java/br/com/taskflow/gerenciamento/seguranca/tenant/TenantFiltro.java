package br.com.taskflow.gerenciamento.seguranca.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class TenantFiltro extends OncePerRequestFilter {

    public static final String HEADER_TENANT = "X-Tenant-ID";

    // White-list de APIs que nunca exigem Tenant
    private static final List<String> PUBLIC_API_PATHS = Arrays.asList(
            "/api/v1/auth",
            "/api/v1/public",
            "/consultar-cnpj"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. Regra de ignorar para caminhos que não são API ou são públicos
        if (!path.startsWith("/api/") || isPublicApi(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. AVISO: Se já existe um Tenant definido pelo JWT (ou outro filtro),
        // NÃO sobrescreva nem limpe. Apenas prossiga.
        if (TenantContext.possuiTenant()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Caso não exista Tenant, tenta buscar no Header (apenas como fallback)
        try {
            String tenantId = request.getHeader(HEADER_TENANT);

            if (StringUtils.hasText(tenantId)) {
                TenantContext.setTenant(tenantId.trim());
            } else {
                log.debug("API privada acessada sem Header de Tenant: {}", path);
            }

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            log.error("Erro crítico no processamento de Tenant para API: {}", path, ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erro de isolamento de dados.");
        } finally {
            // 4. IMPORTANTE: Só limpe o contexto se este filtro foi quem realmente o definiu
            // ou se não houver autenticação (segurança extra)
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                TenantContext.clear();
            }
        }
    }

    private boolean isPublicApi(String path) {
        return PUBLIC_API_PATHS.stream().anyMatch(path::contains);
    }
}