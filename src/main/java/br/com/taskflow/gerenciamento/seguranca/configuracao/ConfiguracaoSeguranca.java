package br.com.taskflow.gerenciamento.seguranca.configuracao;

import br.com.taskflow.gerenciamento.seguranca.autenticacao.provedor.ProvedorAutenticacaoCustomizado;
import br.com.taskflow.gerenciamento.excecao.HandlerSegurancaException;
import br.com.taskflow.gerenciamento.seguranca.headers.FiltroHeadersSeguranca;
import br.com.taskflow.gerenciamento.seguranca.jwt.JwtFiltroAutenticacao;
import br.com.taskflow.gerenciamento.seguranca.rateLimit.FiltroRateLimit;
import br.com.taskflow.gerenciamento.seguranca.tenant.TenantFiltro;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ConfiguracaoSeguranca {

    private final JwtFiltroAutenticacao jwtFiltroAutenticacao;
    private final TenantFiltro tenantFiltro;
    private final FiltroRateLimit filtroRateLimit;
    private final FiltroHeadersSeguranca filtroHeadersSeguranca;
    private final HandlerSegurancaException handlerSegurancaException;

    @Bean
    public PasswordEncoder codificadorSenha() {
        // Força bruta 12 é excelente para produção
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager gerenciadorAutenticacao(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain cadeiaSeguranca(
            HttpSecurity http,
            ProvedorAutenticacaoCustomizado provedorAutenticacao
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(fonteConfiguracaoCors()))
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(provedorAutenticacao)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(handlerSegurancaException))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                        "img-src 'self' data: blob: https://bs-uploads.toptal.io https://cdn.sanity.io https://mir-s3-cdn-cf.behance.net https://www.inetsoft.com; " +
                                        "font-src 'self' data: https://fonts.gstatic.com; " +
                                        "connect-src 'self' http://localhost:8080; " +
                                        "connect-src 'self'; " +
                                        "frame-ancestors 'none';"
                        ))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/empresas/consultar-cnpj/**").permitAll()
                        .requestMatchers("/", "/empresa", "/login", "/cadastro", "/landing.html", "/favicon.ico", "/site.webmanifest").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/assets/**", "/images/**").permitAll()
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                        .anyRequest().authenticated()
                );

        http.addFilterBefore(filtroHeadersSeguranca, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(filtroRateLimit, FiltroHeadersSeguranca.class);

        http.addFilterBefore(jwtFiltroAutenticacao, UsernamePasswordAuthenticationFilter.class);

        http.addFilterAfter(tenantFiltro, JwtFiltroAutenticacao.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource fonteConfiguracaoCors() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "https://taskflow.com",
                "https://app.taskflow.com",
                "http://localhost:3000",
                "http://localhost:8080"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-ID"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}