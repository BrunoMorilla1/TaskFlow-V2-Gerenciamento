package br.com.taskflow.gerenciamento.seguranca.jwt;

import br.com.taskflow.gerenciamento.excecao.HandlerSegurancaException;
import br.com.taskflow.gerenciamento.seguranca.tenant.TenantContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFiltroAutenticacao extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtServico jwtServico;
    private final UserDetailsService userDetailsService;
    private final HandlerSegurancaException handlerSegurancaException;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/")
                || path.equals("/login")
                || path.equals("/index.html")
                || path.equals("/favicon.ico")
                || path.equals("/static/manifest.json")
                || path.equals("/sw.js")
                || path.startsWith("/api/v1/auth/")
                || path.startsWith("/static/css/")
                || path.startsWith("/static/js/")
                || path.startsWith("/static/assets/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Tratamento de Preflight (CORS) para evitar erro "Found: 0 periods" em OPTIONS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        final String ip = extrairIp(request);
        final String userAgent = request.getHeader("User-Agent");

        try {
            String token = extrairBearerToken(request);

            if (StringUtils.hasText(token)) {
                // 2. Validação Estrutural Rígida: Impede que tokens malformados quebrem o parser
                if (token.split("\\.").length != 3) {
                    log.warn("JwtFiltroAutenticacao - Estrutura de token inválida. ip={}, ua={}", ip, userAgent);
                    limparContextos();
                    handlerSegurancaException.tratarTokenInvalido(request, response);
                    return;
                }

                processarAutenticacao(token, request, ip, userAgent);
            }

            filterChain.doFilter(request, response);

        } catch (JwtException ex) {
            log.warn("JwtFiltroAutenticacao - Falha validação JWT: {} | ip={}", ex.getMessage(), ip);
            limparContextos();
            handlerSegurancaException.tratarTokenInvalido(request, response);
        } catch (Exception ex) {
            log.error("JwtFiltroAutenticacao - Erro inesperado: {} | ip={}", ex.getMessage(), ip, ex);
            limparContextos();
            handlerSegurancaException.tratarTokenInvalido(request, response);
        } finally {
            // Segurança de ThreadLocal: Limpa o Tenant se a autenticação falhar ou terminar
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                TenantContext.clear();
            }
        }
    }

    private void processarAutenticacao(String token, HttpServletRequest request, String ip, String userAgent) {
        String email = jwtServico.extrairEmail(token);
        String empresaId = jwtServico.extrairEmpresaId(token);

        if (StringUtils.hasText(email) && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtServico.validarAccessToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // Define o contexto multi-tenant para a transação atual
                TenantContext.setTenant(empresaId);

                log.debug("JwtFiltroAutenticacao - Autenticado: email={}, empresaId={}, ip={}", email, empresaId, ip);
            } else {
                log.warn("JwtFiltroAutenticacao - Token expirado ou inválido para: {}", email);
            }
        }
    }

    private String extrairBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(PREFIXO_BEARER)) {
            return header.substring(PREFIXO_BEARER.length());
        }
        return null;
    }

    private void limparContextos() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private String extrairIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return (StringUtils.hasText(remoteAddr)) ? remoteAddr : "desconhecido";
    }
}