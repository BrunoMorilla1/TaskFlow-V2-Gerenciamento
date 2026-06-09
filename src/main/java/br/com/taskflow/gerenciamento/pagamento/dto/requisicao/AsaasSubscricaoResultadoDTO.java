package br.com.taskflow.gerenciamento.pagamento.dto.requisicao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AsaasSubscricaoResultadoDTO(
        String id,
        String customer,
        String billingType,
        String status,
        Double value,
        String nextDueDate,
        String cycle,
        String creditCardToken,
        String creditCardBrand,
        String creditCardNumber
) {}
