package br.com.taskflow.gerenciamento.pagamento.eventos;

import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class PagamentoConfirmadoEvento {

    private final Assinatura assinatura;
    private final String ip;
    private final String asaasPaymentId;

    public PagamentoConfirmadoEvento(Assinatura assinatura, String ip, String asaasPaymentId) {
        this.assinatura = assinatura;
        this.ip = ip;
        this.asaasPaymentId = asaasPaymentId;
    }

    public PagamentoConfirmadoEvento(Assinatura assinatura, String ip) {
        this(assinatura, ip, "N/A");
    }

    public Long getUsuarioId() {
        return (this.assinatura != null && this.assinatura.getUsuario() != null)
                ? this.assinatura.getUsuario().getId() : null;
    }
}