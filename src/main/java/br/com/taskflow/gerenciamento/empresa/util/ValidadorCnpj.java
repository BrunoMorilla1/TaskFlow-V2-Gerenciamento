package br.com.taskflow.gerenciamento.empresa.util;

public class ValidadorCnpj {

    public static String limparCnpj(String cnpj) {
        return cnpj.replaceAll("[^0-9]", "");
    }

    public static boolean validar(String cnpj) {
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int digito1 = calcularDigito(cnpj.substring(0, 12), pesos1);
        int digito2 = calcularDigito(cnpj.substring(0, 13), pesos2);

        return cnpj.charAt(12) - '0' == digito1 && cnpj.charAt(13) - '0' == digito2;
    }

    private static int calcularDigito(String trecho, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (trecho.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}