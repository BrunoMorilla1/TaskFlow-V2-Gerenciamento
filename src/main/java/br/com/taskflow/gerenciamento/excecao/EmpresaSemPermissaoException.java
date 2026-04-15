package br.com.taskflow.gerenciamento.excecao;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@Getter
@ResponseStatus(HttpStatus.FORBIDDEN)
public class EmpresaSemPermissaoException extends NegocioException {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final String CODIGO_ERRO = "EMPRESA_ACESSO_NEGADO";
  private final Long empresaId;

  private EmpresaSemPermissaoException(String mensagem, Long empresaId) {
    super(mensagem, HttpStatus.FORBIDDEN, CODIGO_ERRO);
    this.empresaId = empresaId;
  }

  public static EmpresaSemPermissaoException paraEmpresa(Long empresaId) {
    return new EmpresaSemPermissaoException(
            String.format("Você não possui permissão para acessar a empresa com ID: %d.", empresaId),
            empresaId
    );
  }

  public static EmpresaSemPermissaoException acessoNegado() {
    return new EmpresaSemPermissaoException(
            "Você não possui permissão para acessar esta empresa.",
            null
    );
  }
}