package br.com.taskflow.gerenciamento.excecao;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@Getter
@ResponseStatus(HttpStatus.CONFLICT)
public class EmailJaCadastradoException extends NegocioException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String CODIGO_ERRO = "USUARIO_EMAIL_DUPLICADO";
    private final String email;

    public EmailJaCadastradoException(String email) {
        super("Este e-mail já está sendo utilizado no sistema.", HttpStatus.CONFLICT, CODIGO_ERRO);
        this.email = email;
    }

    public EmailJaCadastradoException(String mensagem, String email) {
        super(mensagem, HttpStatus.CONFLICT, CODIGO_ERRO);
        this.email = email;
    }
}