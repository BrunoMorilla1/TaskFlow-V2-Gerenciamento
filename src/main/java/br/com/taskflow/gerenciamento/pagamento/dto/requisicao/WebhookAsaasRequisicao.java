package br.com.taskflow.gerenciamento.pagamento.dto.requisicao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookAsaasRequisicao(
        String id,

        @JsonProperty("event")
        String evento,

        @JsonProperty("payment")
        DadosPagamentoWebhook pagamento
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DadosPagamentoWebhook(
            String id,
            String customer,
            String subscription,
            Double value,
            Double netValue,
            String status,
            String billingType,
            String externalReference
    ) {}
}