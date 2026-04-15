package br.com.taskflow.gerenciamento.pagamento.eventos;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PagamentoFalhoEvento {
    private final Long usuarioId;
    private final String email;
    private final String ip;
    private final String motivo;
    private final String faturaId;
}