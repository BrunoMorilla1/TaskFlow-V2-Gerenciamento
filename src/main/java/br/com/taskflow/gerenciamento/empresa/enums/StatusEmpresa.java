package br.com.taskflow.gerenciamento.empresa.enums;

public enum StatusEmpresa {

    ATIVA,
    INATIVA,
    BLOQUEADA;

    public boolean isAtiva() {
        return this == ATIVA;
    }

    public boolean isInativa() {
        return this == INATIVA;
    }

    public boolean isBloqueada() {
        return this == BLOQUEADA;
    }

    public boolean permiteOperacao() {
        return this == ATIVA;
    }
}