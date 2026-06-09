package br.com.taskflow.gerenciamento.pagamento.dto.requisicao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AsaasPixQrCodeDTO(
        String encodedImage,
        String payload,
        String expirationDate,
        Boolean success
) {}