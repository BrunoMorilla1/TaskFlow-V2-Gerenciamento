package br.com.taskflow.gerenciamento.excecao;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@Getter
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class FalhaComunicacaoGatewayExcecao extends NegocioException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String CODIGO_ERRO = "GATEWAY_INDISPONIVEL";
    private final String gateway;
    private final String usuarioId;
    private final String ip;

    public FalhaComunicacaoGatewayExcecao(String gateway, String usuarioId, String ip, Throwable causa) {
        super(String.format("Falha crítica de comunicação com o gateway %s.", gateway),
                HttpStatus.SERVICE_UNAVAILABLE,
                CODIGO_ERRO);
        this.gateway = gateway;
        this.usuarioId = usuarioId;
        this.ip = ip;
    }

    public FalhaComunicacaoGatewayExcecao(String gateway, String usuarioId, String ip) {
        this(gateway, usuarioId, ip, null);
    }
}