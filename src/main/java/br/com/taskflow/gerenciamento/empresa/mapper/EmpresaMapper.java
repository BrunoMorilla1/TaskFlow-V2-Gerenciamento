package br.com.taskflow.gerenciamento.empresa.mapper;

import br.com.taskflow.gerenciamento.empresa.dto.externo.BrasilApiCnpj;
import br.com.taskflow.gerenciamento.empresa.dto.requisicao.EmpresaAtualizacaoRequisicao;
import br.com.taskflow.gerenciamento.empresa.dto.requisicao.EmpresaCriacaoRequisicao;
import br.com.taskflow.gerenciamento.empresa.dto.resposta.EmpresaResponse;
import br.com.taskflow.gerenciamento.empresa.dto.resposta.EmpresaResumoResponse;
import br.com.taskflow.gerenciamento.empresa.entidade.Empresa;
import br.com.taskflow.gerenciamento.empresa.enums.StatusEmpresa;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import org.springframework.stereotype.Component;

@Component
public class EmpresaMapper {

    public Empresa paraEntidade(EmpresaCriacaoRequisicao requisicao,
                                BrasilApiCnpj dadosApi,
                                Usuario usuarioDono,
                                String usuarioAuditoria) {

        if (requisicao == null) {
            throw new IllegalArgumentException("A requisição de criação da empresa é obrigatória.");
        }

        if (usuarioDono == null || usuarioDono.getId() == null) {
            throw new IllegalArgumentException("O usuário dono da empresa é obrigatório.");
        }

        return Empresa.builder()
                .razaoSocial(dadosApi != null ? dadosApi.razaoSocial() : requisicao.razaoSocial())
                .nomeFantasia(dadosApi != null ? dadosApi.nomeFantasia() : requisicao.nomeFantasia())
                .cnpj(requisicao.cnpj().replaceAll("\\D", ""))
                .email(requisicao.email())
                .telefone(requisicao.telefone())
                .inscricaoEstadual(requisicao.inscricaoEstadual())
                .inscricaoMunicipal(requisicao.inscricaoMunicipal())
                .site(requisicao.site())
                .logradouro(dadosApi != null ? dadosApi.logradouro() : null)
                .bairro(dadosApi != null ? dadosApi.bairro() : null)
                .municipio(dadosApi != null ? dadosApi.municipio() : null)
                .uf(dadosApi != null ? dadosApi.uf() : null)
                .status(StatusEmpresa.ATIVA)
                .ativo(true)
                .usuarioDono(usuarioDono)
                .criadoPor(usuarioAuditoria)
                .atualizadoPor(usuarioAuditoria)
                .build();
    }

    public void atualizarEntidade(Empresa empresa, EmpresaAtualizacaoRequisicao requisicao, String usuarioAuditoria) {
        if (empresa == null) {
            throw new IllegalArgumentException("A empresa para atualização é obrigatória.");
        }

        if (requisicao == null) {
            throw new IllegalArgumentException("A requisição de atualização da empresa é obrigatória.");
        }

        if (requisicao.razaoSocial() != null) {
            empresa.setRazaoSocial(requisicao.razaoSocial());
        }

        if (requisicao.nomeFantasia() != null) {
            empresa.setNomeFantasia(requisicao.nomeFantasia());
        }

        if (requisicao.cnpj() != null) {
            empresa.setCnpj(requisicao.cnpj());
        }

        if (requisicao.email() != null) {
            empresa.setEmail(requisicao.email());
        }

        if (requisicao.telefone() != null) {
            empresa.setTelefone(requisicao.telefone());
        }

        if (requisicao.inscricaoEstadual() != null) {
            empresa.setInscricaoEstadual(requisicao.inscricaoEstadual());
        }

        if (requisicao.inscricaoMunicipal() != null) {
            empresa.setInscricaoMunicipal(requisicao.inscricaoMunicipal());
        }

        if (requisicao.site() != null) {
            empresa.setSite(requisicao.site());
        }

        empresa.setAtualizadoPor(usuarioAuditoria);
    }

    public EmpresaResponse paraResponse(Empresa empresa) {
        if (empresa == null) {
            throw new IllegalArgumentException("A empresa é obrigatória para geração do response.");
        }
        return new EmpresaResponse(
                empresa.getId(),
                empresa.getRazaoSocial(),
                empresa.getNomeFantasia(),
                empresa.getCnpj(),
                empresa.getEmail(),
                empresa.getTelefone(),
                empresa.getLogradouro(),
                empresa.getNumero(),
                empresa.getBairro(),
                empresa.getMunicipio(),
                empresa.getUf(),
                empresa.getCep(),
                empresa.getSegmentos(),
                empresa.getInscricaoEstadual(),
                empresa.getInscricaoMunicipal(),
                empresa.getSite(),
                empresa.getStatus(),
                empresa.isAtivo(),
                empresa.getCriadoEm(),
                empresa.getAtualizadoEm()
        );
    }

    public EmpresaResumoResponse paraResumoResponse(Empresa empresa) {
        if (empresa == null) {
            throw new IllegalArgumentException("A empresa é obrigatória para geração do resumo.");
        }

        return new EmpresaResumoResponse(
                empresa.getId(),
                empresa.getNomeFantasia(),
                empresa.getCnpj(),
                empresa.getSegmentos(),
                empresa.getStatus(),
                empresa.isAtivo()
        );
    }
}