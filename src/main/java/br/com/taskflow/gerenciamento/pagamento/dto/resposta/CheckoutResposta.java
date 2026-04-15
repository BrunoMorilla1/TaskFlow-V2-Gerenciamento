package br.com.taskflow.gerenciamento.pagamento.dto.resposta;

import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckoutResposta(
        String assinaturaId,
        String asaasId,
        StatusPagamento status,
        BigDecimal valor,

        String invoiceUrl,
        String pixQrCode,
        String boletoUrl,

        String mensagem
) {

    public static CheckoutResposta sucesso(String id, String asaasId, String url) {
        return new CheckoutResposta(
                id,
                asaasId,
                StatusPagamento.PENDENTE,
                new BigDecimal("49.90"),
                url,
                null,
                null,
                "Assinatura gerada com sucesso. Aguardando pagamento."
        );
    }
}