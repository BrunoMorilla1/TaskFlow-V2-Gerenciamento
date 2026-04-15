package br.com.taskflow.gerenciamento.seguranca.jwt;

import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServicoRefreshToken {

    private final RepositorioRefreshToken repositorioRefreshToken;
    private final JwtValidadorToken jwtValidadorToken;
    private final JwtGeradorToken jwtGeradorToken;

    @Transactional
    public void salvarRefreshToken(String token) {
        validarToken(token);

        Claims claims = jwtValidadorToken.extrairClaims(token);
        validarClaimsRefreshToken(claims);

        String tokenId = claims.getId();
        String email = claims.getSubject();
        Instant expiracao = claims.getExpiration().toInstant();

        if (repositorioRefreshToken.existsByTokenIdAndRevogadoFalse(tokenId)) {
            log.warn("ServicoRefreshToken - Tentativa de salvar refresh token já existente. email={}, jti={}",
                    email, tokenId);
            return;
        }

        EntidadeRefreshToken entidade = new EntidadeRefreshToken();
        entidade.setTokenId(tokenId);
        entidade.setEmailUsuario(email);
        entidade.setExpiracao(expiracao);
        entidade.setRevogado(false);

        repositorioRefreshToken.save(entidade);

        log.info("ServicoRefreshToken - Refresh token salvo. email={}, jti={}", email, tokenId);
    }

    @Transactional
    public boolean refreshTokenValido(String token) {
        validarToken(token);

        try {
            if (!jwtValidadorToken.validarRefreshToken(token)) {
                log.warn("ServicoRefreshToken - Refresh token inválido na validação JWT.");
                return false;
            }

            Claims claims = jwtValidadorToken.extrairClaims(token);
            String tokenId = claims.getId();

            EntidadeRefreshToken entidade = repositorioRefreshToken.findByTokenIdAndRevogadoFalse(tokenId)
                    .orElse(null);

            if (entidade == null) {
                log.warn("ServicoRefreshToken - Refresh token não encontrado ou já revogado. jti={}", tokenId);
                return false;
            }

            if (Instant.now().isAfter(entidade.getExpiracao())) {
                log.warn("ServicoRefreshToken - Refresh token expirado em base. jti={}", tokenId);
                return false;
            }

            return true;

        } catch (Exception ex) {
            log.warn("ServicoRefreshToken - Falha ao validar refresh token: {}", ex.getMessage());
            return false;
        }
    }

    @Transactional
    public String rotacionarRefreshToken(String refreshToken, UserDetails userDetails) {
        validarToken(refreshToken);
        validarUserDetails(userDetails);

        if (!jwtValidadorToken.validarRefreshToken(refreshToken)) {
            log.warn("ServicoRefreshToken - Tentativa de rotação com refresh token inválido.");
            throw new BadCredentialsException("Refresh token inválido.");
        }

        Claims claims = jwtValidadorToken.extrairClaims(refreshToken);
        validarClaimsRefreshToken(claims);

        String tokenId = claims.getId();
        String emailToken = claims.getSubject();
        String emailUsuario = userDetails.getUsername();

        if (!Objects.equals(emailToken, emailUsuario)) {
            log.warn("ServicoRefreshToken - Subject do token divergente do usuário. tokenEmail={}, userEmail={}",
                    emailToken, emailUsuario);
            throw new BadCredentialsException("Refresh token inválido.");
        }

        EntidadeRefreshToken tokenDb = repositorioRefreshToken.findByTokenIdAndRevogadoFalse(tokenId)
                .orElseThrow(() -> {
                    log.warn("ServicoRefreshToken - Refresh token não encontrado para rotação. jti={}", tokenId);
                    return new BadCredentialsException("Refresh token inválido.");
                });

        tokenDb.setRevogado(true);
        repositorioRefreshToken.save(tokenDb);

        String novoRefreshToken = jwtGeradorToken.gerarRefreshToken(userDetails);
        salvarRefreshToken(novoRefreshToken);

        log.info("ServicoRefreshToken - Refresh token rotacionado com sucesso. email={}, jtiAntigo={}",
                emailUsuario, tokenId);

        return novoRefreshToken;
    }

    @Transactional
    public int revogarTokensUsuario(String email) {
        String emailNormalizado = normalizarEmail(email);

        if (emailNormalizado == null || emailNormalizado.isBlank()) {
            log.warn("ServicoRefreshToken - Tentativa de revogar tokens com email inválido.");
            return 0;
        }

        int totalRevogados = repositorioRefreshToken.revogarTokensAtivosPorUsuario(emailNormalizado);

        log.warn("ServicoRefreshToken - Refresh tokens revogados. email={}, total={}",
                emailNormalizado, totalRevogados);

        return totalRevogados;
    }

    @Transactional
    public int limparTokensExpirados() {
        int totalRemovidos = repositorioRefreshToken.deletarTokensExpirados(Instant.now());

        if (totalRemovidos > 0) {
            log.debug("ServicoRefreshToken - Refresh tokens expirados removidos. total={}", totalRemovidos);
        }

        return totalRemovidos;
    }

    private void validarClaimsRefreshToken(Claims claims) {
        if (claims == null) {
            throw new BadCredentialsException("Refresh token inválido.");
        }

        if (claims.getId() == null || claims.getId().isBlank()) {
            throw new BadCredentialsException("Refresh token inválido.");
        }

        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new BadCredentialsException("Refresh token inválido.");
        }

        if (claims.getExpiration() == null) {
            throw new BadCredentialsException("Refresh token inválido.");
        }

        String tipoToken = claims.get("tokenType", String.class);
        if (!"REFRESH".equals(tipoToken)) {
            throw new BadCredentialsException("Refresh token inválido.");
        }
    }

    private void validarToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("O refresh token é obrigatório.");
        }
    }

    private void validarUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Os dados do usuário são obrigatórios.");
        }

        if (userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new IllegalArgumentException("O username do usuário é obrigatório.");
        }
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return null;
        }

        return email.trim().toLowerCase();
    }
}