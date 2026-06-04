//package br.com.taskflow.gerenciamento.seguranca.jwt;
//
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
//import io.jsonwebtoken.io.Decoders;
//import io.jsonwebtoken.security.Keys;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.stereotype.Component;
//
//import java.security.Key;
//import java.time.Instant;
//import java.util.Collection;
//import java.util.Date;
//import java.util.Objects;
//import java.util.UUID;
//
//@Component
//@Slf4j
//public class JwtGeradorToken {
//
//    private static final String CLAIM_ROLE = "role";
//    private static final String CLAIM_EMPRESA_ID = "empresaId";
//    private static final String CLAIM_TYPE = "type";
//
//    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
//    private static final String TOKEN_TYPE_REFRESH = "REFRESH";
//
//    @Value("${jwt.secret}")
//    private String chaveSecreta;
//
//    @Value("${jwt.expiration.access}")
//    private long expiracaoAccessToken;
//
//    @Value("${jwt.expiration.refresh}")
//    private long expiracaoRefreshToken;
//
//    @Value("${jwt.issuer}")
//    private String issuer;
//
//    @Value("${jwt.audience}")
//    private String audience;
//
//    private Key obterChave() {
//        if (chaveSecreta == null || chaveSecreta.isBlank()) {
//            throw new IllegalStateException("A chave secreta do JWT não foi configurada.");
//        }
//
//        byte[] keyBytes = Decoders.BASE64.decode(chaveSecreta);
//        return Keys.hmacShaKeyFor(keyBytes);
//    }
//
//    public String gerarAccessToken(UserDetails userDetails, String empresaId) {
//        validarUserDetails(userDetails);
//        validarConfiguracao();
//
//        Instant agora = Instant.now();
//        Instant expiracao = agora.plusMillis(expiracaoAccessToken);
//        String tokenId = UUID.randomUUID().toString();
//        String role = extrairPrimeiraAuthority(userDetails.getAuthorities());
//
//        String token = Jwts.builder()
//                .setId(tokenId)
//                .setSubject(userDetails.getUsername())
//                .claim(CLAIM_ROLE, role)
//                .claim(CLAIM_EMPRESA_ID, normalizarEmpresaId(empresaId))
//                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
//                .setIssuer(issuer)
//                .setAudience(audience)
//                .setIssuedAt(Date.from(agora))
//                .setNotBefore(Date.from(agora))
//                .setExpiration(Date.from(expiracao))
//                .signWith(obterChave(), SignatureAlgorithm.HS256)
//                .compact();
//
//        log.debug("JwtGeradorToken - Access token gerado. usuario={}, empresaId={}, jti={}",
//                userDetails.getUsername(), empresaId, tokenId);
//
//        return token;
//    }
//
//    public String gerarRefreshToken(UserDetails userDetails) {
//        validarUserDetails(userDetails);
//        validarConfiguracao();
//
//        Instant agora = Instant.now();
//        Instant expiracao = agora.plusMillis(expiracaoRefreshToken);
//        String tokenId = UUID.randomUUID().toString();
//
//        String token = Jwts.builder()
//                .setId(tokenId)
//                .setSubject(userDetails.getUsername())
//                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
//                .setIssuer(issuer)
//                .setAudience(audience)
//                .setIssuedAt(Date.from(agora))
//                .setNotBefore(Date.from(agora))
//                .setExpiration(Date.from(expiracao))
//                .signWith(obterChave(), SignatureAlgorithm.HS256)
//                .compact();
//
//        log.debug("JwtGeradorToken - Refresh token gerado. usuario={}, jti={}",
//                userDetails.getUsername(), tokenId);
//
//        return token;
//    }
//
//    public long getExpiracaoAccessTokenEmSegundos() {
//        return expiracaoAccessToken / 1000;
//    }
//
//    public long getExpiracaoRefreshTokenEmSegundos() {
//        return expiracaoRefreshToken / 1000;
//    }
//
//    private void validarUserDetails(UserDetails userDetails) {
//        Objects.requireNonNull(userDetails, "Os dados do usuário são obrigatórios.");
//
//        if (userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
//            throw new IllegalArgumentException("O username do usuário é obrigatório para gerar token.");
//        }
//    }
//
//    private void validarConfiguracao() {
//        if (expiracaoAccessToken <= 0) {
//            throw new IllegalStateException("A expiração do access token deve ser maior que zero.");
//        }
//
//        if (expiracaoRefreshToken <= 0) {
//            throw new IllegalStateException("A expiração do refresh token deve ser maior que zero.");
//        }
//
//        if (issuer == null || issuer.isBlank()) {
//            throw new IllegalStateException("O issuer do JWT não foi configurado.");
//        }
//
//        if (audience == null || audience.isBlank()) {
//            throw new IllegalStateException("O audience do JWT não foi configurado.");
//        }
//    }
//
//    private String extrairPrimeiraAuthority(Collection<? extends GrantedAuthority> authorities) {
//        if (authorities == null || authorities.isEmpty()) {
//            throw new IllegalArgumentException("O usuário deve possuir pelo menos uma authority.");
//        }
//
//        return authorities.stream()
//                .findFirst()
//                .map(GrantedAuthority::getAuthority)
//                .orElseThrow(() -> new IllegalArgumentException("Não foi possível extrair a authority do usuário."));
//    }
//
//    private String normalizarEmpresaId(String empresaId) {
//        if (empresaId == null) {
//            return null;
//        }
//
//        String valorNormalizado = empresaId.trim();
//        return valorNormalizado.isBlank() ? null : valorNormalizado;
//    }
//}