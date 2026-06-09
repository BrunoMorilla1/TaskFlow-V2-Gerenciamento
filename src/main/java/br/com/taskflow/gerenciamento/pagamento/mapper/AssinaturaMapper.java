package br.com.taskflow.gerenciamento.pagamento.mapper;

import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.AsaasPagamentoDTO;
import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.AsaasPixQrCodeDTO;
import br.com.taskflow.gerenciamento.pagamento.dto.resposta.CheckoutResposta;
import br.com.taskflow.gerenciamento.pagamento.dto.resposta.StatusPagamentoResposta;
import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import br.com.taskflow.gerenciamento.pagamento.entidade.Transacao;
import br.com.taskflow.gerenciamento.pagamento.enums.FormaPagamento;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AssinaturaMapper {

    public CheckoutResposta paraCheckoutResposta(Assinatura assinatura,
                                                 AsaasPagamentoDTO pagamento,
                                                 AsaasPixQrCodeDTO qr,
                                                 FormaPagamento forma) {

        String ultimos = null, bandeira = null;
        if (pagamento != null && pagamento.creditCardNumber() != null) {
            String num = pagamento.creditCardNumber();
            ultimos = num.length() >= 4 ? num.substring(num.length() - 4) : num;
            bandeira = pagamento.creditCardBrand();
        }

        String msg = switch (forma) {
            case PIX -> "Escaneie o QR Code ou copie o código PIX para concluir o pagamento.";
            case CREDITO, DEBITO -> "Pagamento processado. Aguardando confirmação da operadora.";
            case BOLETO -> "Boleto gerado. Pague até o vencimento.";
            default -> "Cobrança gerada com sucesso.";
        };

        return new CheckoutResposta(
                assinatura.getId().toString(),
                assinatura.getAsaasId(),
                pagamento != null ? pagamento.id() : null,
                forma,
                StatusPagamento.PENDENTE,
                assinatura.getValor() != null ? assinatura.getValor() : new BigDecimal("49.90"),
                        qr != null ? qr.encodedImage() : null,
                        qr != null ? qr.payload() : null,
                        qr != null ? qr.expirationDate() : null,
                        pagamento != null ? pagamento.invoiceUrl() : null,
                        pagamento != null ? pagamento.bankSlipUrl() : null,
                        ultimos,
                        bandeira,
                        msg
                );
    }

    public StatusPagamentoResposta paraStatusResposta(Transacao t) {
        return new StatusPagamentoResposta(
                t.getAsaasId(),
                t.getStatus(),
                t.getValor(),
                t.getDataPagamento(),
                t.getDataVencimento(),
                t.getUrlPagamento(),
                gerarMensagem(t.getStatus()),
                t.estaPaga()
        );
    }

    private String gerarMensagem(StatusPagamento s) {
        return switch (s) {
            case CONFIRMADO, RECEBIDO -> "Pagamento confirmado. Acesso liberado.";
            case VENCIDO -> "Pagamento vencido. Regularize para evitar suspensão.";
            case ESTORNADO -> "Pagamento estornado.";
            case CANCELADO -> "Assinatura cancelada.";
            default -> "Aguardando confirmação do gateway.";
        };
    }
}