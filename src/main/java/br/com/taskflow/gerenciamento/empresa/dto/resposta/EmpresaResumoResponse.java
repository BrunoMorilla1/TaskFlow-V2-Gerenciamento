package br.com.taskflow.gerenciamento.empresa.dto.resposta;

import br.com.taskflow.gerenciamento.empresa.enums.Segmentos;
import br.com.taskflow.gerenciamento.empresa.enums.StatusEmpresa;

public record EmpresaResumoResponse(

        Long id,

        String nomeFantasia,

        String cnpj,

        Segmentos segmentos,

        StatusEmpresa status,

        boolean ativo

) {
}