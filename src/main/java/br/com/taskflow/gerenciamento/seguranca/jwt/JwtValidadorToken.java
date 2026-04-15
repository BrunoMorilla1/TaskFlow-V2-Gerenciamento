package br.com.taskflow.gerenciamento.seguranca.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtValidadorToken {

    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    @Value("${jwt.secret}")
    private String chaveSecreta;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience}")
    private String audience;

    private final ServicoBlacklistToken servicoBlacklistToken;

    private Key obterChave() {
        if (chaveSecreta == null || chaveSecreta.isBlank()) {
            throw new IllegalStateException("A chave secreta do JWT não foi configurada.");
        }

        byte[] keyBytes = Decoders.BASE64.decode(chaveSecreta);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validarAccessToken(String token) {
        try {
            Claims claims = extrairClaims(token);
            validarTipoToken(claims, TOKEN_TYPE_ACCESS);
            validarExpiracao(claims);
            validarBlacklist(claims);

            return true;

        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JwtValidadorToken - Access token inválido: {}", ex.getMessage());
            return false;
        }
    }

    public boolean validarRefreshToken(String token) {
        try {
            Claims claims = extrairClaims(token);
            validarTipoToken(claims, TOKEN_TYPE_REFRESH);
            validarExpiracao(claims);
            validarBlacklist(claims);

            return true;

        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JwtValidadorToken - Refresh token inválido: {}", ex.getMessage());
            return false;
        }
    }

    public Claims extrairClaims(String token) {
        validarTokenInformado(token);
        validarConfiguracao();

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(obterChave())
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

        } catch (ExpiredJwtException ex) {
            log.warn("JwtValidadorToken - Token expirado.");
            throw ex;

        } catch (UnsupportedJwtException ex) {
            log.warn("JwtValidadorToken - Token não suportado.");
            throw ex;

        } catch (MalformedJwtException ex) {
            log.warn("JwtValidadorToken - Token malformado.");
            throw ex;

        } catch (SignatureException ex) {
            log.warn("JwtValidadorToken - Assinatura inválida.");
            throw ex;

        } catch (IllegalArgumentException ex) {
            log.warn("JwtValidadorToken - Token vazio ou inválido.");
            throw ex;
        }
    }

    private void validarExpiracao(Claims claims) {
        Date expiracao = claims.getExpiration();

        if (expiracao == null) {
            throw new JwtException("Token sem data de expiração.");
        }

        if (expiracao.before(new Date())) {
            throw new JwtException("Token expirado.");
        }
    }

    private void validarTipoToken(Claims claims, String tipoEsperado) {
        String tipoToken = claims.get(CLAIM_TOKEN_TYPE, String.class);

        if (tipoToken == null || tipoToken.isBlank()) {
            throw new JwtException("Tipo do token não informado.");
        }

        if (!tipoEsperado.equals(tipoToken)) {
            throw new JwtException("Tipo de token inválido.");
        }
    }

    private void validarBlacklist(Claims claims) {
        String tokenId = claims.getId();

        if (tokenId == null || tokenId.isBlank()) {
            throw new JwtException("Identificador do token não informado.");
        }

        if (servicoBlacklistToken.tokenRevogado(tokenId)) {
            throw new JwtException("Token revogado.");
        }
    }

    private void validarTokenInformado(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("O token JWT é obrigatório.");
        }
    }

    private void validarConfiguracao() {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("O issuer do JWT não foi configurado.");
        }

        if (audience == null || audience.isBlank()) {
            throw new IllegalStateException("O audience do JWT não foi configurado.");
        }
    }
}