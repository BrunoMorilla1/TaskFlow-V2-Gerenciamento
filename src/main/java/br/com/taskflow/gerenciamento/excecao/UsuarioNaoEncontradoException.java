package br.com.taskflow.gerenciamento.excecao;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@Getter
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UsuarioNaoEncontradoException extends NegocioException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String CODIGO_ERRO = "USUARIO_NAO_ENCONTRADO";
    private final String identificador;

    public UsuarioNaoEncontradoException() {
        super("Usuário não encontrado.", HttpStatus.NOT_FOUND, CODIGO_ERRO);
        this.identificador = null;
    }

    public UsuarioNaoEncontradoException(String identificador) {
        super(String.format("Usuário não encontrado com o identificador: %s", identificador),
                HttpStatus.NOT_FOUND,
                CODIGO_ERRO);
        this.identificador = identificador;
    }
}