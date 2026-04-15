package br.com.taskflow.gerenciamento.pagamento.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum FormaPagamento {

    PIX("PIX", "Pix"),
    BOLETO("BOLETO", "Boleto Bancário"),
    CREDITO("CREDIT_CARD", "Cartão de Crédito"),
    INDEFINIDO("UNDEFINED", "Não selecionado");

    private final String codigoAsaas;
    private final String descricao;

    public static FormaPagamento deCodigoAsaas(String codigo) {
        if (codigo == null) return INDEFINIDO;

        return Arrays.stream(FormaPagamento.values())
                .filter(f -> f.getCodigoAsaas().equalsIgnoreCase(codigo.trim()))
                .findFirst()
                .orElse(INDEFINIDO);
    }

    public static FormaPagamento deString(String valor) {
        if (valor == null) return INDEFINIDO;

        return Arrays.stream(FormaPagamento.values())
                .filter(f -> f.name().equalsIgnoreCase(valor.trim()) || f.getDescricao().equalsIgnoreCase(valor.trim()))
                .findFirst()
                .orElse(INDEFINIDO);
    }

    public boolean isCartao() {
        return this == CREDITO;
    }
}