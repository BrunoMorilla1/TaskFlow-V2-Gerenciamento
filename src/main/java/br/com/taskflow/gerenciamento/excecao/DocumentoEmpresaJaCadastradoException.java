package br.com.taskflow.gerenciamento.excecao;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@Getter
@ResponseStatus(HttpStatus.CONFLICT)
public class DocumentoEmpresaJaCadastradoException extends NegocioException {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final String CODIGO_ERRO = "EMPRESA_DOCUMENTO_DUPLICADO";
  private final String documento;

  private DocumentoEmpresaJaCadastradoException(String mensagem, String documento) {
    super(mensagem, HttpStatus.CONFLICT, CODIGO_ERRO);
    this.documento = documento;
  }

  public static DocumentoEmpresaJaCadastradoException comCnpj(String cnpj) {
    return new DocumentoEmpresaJaCadastradoException(
            String.format("O CNPJ %s já possui um cadastro ativo no TaskFlow.", cnpj),
            cnpj
    );
  }
  public static DocumentoEmpresaJaCadastradoException comDocumentoGenerico(String documento) {
    return new DocumentoEmpresaJaCadastradoException(
            "O documento informado já está vinculado a outra empresa.",
            documento
    );
  }
}