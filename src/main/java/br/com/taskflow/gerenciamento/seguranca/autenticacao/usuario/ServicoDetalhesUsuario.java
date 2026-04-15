package br.com.taskflow.gerenciamento.seguranca.autenticacao.usuario;

import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import br.com.taskflow.gerenciamento.usuarios.repositorio.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServicoDetalhesUsuario implements UserDetailsService {

    private final UsuarioRepositorio usuarioRepositorio;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        final String emailNormalizado = normalizarEmail(email);

        if (emailNormalizado == null || emailNormalizado.isBlank()) {
            log.warn("Security[UserDetailsService] - Tentativa de carga de usuário com email inválido");
            throw new UsernameNotFoundException("Credenciais inválidas.");
        }

        log.debug("Security[UserDetailsService] - Buscando usuário para autenticação. email={}", emailNormalizado);

        Usuario usuario = usuarioRepositorio.findByEmailIgnoreCaseAndDeletadoFalse(emailNormalizado)
                .orElseThrow(() -> {
                    log.warn("Security[UserDetailsService] - Usuário não encontrado. email={}", emailNormalizado);
                    return new UsernameNotFoundException("Credenciais inválidas.");
                });

        validarUsuario(usuario);

        log.info("Security[UserDetailsService] - Usuário apto para autenticação. usuarioId={}, email={}, role={}",
                usuario.getId(),
                usuario.getEmail(),
                usuario.getRole());

        return new UsuarioAutenticado(usuario);
    }

    private void validarUsuario(Usuario usuario) {
        if (!usuario.isAtivo()) {
            log.warn("Security[UserDetailsService] - Usuário inativo. usuarioId={}, email={}",
                    usuario.getId(), usuario.getEmail());
            throw new DisabledException("Usuário inativo.");
        }

        if (!usuario.isAccountNonLocked()) {
            log.warn("Security[UserDetailsService] - Usuário bloqueado. usuarioId={}, email={}",
                    usuario.getId(), usuario.getEmail());
            throw new LockedException("Usuário bloqueado.");
        }

        if (!usuario.isAccountNonExpired()) {
            log.warn("Security[UserDetailsService] - Conta expirada. usuarioId={}, email={}",
                    usuario.getId(), usuario.getEmail());
            throw new DisabledException("Conta expirada.");
        }

        if (!usuario.isCredentialsNonExpired()) {
            log.warn("Security[UserDetailsService] - Credenciais expiradas. usuarioId={}, email={}",
                    usuario.getId(), usuario.getEmail());
            throw new CredentialsExpiredException("Credenciais expiradas.");
        }

        if (!usuario.isEnabled()) {
            log.warn("Security[UserDetailsService] - Usuário desabilitado. usuarioId={}, email={}",
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
}