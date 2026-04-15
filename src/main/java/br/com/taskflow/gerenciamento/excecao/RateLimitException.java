package br.com.taskflow.gerenciamento.excecao;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@Getter
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitException extends NegocioException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String CODIGO_ERRO = "REQUISICOES_EXCEDIDAS";

    public RateLimitException(String mensagem) {
        super(mensagem, HttpStatus.TOO_MANY_REQUESTS, CODIGO_ERRO);
    }

    public RateLimitException(String mensagem, Throwable causa) {
        super(mensagem, HttpStatus.TOO_MANY_REQUESTS, CODIGO_ERRO);
    }
}