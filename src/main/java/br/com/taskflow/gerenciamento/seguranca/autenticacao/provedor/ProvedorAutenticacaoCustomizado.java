package br.com.taskflow.gerenciamento.seguranca.autenticacao.provedor;

import br.com.taskflow.gerenciamento.seguranca.autenticacao.usuario.UsuarioAutenticado;
import br.com.taskflow.gerenciamento.seguranca.autenticacao.usuario.ServicoDetalhesUsuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProvedorAutenticacaoCustomizado implements AuthenticationProvider {

    private final ServicoDetalhesUsuario servicoDetalhesUsuario;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication == null) {
            log.error("AuthProvider - Objeto de autenticação não informado.");
            throw new BadCredentialsException("Credenciais inválidas.");
        }

        final String email = normalizarEmail(authentication.getName());
        final String senhaInformada = extrairSenha(authentication.getCredentials());

        if (email == null || email.isBlank()) {
            log.warn("AuthProvider - Tentativa de autenticação sem email.");
            throw new BadCredentialsException("Credenciais inválidas.");
        }

        if (senhaInformada == null || senhaInformada.isBlank()) {
            log.warn("AuthProvider - Tentativa de autenticação sem senha. email={}", email);
            throw new BadCredentialsException("Credenciais inválidas.");
        }

        log.debug("AuthProvider - Iniciando autenticação. email={}", email);

        UsuarioAutenticado usuario = carregarUsuario(email);
        validarStatusConta(usuario);

        if (!passwordEncoder.matches(senhaInformada, usuario.getPassword())) {
            log.warn("AuthProvider - Senha inválida. email={}", email);
            throw new BadCredentialsException("Credenciais inválidas.");
        }

        log.info("AuthProvider - Autenticação concluída com sucesso. usuarioId={}, email={}",
                usuario.getId(), usuario.getEmail());

        return UsernamePasswordAuthenticationToken.authenticated(
                usuario,
                null,
                usuario.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private UsuarioAutenticado carregarUsuario(String email) {
        Object userDetails = servicoDetalhesUsuario.loadUserByUsername(email);

        if (userDetails instanceof UsuarioAutenticado usuarioAutenticado) {
            return usuarioAutenticado;
        }

        log.error("AuthProvider - Tipo de principal inválido retornado pelo UserDetailsService. tipo={}",
                userDetails != null ? userDetails.getClass().getName() : null);

        throw new BadCredentialsException("Credenciais inválidas.");
    }

    private void validarStatusConta(UsuarioAutenticado usuario) {
        if (!usuario.isAccountNonLocked()) {
            log.warn("AuthProvider - Usuário bloqueado. usuarioId={}, email={}",
                    usuario.getId(), usuario.getEmail());
            throw new LockedException("Usuário bloqueado.");
        }

        if (!usuario.isAccountNonExpired()) {
            log.warn("AuthProvider - Conta expirada. usuarioId={}, email={}",
                    usuario.getId(), usuario.getEmail());
            throw new DisabledException("Conta expirada.");
        }

        if (!usuario.isCredentialsNonExpired()) {
            log.warn("AuthProvider - Credenciais expiradas. usuarioId={}, email={}",
                    usuario.getId(), usuario.getEmail());
            throw new CredentialsExpiredException("Credenciais expiradas.");
        }

        if (!usuario.isEnabled()) {
            log.warn("AuthProvider - Usuário desabilitado/inativo. usuarioId={}, email={}",
                    usuario.getId(), usuario.getEmail());
            throw new DisabledException("Usuário desabilitado.");
        }
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String extrairSenha(Object credentials) {
        if (credentials == null) {
            return null;
        }
        return credentials.toString();
    }
}