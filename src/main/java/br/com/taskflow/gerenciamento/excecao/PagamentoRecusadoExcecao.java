package br.com.taskflow.gerenciamento.excecao;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@Getter
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PagamentoRecusadoExcecao extends NegocioException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String CODIGO_ERRO = "PAGAMENTO_RECUSADO";
    private final String usuarioId;
    private final String motivo;
    private final String ip;
    private final String asaasId;

    public PagamentoRecusadoExcecao(String motivo, String usuarioId, String asaasId, String ip) {
        super(String.format("Pagamento recusado pelo gateway: %s", motivo),
                HttpStatus.PAYMENT_REQUIRED,
                CODIGO_ERRO);
        this.motivo = motivo;
        this.usuarioId = usuarioId;
        this.asaasId = asaasId;
        this.ip = ip;
    }
}