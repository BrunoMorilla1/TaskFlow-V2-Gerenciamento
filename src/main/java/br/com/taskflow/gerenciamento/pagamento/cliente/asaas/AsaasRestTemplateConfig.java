package br.com.taskflow.gerenciamento.pagamento.cliente.asaas;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AsaasRestTemplateConfig {

    private final ConfiguracaoAsaas config;

    @Bean(name = "asaasRestTemplate")
    @Primary
    public RestTemplate asaasRestTemplate(RestTemplateBuilder builder) {
        validarConfiguracao();

        String baseUrl = config.getUrl().endsWith("/")
                ? config.getUrl()
                : config.getUrl() + "/";

        log.info("Asaas RestTemplate configurado. baseUrl='{}'", baseUrl);

        return builder
                .rootUri(baseUrl)
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(20))
                .additionalInterceptors((request, body, execution) -> {
                    HttpHeaders headers = request.getHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                    headers.set("access_token", config.getChave());
                    headers.set(HttpHeaders.USER_AGENT, config.getUserAgent());
                    return execution.execute(request, body);
                })
                .build();
    }

    private void validarConfiguracao() {
        String url = config.getUrl();
        String chave = config.getChave();

        log.debug("Asaas[Config] - url='{}' | chave='{}'",
                url, chave == null ? "NULA" : "ok");

        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "ERRO CRÍTICO: 'asaas.url' não configurada. " +
                            "Defina a variável de ambiente ASAAS_URL.");
        }
        if (chave == null || chave.isBlank() || chave.contains("sua_chave")) {
            throw new IllegalStateException(
                    "ERRO CRÍTICO: 'asaas.chave' inválida ou não configurada. " +
                            "Defina a variável de ambiente ASAAS_API_KEY.");
        }
    }
}