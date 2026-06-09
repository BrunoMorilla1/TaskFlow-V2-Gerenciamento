package br.com.taskflow.gerenciamento.pagamento.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AsaasConfigValidator {

    @Value("${asaas.chave}")
    private String chave;

    @PostConstruct
    public void validar() {
        if (chave == null || chave.isBlank() || chave.startsWith("${")) {
            log.error("ERRO CRÍTICO: Variável de ambiente 'ASAAS_API_KEY' não foi configurada!");
            throw new IllegalStateException("O sistema não pode iniciar sem a chave do Asaas.");
        }
        log.info("Configuração do Asaas carregada com sucesso.");
    }
}