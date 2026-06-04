package br.com.taskflow.gerenciamento.empresa.dto.requisicao;

import br.com.taskflow.gerenciamento.empresa.enums.Segmentos;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record EmpresaAtualizacaoRequisicao(

        @Size(max = 200, message = "A razão social deve ter no máximo 200 caracteres.")
        String razaoSocial,

        @Size(max = 200, message = "O nome fantasia deve ter no máximo 200 caracteres.")
        String nomeFantasia,

        @Size(max = 14, message = "O CNPJ deve conter no máximo 14 dígitos.")
        String cnpj,

        @Email(message = "O email informado é inválido.")
        @Size(max = 150, message = "O email deve ter no máximo 150 caracteres.")
        String email,

        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
        String telefone,

        @Size(max = 30, message = "A inscrição estadual deve ter no máximo 30 caracteres.")
        String inscricaoEstadual,

        @Size(max = 30, message = "A inscrição municipal deve ter no máximo 30 caracteres.")
        String inscricaoMunicipal,

        @Size(max = 200, message = "O site deve ter no máximo 200 caracteres.")
        String site,

        Segmentos segmentos

) {
}