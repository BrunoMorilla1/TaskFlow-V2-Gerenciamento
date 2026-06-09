package br.com.taskflow.gerenciamento.pagamento.dto.resposta;

import br.com.taskflow.gerenciamento.pagamento.enums.FormaPagamento;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckoutResposta(
        String assinaturaId,
        String asaasSubscriptionId,
        String asaasPaymentId,
        FormaPagamento formaPagamento,
        StatusPagamento status,
        BigDecimal valor,

        String pixQrCodeBase64,
        String pixCopiaECola,
        String pixExpiracao,

        String invoiceUrl,
        String boletoUrl,

        String cartaoUltimosDigitos,
        String cartaoBandeira,

        String mensagem
) {}