package br.com.taskflow.gerenciamento.pagamento.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum StatusPagamento {

    PENDENTE("PENDING", "Aguardando Pagamento", "#FFA500"),
    RECEBIDO("RECEIVED", "Pagamento Recebido", "#008000"),
    CONFIRMADO("CONFIRMED", "Pagamento Confirmado", "#2E8B57"),
    VENCIDO("OVERDUE", "Pagamento Vencido", "#FF0000"),
    ESTORNADO("REFUNDED", "Pagamento Estornado", "#808080"),
    CANCELADO("DELETED", "Cobrança Cancelada", "#000000"),
    FALHA_CARTAO("DUNNING", "Falha no Cartão", "#B22222"),
    EM_PROCESSAMENTO("PROCESSING", "Em Processamento", "#4169E1"),
    INDEFINIDO("UNDEFINED", "Status Desconhecido", "#D3D3D3");

    private final String codigoAsaas;
    private final String descricao;
    private final String corHex;

    public static StatusPagamento deCodigoAsaas(String codigo) {
        if (codigo == null) return INDEFINIDO;

        return Arrays.stream(StatusPagamento.values())
                .filter(s -> s.getCodigoAsaas().equalsIgnoreCase(codigo.trim()))
                .findFirst()
                .orElse(INDEFINIDO);
    }

    public boolean isPago() {
        return this == RECEBIDO || this == CONFIRMADO;
    }

    public boolean isFinalizado() {
        return isPago() || this == ESTORNADO || this == CANCELADO;
    }

    public boolean podeSerCancelado() {
        return this == PENDENTE || this == VENCIDO;
    }
}