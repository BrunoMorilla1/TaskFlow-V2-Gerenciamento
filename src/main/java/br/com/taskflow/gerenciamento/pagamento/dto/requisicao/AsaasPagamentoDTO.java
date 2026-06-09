package br.com.taskflow.gerenciamento.pagamento.dto.requisicao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AsaasPagamentoDTO(
        String id,
        String customer,
        String subscription,
        String billingType,
        String status,
        Double value,
        Double netValue,
        String dueDate,
        String invoiceUrl,
        String bankSlipUrl,
        String creditCardNumber,
        String creditCardBrand,
        String creditCardToken
) {}