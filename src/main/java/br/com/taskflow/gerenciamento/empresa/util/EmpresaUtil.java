package br.com.taskflow.gerenciamento.empresa.util;

@Deprecated
public class EmpresaUtil {

    @Deprecated
    public static String limparCnpj(String cnpj) {
        return ValidadorCnpj.limparCnpj(cnpj);
    }
}