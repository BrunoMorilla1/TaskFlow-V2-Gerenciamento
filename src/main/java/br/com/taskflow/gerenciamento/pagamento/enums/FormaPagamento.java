package br.com.taskflow.gerenciamento.pagamento.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum FormaPagamento {

    PIX("PIX", "Pix"),
            BOLETO("BOLETO", "Boleto Bancário"),
                    CREDITO("CREDIT_CARD", "Cartão de Crédito"),
                            DEBITO("DEBIT_CARD", "Cartão de Débito"),
                                    INDEFINIDO("UNDEFINED", "Não selecionado");

    private final String codigoAsaas;
    private final String descricao;

    @JsonCreator
    public static FormaPagamento deCodigoAsaas(String codigo) {
        if (codigo == null || codigo.isBlank()) return INDEFINIDO;

        return Arrays.stream(values())
                .filter(f -> f.codigoAsaas.equalsIgnoreCase(codigo.trim()) ||
                        f.name().equalsIgnoreCase(codigo.trim()))
                .findFirst()
                .orElse(INDEFINIDO);
    }

    public boolean isCartao() {
        return this == CREDITO || this == DEBITO;
    }

    public boolean isPix() {
        return this == PIX;
    }
}
