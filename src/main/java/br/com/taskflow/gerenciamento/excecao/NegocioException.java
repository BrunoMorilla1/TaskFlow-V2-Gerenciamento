package br.com.taskflow.gerenciamento.excecao;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serial;

@Getter
public class NegocioException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  private final HttpStatus status;
  private final String codigo;

  public NegocioException(String mensagem, HttpStatus status) {
    super(mensagem);
    this.status = status;
    this.codigo = status.name();
  }

  public NegocioException(String mensagem, HttpStatus status, String codigo) {
    super(mensagem);
    this.status = status;
    this.codigo = codigo;
  }
}