package br.com.taskflow.gerenciamento.empresa.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidador implements ConstraintValidator<CnpjValido, String> {

    @Override
    public boolean isValid(String cnpj, ConstraintValidatorContext context) {
        if (cnpj == null || cnpj.isBlank()) return false;

        String cnpjLimpo = ValidadorCnpj.limparCnpj(cnpj);

        if (cnpjLimpo.length() != 14) return false;
        if (cnpjLimpo.chars().distinct().count() == 1) return false;

        return ValidadorCnpj.validar(cnpjLimpo);
    }
}