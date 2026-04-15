package br.com.taskflow.gerenciamento.pagamento.cliente.asaas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradutorErroAsaas {

    private final ObjectMapper objectMapper;
    private static final Map<String, String> DE_PARA_ERROS;

    static {
        Map<String, String> mapa = new ConcurrentHashMap<>();
        mapa.put("invalid_card", "O número do cartão de crédito é inválido.");
        mapa.put("invalid_number", "O número do cartão de crédito é inválido.");
        mapa.put("invalid_expiryMonth", "O mês de validade do cartão é inválido.");
        mapa.put("invalid_expiryYear", "O ano de validade do cartão é inválido.");
        mapa.put("invalid_ccv", "O código de segurança (CVV) está incorreto.");
        mapa.put("card_not_accepted", "Este cartão não é aceito. Tente outro meio de pagamento.");
        mapa.put("insufficient_funds", "Saldo insuficiente no cartão selecionado.");
        mapa.put("transaction_not_allowed", "Transação não permitida pelo banco emissor do cartão.");
        mapa.put("customer_not_found", "Erro interno de cadastro. Entre em contato com o suporte.");
        mapa.put("duplicate_payment", "Pagamento duplicado detectado. Aguarde alguns instantes.");
        mapa.put("invalid_cpf_cnpj", "O CPF ou CNPJ informado é inválido.");
        mapa.put("invalid_email", "O e-mail informado é inválido para o gateway.");
        DE_PARA_ERROS = Collections.unmodifiableMap(mapa);
    }

    public String traduzir(HttpClientErrorException ex, String usuarioId, String ip) {
        log.error("Asaas[Erro] - Falha na requisição API. usuarioId={}, status={}, ip={}",
                usuarioId, ex.getStatusCode(), ip);

        try {
            String responseBody = ex.getResponseBodyAsString();
            if (responseBody == null || responseBody.isBlank()) {
                return "Resposta vazia do gateway de pagamento.";
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errors = root.path("errors");

            if (errors.isArray() && !errors.isEmpty()) {
                JsonNode primeiroErro = errors.get(0);
                String code = primeiroErro.path("code").asText();
                String description = primeiroErro.path("description").asText();

                log.warn("Asaas[Erro] - Detalhes do Gateway. usuarioId={}, code={}, descricao={}",
                        usuarioId, code, description);

                return Optional.ofNullable(DE_PARA_ERROS.get(code))
                        .orElse("Erro no processamento: " + description);
            }
        } catch (Exception e) {
            log.error("Asaas[Erro] - Falha ao processar corpo do erro. usuarioId={}, erroInterno={}",
                    usuarioId, e.getMessage());
        }

        return "Não foi possível processar seu pagamento no momento. Tente novamente mais tarde.";
    }
}