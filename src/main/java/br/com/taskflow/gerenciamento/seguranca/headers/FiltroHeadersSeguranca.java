package br.com.taskflow.gerenciamento.seguranca.headers;

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
public class FiltroHeadersSeguranca extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        setHeaderIfAbsent(response, "X-Frame-Options", "DENY");
        setHeaderIfAbsent(response, "X-Content-Type-Options", "nosniff");

        setHeaderIfAbsent(response, "X-XSS-Protection", "0");

        setHeaderIfAbsent(response,
                "Strict-Transport-Security",
                "max-age=31536000; includeSubDomains; preload"
        );

        setHeaderIfAbsent(response,
                "Referrer-Policy",
                "strict-origin-when-cross-origin"
        );

        setHeaderIfAbsent(response,
                "Permissions-Policy",
                "geolocation=(), microphone=(), camera=()"
        );

        setHeaderIfAbsent(response,
                "Cache-Control",
                "no-store, no-cache, must-revalidate, max-age=0"
        );
        setHeaderIfAbsent(response, "Pragma", "no-cache");
        setHeaderIfAbsent(response, "Expires", "0");

        log.debug("FiltroHeadersSeguranca - Headers de segurança aplicados path={}", request.getRequestURI());

        filterChain.doFilter(request, response);
    }

    private void setHeaderIfAbsent(HttpServletResponse response, String name, String value) {
        if (!response.containsHeader(name)) {
            response.setHeader(name, value);
        }
    }
}