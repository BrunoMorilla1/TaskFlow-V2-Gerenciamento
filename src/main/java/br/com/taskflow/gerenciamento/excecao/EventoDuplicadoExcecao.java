package br.com.taskflow.gerenciamento.excecao;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@Getter
@ResponseStatus(HttpStatus.CONFLICT)
public class EventoDuplicadoExcecao extends NegocioException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String CODIGO_ERRO = "PAGAMENTO_EVENTO_DUPLICADO";
    private final String eventoId;

    public EventoDuplicadoExcecao(String eventoId) {
        super(String.format("Evento de Webhook já processado anteriormente. ID: %s", eventoId),
                HttpStatus.CONFLICT,
                CODIGO_ERRO);
        this.eventoId = eventoId;
    }
}