package br.com.taskflow.gerenciamento.empresa.dto.externo;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BrasilApiCnpj(
        String cnpj,
        @JsonProperty("razao_social") String razaoSocial,
        @JsonProperty("nome_fantasia") String nomeFantasia,
        String logradouro,
        String numero,
        String bairro,
        String municipio,
        String uf,
        String cep
) {}