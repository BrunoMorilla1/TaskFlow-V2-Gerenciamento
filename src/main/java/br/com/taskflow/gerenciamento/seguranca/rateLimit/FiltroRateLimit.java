package br.com.taskflow.gerenciamento.seguranca.rateLimit;

import br.com.taskflow.gerenciamento.excecao.RateLimitException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class FiltroRateLimit extends OncePerRequestFilter {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    private final ServicoRateLimit servicoRateLimit;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.equals("/")
                || path.equals("/index")
                || path.equals("/landing.html")
                || path.equals("/login")
                || path.equals("/cadastro")
                || path.equals("/favicon.ico")
                || path.equals("/manifest.json")
                || path.equals("/sw.js")
                || path.equals("/landing.js")
                || path.equals("/robots.txt")
                || path.startsWith("/static/")
                || path.startsWith("/css/")
                || path.startsWith("/landing.css")
                || path.startsWith("/js/")
                || path.startsWith("/assets/")
                || path.startsWith("/api/v1/auth/")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String ip = obterIp(request);
        final String path = request.getRequestURI();
        final String method = request.getMethod();

        try {
            servicoRateLimit.validarLimite(ip);
            filterChain.doFilter(request, response);

        } catch (RateLimitException ex) {
            log.warn("FiltroRateLimit - Limite excedido. ip={}, path={}, method={}", ip, path, method);

            if (response.isCommitted()) {
                log.warn("FiltroRateLimit - Resposta já comprometida ao tratar rate limit. ip={}, path={}", ip, path);
                return;
            }

            response.resetBuffer();
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            response.getWriter().write("""
                    {
                      "status": 429,
                      "erro": "TOO_MANY_REQUESTS",
                      "mensagem": "Muitas requisições. Tente novamente mais tarde."
                    }
                    """);

            response.flushBuffer();
        }
    }

    private String obterIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader(HEADER_X_REAL_IP);
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "desconhecido" : remoteAddr;
    }
}