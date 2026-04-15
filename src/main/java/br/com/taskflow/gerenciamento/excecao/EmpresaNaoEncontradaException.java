package br.com.taskflow.gerenciamento.excecao;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@Getter
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EmpresaNaoEncontradaException extends NegocioException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String CODIGO_ERRO = "EMPRESA_NAO_ENCONTRADA";
    private final Long empresaId;
    private final Long usuarioId;

    private EmpresaNaoEncontradaException(String mensagem, Long empresaId, Long usuarioId) {
        super(mensagem, HttpStatus.NOT_FOUND, CODIGO_ERRO);
        this.empresaId = empresaId;
        this.usuarioId = usuarioId;
    }

    public static EmpresaNaoEncontradaException porId(Long id) {
        return new EmpresaNaoEncontradaException(
                String.format("Empresa não encontrada com o ID: %d.", id),
                id,
                null
        );
    }

    public static EmpresaNaoEncontradaException porUsuario(Long empresaId, Long usuarioId) {
        return new EmpresaNaoEncontradaException(
                String.format("Empresa com ID %d não encontrada para o usuário %d.", empresaId, usuarioId),
                empresaId,
                usuarioId
        );
    }
}