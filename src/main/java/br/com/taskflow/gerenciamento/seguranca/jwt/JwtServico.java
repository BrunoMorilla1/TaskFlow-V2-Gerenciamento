package br.com.taskflow.gerenciamento.seguranca.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

@Service
@Slf4j
public class JwtServico {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMPRESA_ID = "empresaId";
    private static final String CLAIM_TYPE = "type";

    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    @Value("${jwt.secret}")
    private String chaveSecreta;

    @Value("${jwt.expiration.access}")
    private long expiracaoAccessToken;

    @Value("${jwt.expiration.refresh}")
    private long expiracaoRefreshToken;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience}")
    private String audience;

    private Key obterChaveAssinatura() {
        byte[] keyBytes = Decoders.BASE64.decode(chaveSecreta);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extrairEmail(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    public String extrairEmpresaId(String token) {
        return extrairClaim(token, claims -> claims.get(CLAIM_EMPRESA_ID, String.class));
    }

    public String extrairRole(String token) {
        return extrairClaim(token, claims -> claims.get(CLAIM_ROLE, String.class));
    }

    public String extrairTokenId(String token) {
        return extrairClaim(token, Claims::getId);
    }

    public String extrairTipoToken(String token) {
        return extrairClaim(token, claims -> claims.get(CLAIM_TYPE, String.class));
    }

    public Date extrairExpiracao(String token) {
        return extrairClaim(token, Claims::getExpiration);
    }

    public Date extrairCriacao(String token) {
        return extrairClaim(token, Claims::getIssuedAt);
    }

    public long getExpiracaoAccessTokenEmSegundos() {
        return expiracaoAccessToken / 1000;
    }

    public long getExpiracaoRefreshTokenEmSegundos() {
        return expiracaoRefreshToken / 1000;
    }

    public <T> T extrairClaim(String token, Function<Claims, T> resolver) {
        Objects.requireNonNull(resolver, "O resolvedor de claims é obrigatório.");
        Claims claims = extrairTodosClaims(token);
        return resolver.apply(claims);
    }

    private Claims extrairTodosClaims(String token) {
        validarTokenInformado(token);

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(obterChaveAssinatura())
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

        } catch (ExpiredJwtException ex) {
            log.warn("JwtServico - Token expirado.");
            throw ex;

        } catch (UnsupportedJwtException ex) {
            log.warn("JwtServico - Token não suportado.");
            throw ex;

        } catch (MalformedJwtException ex) {
            log.warn("JwtServico - Token malformado.");
            throw ex;

        } catch (SignatureException ex) {
            log.warn("JwtServico - Assinatura do token inválida.");
            throw ex;

        } catch (IllegalArgumentException ex) {
            log.warn("JwtServico - Token vazio ou inválido.");
            throw ex;
        }
    }

    public String gerarAccessToken(UserDetails userDetails, String empresaId) {
        validarUserDetails(userDetails);

        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoAccessToken);
        String tokenId = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(tokenId)
                .setSubject(userDetails.getUsername())
                .claim(CLAIM_ROLE, extrairPrimeiraAuthority(userDetails.getAuthorities()))
                .claim(CLAIM_EMPRESA_ID, empresaId)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(agora)
                .setNotBefore(agora)
                .setExpiration(expiracao)
                .signWith(obterChaveAssinatura(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String gerarRefreshToken(UserDetails userDetails) {
        validarUserDetails(userDetails);

        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoRefreshToken);
        String tokenId = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(tokenId)
                .setSubject(userDetails.getUsername())
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(agora)
                .setNotBefore(agora)
                .setExpiration(expiracao)
                .signWith(obterChaveAssinatura(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validarAccessToken(String token, UserDetails userDetails) {
        validarUserDetails(userDetails);

        try {
            String email = extrairEmail(token);
            String tipo = extrairTipoToken(token);

            if (!TOKEN_TYPE_ACCESS.equals(tipo)) {
                log.warn("JwtServico - Token informado não é do tipo ACCESS.");
                return false;
            }

            return email.equals(userDetails.getUsername()) && !tokenExpirado(token);

        } catch (JwtException ex) {
            log.warn("JwtServico - Falha na validação do access token: {}", ex.getMessage());
            return false;
        }
    }

    public boolean validarRefreshToken(String token) {
        try {
            String tipo = extrairTipoToken(token);

            if (!TOKEN_TYPE_REFRESH.equals(tipo)) {
                log.warn("JwtServico - Token informado não é do tipo REFRESH.");
                return false;
            }

            return !tokenExpirado(token);

        } catch (JwtException ex) {
            log.warn("JwtServico - Falha na validação do refresh token: {}", ex.getMessage());
            return false;
        }
    }

    public boolean tokenExpirado(String token) {
        return extrairExpiracao(token).before(new Date());
    }

    private void validarTokenInformado(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("O token JWT é obrigatório.");
        }
    }

    private void validarUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Os dados do usuário são obrigatórios para gerar/validar token.");
        }

        if (userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new IllegalArgumentException("O username do usuário é obrigatório.");
        }
    }

    private String extrairPrimeiraAuthority(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            throw new IllegalArgumentException("O usuário deve possuir pelo menos uma authority.");
        }

        return authorities.stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElseThrow(() -> new IllegalArgumentException("Não foi possível extrair a authority do usuário."));
    }
}