package br.com.taskflow.gerenciamento.empresa.dto.requisicao;

import br.com.taskflow.gerenciamento.empresa.enums.StatusEmpresa;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record EmpresaFiltroRequisicao(

        @Size(max = 200, message = "A razão social deve ter no máximo 200 caracteres.")
        String razaoSocial,

        @Size(max = 200, message = "O nome fantasia deve ter no máximo 200 caracteres.")
        String nomeFantasia,

        @Size(max = 14, message = "O CNPJ deve conter no máximo 14 dígitos.")
        String cnpj,

        StatusEmpresa status,

        Boolean ativo,

        @Min(value = 0, message = "A página não pode ser negativa.")
        Integer pagina,

        @Min(value = 1, message = "O tamanho mínimo da página é 1.")
        @Max(value = 100, message = "O tamanho máximo da página é 100.")
        Integer tamanho

) {
}