package br.com.taskflow.gerenciamento.pagamento.cliente.asaas;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "asaas")
public class ConfiguracaoAsaas {

    private String url;
    private String chave;

    @Bean
    public RestTemplate asaasRestTemplate(RestTemplateBuilder builder) {
        validarConfiguracao();

        log.info("Asaas[Configuracao] - Inicializando RestTemplate. URL base definida como: {}", url);

        return builder
                .rootUri(this.url)
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(15))
                .defaultHeader("access_token", this.chave)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    private void validarConfiguracao() {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("ERRO: 'asaas.url' não encontrada no application.yml");
        }
        if (chave == null || chave.isBlank() || chave.contains("sua_chave")) {
            throw new IllegalStateException("ERRO: 'asaas.chave' inválida ou não configurada.");
        }
    }
}