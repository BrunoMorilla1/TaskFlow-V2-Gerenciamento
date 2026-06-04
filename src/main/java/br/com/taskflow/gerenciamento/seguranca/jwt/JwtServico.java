package br.com.taskflow.gerenciamento.seguranca.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.security.converter.RsaKeyConverters;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

@Component
@Slf4j
public class JwtServico {

    // =========================
    // CLAIMS
    // =========================
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMPRESA_ID = "empresaId";
    private static final String CLAIM_TYPE = "type";

    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    // =========================
    // CONFIG
    // =========================
    @Value("${jwt.expiration.access}")
    private long expiracaoAccessToken;

    @Value("${jwt.expiration.refresh}")
    private long expiracaoRefreshToken;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience}")
    private String audience;

    // =========================
    // RSA KEYS (ENTERPRISE SAFE)
    // =========================
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    private final Clock clock = Clock.systemUTC();

    public JwtServico(
            @Value("classpath:jwt/private.pem") Resource privateKeyResource,
            @Value("classpath:jwt/public.pem") Resource publicKeyResource
    ) {
        try {
            this.privateKey = RsaKeyConverters.pkcs8().convert(privateKeyResource.getInputStream());
            this.publicKey = RsaKeyConverters.x509().convert(publicKeyResource.getInputStream());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao carregar chaves RSA do JWT", e);
        }
    }

    // =========================================================
    // GERAÇÃO DE TOKENS
    // =========================================================

    public String gerarAccessToken(UserDetails user, String empresaId) {
        validarUser(user);

        Instant now = Instant.now(clock);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(jti)
                .setSubject(user.getUsername())
                .claim(CLAIM_ROLE, extrairRoles(user.getAuthorities()))
                .claim(CLAIM_EMPRESA_ID, normalizar(empresaId))
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expiracaoAccessToken)))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String gerarRefreshToken(UserDetails user) {
        validarUser(user);

        Instant now = Instant.now(clock);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(jti)
                .setSubject(user.getUsername())
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expiracaoRefreshToken)))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    // =========================================================
    // EXTRAÇÃO (compatível com seu sistema atual)
    // =========================================================

    public String extrairEmail(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    public String extrairEmpresaId(String token) {
        return extrairClaim(token, c -> c.get(CLAIM_EMPRESA_ID, String.class));
    }

    public String extrairRole(String token) {
        return extrairClaim(token, c -> c.get(CLAIM_ROLE, String.class));
    }

    public String extrairTokenId(String token) {
        return extrairClaim(token, Claims::getId);
    }

    public String extrairTipoToken(String token) {
        return extrairClaim(token, c -> c.get(CLAIM_TYPE, String.class));
    }

    public Date extrairExpiracao(String token) {
        return extrairClaim(token, Claims::getExpiration);
    }

    public Date extrairCriacao(String token) {
        return extrairClaim(token, Claims::getIssuedAt);
    }

    public <T> T extrairClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extrairClaims(token));
    }

    // =========================================================
    // VALIDAÇÃO ENTERPRISE
    // =========================================================

    public boolean validarAccessToken(String token, UserDetails user) {
        try {
            Claims claims = extrairClaims(token);

            return TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))
                    && claims.getSubject().equals(user.getUsername())
                    && !isExpired(claims);

        } catch (Exception e) {
            log.warn("Access token inválido: {}", e.getMessage());
            return false;
        }
    }

    public boolean validarRefreshToken(String token) {
        try {
            Claims claims = extrairClaims(token);

            return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))
                    && !isExpired(claims);

        } catch (Exception e) {
            log.warn("Refresh token inválido: {}", e.getMessage());
            return false;
        }
    }

    private Claims extrairClaims(String token) {
        validarToken(token);

        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .setAllowedClockSkewSeconds(30)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    // =========================================================
    // UTILITÁRIOS
    // =========================================================

    private void validarToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token JWT obrigatório");
        }
    }

    private void validarUser(UserDetails user) {
        Objects.requireNonNull(user, "UserDetails obrigatório");

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username obrigatório");
        }
    }

    private String extrairRoles(Collection<? extends GrantedAuthority> auth) {
        return auth.stream()
                .map(GrantedAuthority::getAuthority)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    private String normalizar(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isBlank() ? null : v;
    }

    public long getExpiracaoAccessTokenEmSegundos() {
        return expiracaoAccessToken / 1000;
    }

    public long getExpiracaoRefreshTokenEmSegundos() {
        return expiracaoRefreshToken / 1000;
    }
}