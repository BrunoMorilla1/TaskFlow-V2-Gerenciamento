package br.com.taskflow.gerenciamento.pagamento.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum NomePlano {
    PRO, BASIC, PREMIUM;

    @JsonCreator
    public static NomePlano fromString(String value) {
        for (NomePlano plano : NomePlano.values()) {
            if (plano.name().equalsIgnoreCase(value)) {
                return plano;
            }
        }
        throw new IllegalArgumentException("Plano inválido: " + value);
    }
}