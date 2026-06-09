package br.com.taskflow.gerenciamento.pagamento.cliente.asaas;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "asaas")
public class ConfiguracaoAsaas {

    private String url;
    private String chave;
    private String webhookToken;
    private String userAgent = "TaskFlow-Gerenciamento/1.0";
}