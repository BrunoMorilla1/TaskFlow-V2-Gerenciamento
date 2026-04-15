package br.com.taskflow.gerenciamento.empresa.util;

public class EmpresaUtil {
    public static String limparCnpj(String cnpj) {
        return cnpj == null ? null : cnpj.replaceAll("\\D", "").trim();
    }
}

