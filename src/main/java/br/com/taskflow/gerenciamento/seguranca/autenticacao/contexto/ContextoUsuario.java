package br.com.taskflow.gerenciamento.seguranca.autenticacao.contexto;

import br.com.taskflow.gerenciamento.seguranca.autenticacao.usuario.UsuarioAutenticado;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Slf4j
public final class ContextoUsuario {

    private ContextoUsuario() {
        // Utility class
    }

    public static Optional<UsuarioAutenticado> obterUsuarioLogado() {
        Authentication authentication = obterAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UsuarioAutenticado usuario) {
            return Optional.of(usuario);
        }

        log.warn("ContextoUsuario - Principal não é do tipo UsuarioAutenticado. tipo={}",
                principal != null ? principal.getClass().getName() : null);

        return Optional.empty();
    }

    public static UsuarioAutenticado obterUsuarioObrigatorio() {
        return obterUsuarioLogado()
                .orElseThrow(() -> {
                    log.warn("ContextoUsuario - Usuário não autenticado ao tentar acesso obrigatório.");
                    return new IllegalStateException("Usuário não autenticado.");
                });
    }

    public static Long obterUsuarioId() {
        return obterUsuarioLogado()
                .map(UsuarioAutenticado::getId)
                .orElse(null);
    }

    public static String obterEmail() {
        return obterUsuarioLogado()
                .map(UsuarioAutenticado::getEmail)
                .orElse(null);
    }

    public static String obterTenantId() {
        return obterUsuarioLogado()
                .map(UsuarioAutenticado::getTenantId)
                .orElse(null);
    }

    public static boolean estaAutenticado() {
        Authentication authentication = obterAuthentication();

        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    public static void limparContexto() {
        SecurityContextHolder.clearContext();
        log.debug("ContextoUsuario - SecurityContext limpo.");
    }

    private static Authentication obterAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}