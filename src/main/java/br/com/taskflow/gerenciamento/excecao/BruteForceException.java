package br.com.taskflow.gerenciamento.excecao;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class BruteForceException extends NegocioException {

  private static final long serialVersionUID = 1L;

  public static final String MSG_PADRAO = "Muitas tentativas de acesso. Bloqueio temporário ativado.";
  public static final String CODIGO_ERRO = "AUTH_BRUTE_FORCE_LIMIT";

  private final String ipOrigem;

  public BruteForceException(String ipOrigem) {
    super(MSG_PADRAO, HttpStatus.TOO_MANY_REQUESTS, CODIGO_ERRO);
    this.ipOrigem = ipOrigem;
  }

  public BruteForceException(String mensagem, String ipOrigem) {
    super(mensagem, HttpStatus.TOO_MANY_REQUESTS, CODIGO_ERRO);
    this.ipOrigem = ipOrigem;
  }
}