package br.com.taskflow.gerenciamento.seguranca.autenticacao.servico;

import br.com.taskflow.gerenciamento.seguranca.autenticacao.dto.requisicao.LoginRequisicao;
import br.com.taskflow.gerenciamento.seguranca.autenticacao.dto.requisicao.RefreshTokenRequisicao;
import br.com.taskflow.gerenciamento.seguranca.autenticacao.dto.resposta.LoginResposta;
import br.com.taskflow.gerenciamento.seguranca.autenticacao.usuario.UsuarioAutenticado;
import br.com.taskflow.gerenciamento.seguranca.jwt.JwtServico;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import br.com.taskflow.gerenciamento.excecao.UsuarioNaoEncontradoException;
import br.com.taskflow.gerenciamento.usuarios.repositorio.UsuarioRepositorio;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServicoAutenticacao {

    private static final int MAX_TENTATIVAS_LOGIN = 5;
    private static final long BLOQUEIO_EM_MINUTOS = 30L;

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepositorio usuarioRepositorio;
    private final JwtServico jwtServico;

    @Transactional
    public LoginResposta autenticar(LoginRequisicao requisicao, HttpServletRequest request) {
        validarRequisicaoLogin(requisicao);

        final String email = requisicao.emailNormalizado();
        final String ip = extrairIp(request);

        log.info("AuthService - Iniciando autenticação. email={}, ip={}", email, ip);

        Usuario usuario = usuarioRepositorio.findByEmailIgnoreCaseAndDeletadoFalse(email)
                .orElseThrow(() -> {
                    log.warn("AuthService - Tentativa de login com usuário inexistente. email={}, ip={}", email, ip);
                    return new BadCredentialsException("Email ou senha inválidos.");
                });

        desbloquearContaSePrazoExpirou(usuario);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, requisicao.senha())
            );

            UsuarioAutenticado principal = extrairPrincipal(authentication);

            usuario.registrarSucessoLogin(ip);
            usuarioRepositorio.save(usuario);

            String empresaId = resolverEmpresaId(usuario, principal, request);

            String accessToken = jwtServico.gerarAccessToken(principal, empresaId);
            String refreshToken = jwtServico.gerarRefreshToken(principal);

            log.info("AuthService - Autenticação realizada com sucesso. usuarioId={}, email={}, ip={}",
                    usuario.getId(), usuario.getEmail(), ip);

            return LoginResposta.of(
                    accessToken,
                    refreshToken,
                    jwtServico.getExpiracaoAccessTokenEmSegundos()
            );

        } catch (BadCredentialsException ex) {
            registrarFalhaLogin(usuario, ip);
            log.warn("AuthService - Falha de autenticação por credenciais inválidas. email={}, ip={}", email, ip);
            throw new BadCredentialsException("Email ou senha inválidos.");

        } catch (LockedException ex) {
            log.warn("AuthService - Tentativa de login em conta bloqueada. usuarioId={}, email={}, ip={}",
                    usuario.getId(), usuario.getEmail(), ip);
            throw ex;

        } catch (DisabledException ex) {
            log.warn("AuthService - Tentativa de login em conta desabilitada/inativa. usuarioId={}, email={}, ip={}",
                    usuario.getId(), usuario.getEmail(), ip);
            throw ex;

        } catch (CredentialsExpiredException ex) {
            log.warn("AuthService - Tentativa de login com credenciais expiradas. usuarioId={}, email={}, ip={}",
                    usuario.getId(), usuario.getEmail(), ip);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public LoginResposta renovarToken(RefreshTokenRequisicao requisicao, HttpServletRequest request) {
        validarRequisicaoRefresh(requisicao);

        final String refreshToken = requisicao.tokenNormalizado();

        log.info("AuthService - Iniciando renovação de access token.");

        if (!jwtServico.validarRefreshToken(refreshToken)) {
            log.warn("AuthService - Refresh token inválido.");
            throw new BadCredentialsException("Refresh token inválido.");
        }

        String email = normalizarEmail(jwtServico.extrairEmail(refreshToken));

        Usuario usuario = usuarioRepositorio.findByEmailIgnoreCaseAndDeletadoFalse(email)
                .orElseThrow(() -> {
                    log.warn("AuthService - Usuário não encontrado na renovação de token. email={}", email);
                    return new UsuarioNaoEncontradoException("Usuário não encontrado.");
                });

        validarUsuarioAptoParaToken(usuario);

        UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
        String empresaId = resolverEmpresaId(usuario, principal, request);

        String novoAccessToken = jwtServico.gerarAccessToken(principal, empresaId);
        String novoRefreshToken = jwtServico.gerarRefreshToken(principal);

        log.info("AuthService - Token renovado com sucesso. usuarioId={}, email={}",
                usuario.getId(), usuario.getEmail());

        return LoginResposta.of(
                novoAccessToken,
                novoRefreshToken,
                jwtServico.getExpiracaoAccessTokenEmSegundos()
        );
    }

    @Transactional(readOnly = true)
    public UsuarioAutenticado carregarUsuarioAutenticadoPorEmail(String email) {
        String emailNormalizado = normalizarEmail(email);

        if (emailNormalizado == null || emailNormalizado.isBlank()) {
            log.warn("AuthService - Email inválido ao carregar usuário autenticado.");
            throw new UsuarioNaoEncontradoException("Usuário não encontrado.");
        }

        Usuario usuario = usuarioRepositorio.findByEmailIgnoreCaseAndDeletadoFalse(emailNormalizado)
                .orElseThrow(() -> {
                    log.warn("AuthService - Usuário não encontrado. email={}", emailNormalizado);
                    return new UsuarioNaoEncontradoException("Usuário não encontrado.");
                });

        return new UsuarioAutenticado(usuario);
    }

    @Transactional
    public void registrarLogout(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            log.debug("AuthService - Logout recebido sem header Authorization.");
            return;
        }

        log.info("AuthService - Logout processado.");
        /*
         * Ponto de extensão para blacklist/revogação de tokens.
         */
    }

    private void registrarFalhaLogin(Usuario usuario, String ip) {
        usuario.registrarFalhaLogin(MAX_TENTATIVAS_LOGIN);
        usuarioRepositorio.save(usuario);

        if (usuario.estaBloqueado()) {
            log.warn("AuthService - Usuário bloqueado por excesso de tentativas. usuarioId={}, email={}, ip={}, tentativas={}",
                    usuario.getId(), usuario.getEmail(), ip, usuario.getTentativasLogin());
            return;
        }

        log.warn("AuthService - Tentativa inválida registrada. usuarioId={}, email={}, ip={}, tentativas={}",
                usuario.getId(), usuario.getEmail(), ip, usuario.getTentativasLogin());
    }

    private void desbloquearContaSePrazoExpirou(Usuario usuario) {
        if (!usuario.estaBloqueado() || usuario.getDataBloqueio() == null) {
            return;
        }

        LocalDateTime dataDesbloqueio = usuario.getDataBloqueio().plusMinutes(BLOQUEIO_EM_MINUTOS);

        if (LocalDateTime.now().isAfter(dataDesbloqueio)) {
            usuario.desbloquearConta();
            usuarioRepositorio.save(usuario);

            log.info("AuthService - Conta desbloqueada automaticamente. usuarioId={}, email={}",
                    usuario.getId(), usuario.getEmail());
        }
    }

    private void validarUsuarioAptoParaToken(Usuario usuario) {
        if (!usuario.isAtivo()) {
            throw new DisabledException("Usuário inativo.");
        }

        if (!usuario.isAccountNonLocked()) {
            throw new LockedException("Usuário bloqueado.");
        }

        if (!usuario.isAccountNonExpired()) {
            throw new DisabledException("Conta expirada.");
        }

        if (!usuario.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("Credenciais expiradas.");
        }

        if (!usuario.isEnabled()) {
            throw new DisabledException("Usuário desabilitado.");
        }
    }

    private UsuarioAutenticado extrairPrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof UsuarioAutenticado usuarioAutenticado) {
            return usuarioAutenticado;
        }

        log.error("AuthService - Principal autenticado inválido: {}", principal != null ? principal.getClass().getName() : null);
        throw new IllegalStateException("Falha ao obter o principal autenticado.");
    }

    private void validarRequisicaoLogin(LoginRequisicao requisicao) {
        if (requisicao == null) {
            log.error("AuthService - Requisição de login não informada.");
            throw new IllegalArgumentException("Os dados de login são obrigatórios.");
        }

        if (!requisicao.possuiEmailValido()) {
            log.error("AuthService - Email não informado na requisição de login.");
            throw new IllegalArgumentException("O email é obrigatório.");
        }

        if (!requisicao.possuiSenhaValida()) {
            log.error("AuthService - Senha não informada na requisição de login.");
            throw new IllegalArgumentException("A senha é obrigatória.");
        }
    }

    private void validarRequisicaoRefresh(RefreshTokenRequisicao requisicao) {
        if (requisicao == null) {
            log.error("AuthService - Requisição de refresh token não informada.");
            throw new IllegalArgumentException("Os dados de renovação são obrigatórios.");
        }

        if (!requisicao.possuiTokenValido()) {
            log.error("AuthService - Refresh token não informado.");
            throw new IllegalArgumentException("O refresh token é obrigatório.");
        }
    }

    private String resolverEmpresaId(Usuario usuario, UsuarioAutenticado principal, HttpServletRequest request) {
        /*
         * Regra ainda não enviada.
         * Quando você me mandar a origem real do empresaId/tenant,
         * eu ajusto só este método.
         */
        return null;
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String extrairIp(HttpServletRequest request) {
        if (request == null) {
            return "desconhecido";
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "desconhecido" : remoteAddr;
    }
}