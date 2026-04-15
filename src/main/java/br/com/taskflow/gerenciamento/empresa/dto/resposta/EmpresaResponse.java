package br.com.taskflow.gerenciamento.empresa.dto.resposta;

import br.com.taskflow.gerenciamento.empresa.enums.StatusEmpresa;
import java.time.LocalDateTime;

public record EmpresaResponse(
        Long id,
        String razaoSocial,
        String nomeFantasia,
        String cnpj,
        String email,
        String telefone,
        String logradouro,
        String numero,
        String bairro,
        String municipio,
        String uf,
        String cep,
        String inscricaoEstadual,
        String inscricaoMunicipal,
        String site,
        StatusEmpresa status,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {}