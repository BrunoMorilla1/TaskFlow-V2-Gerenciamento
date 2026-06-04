package br.com.taskflow.gerenciamento.empresa.servico;

import br.com.taskflow.gerenciamento.empresa.cliente.BrasilApiCliente;
import br.com.taskflow.gerenciamento.empresa.dto.externo.BrasilApiCnpj;
import br.com.taskflow.gerenciamento.empresa.dto.requisicao.EmpresaAtualizacaoRequisicao;
import br.com.taskflow.gerenciamento.empresa.dto.requisicao.EmpresaCriacaoRequisicao;
import br.com.taskflow.gerenciamento.empresa.dto.resposta.EmpresaResponse;
import br.com.taskflow.gerenciamento.empresa.dto.resposta.EmpresaResumoResponse;
import br.com.taskflow.gerenciamento.empresa.entidade.Empresa;
import br.com.taskflow.gerenciamento.excecao.DocumentoEmpresaJaCadastradoException;
import br.com.taskflow.gerenciamento.excecao.EmpresaNaoEncontradaException;
import br.com.taskflow.gerenciamento.empresa.mapper.EmpresaMapper;
import br.com.taskflow.gerenciamento.empresa.repositorio.EmpresaRepositorio;
import br.com.taskflow.gerenciamento.empresa.util.EmpresaUtil; // Importado o seu utilitário
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import br.com.taskflow.gerenciamento.usuarios.repositorio.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaServico {

    private final EmpresaRepositorio empresaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final EmpresaMapper empresaMapper;
    private final BrasilApiCliente brasilApiClient;

    @Transactional
    public EmpresaResponse criarEmpresa(EmpresaCriacaoRequisicao requisicao, String emailUsuario, String ip) {
        validarRequisicaoCriacao(requisicao);
        Usuario usuarioLogado = buscarUsuarioOuFalhar(emailUsuario);

        final String cnpjNormalizado = EmpresaUtil.limparCnpj(requisicao.cnpj());

        log.info("Servico[Empresa] - Iniciando criação de empresa. usuarioId={}, cnpj={}, ip={}",
                usuarioLogado.getId(), cnpjNormalizado, ip);

        validarCnpjDuplicado(cnpjNormalizado);

        var dadosExternos = buscarDadosExternos(cnpjNormalizado);

        Empresa empresa = empresaMapper.paraEntidade(
                requisicao,
                dadosExternos,
                usuarioLogado,
                usuarioLogado.getEmail()
        );

        empresa.setCnpj(cnpjNormalizado);

        Empresa empresaSalva = empresaRepositorio.save(empresa);

        log.info("Servico[Empresa] - Empresa criada com sucesso. id={}, usuarioId={}, cnpj={}, ip={}",
                empresaSalva.getId(), usuarioLogado.getId(), empresaSalva.getCnpj(), ip);

        return empresaMapper.paraResponse(empresaSalva);
    }

    @Transactional(readOnly = true)
    public EmpresaResponse buscarPorId(Long id, String emailUsuario, String ip) {
        validarId(id);
        Usuario usuarioLogado = buscarUsuarioOuFalhar(emailUsuario);

        log.info("Servico[Empresa] - Buscando empresa por id. empresaId={}, usuarioId={}, ip={}",
                id, usuarioLogado.getId(), ip);

        Empresa empresa = buscarEntidadePorIdEUsuario(id, usuarioLogado.getId());

        return empresaMapper.paraResponse(empresa);
    }

    @Transactional(readOnly = true)
    public List<EmpresaResumoResponse> listarMinhasEmpresas(String emailUsuario, String ip) {
        Usuario usuarioLogado = buscarUsuarioOuFalhar(emailUsuario);

        log.info("Servico[Empresa] - Listando empresas do usuário. usuarioId={}, ip={}",
                usuarioLogado.getId(), ip);

        return empresaRepositorio.findAllByUsuarioDonoId(usuarioLogado.getId())
                .stream()
                .map(empresaMapper::paraResumoResponse)
                .toList();
    }

    @Transactional
    public EmpresaResponse atualizarEmpresa(Long id, EmpresaAtualizacaoRequisicao requisicao, String emailUsuario, String ip) {
        validarId(id);
        validarRequisicaoAtualizacao(requisicao);
        Usuario usuarioLogado = buscarUsuarioOuFalhar(emailUsuario);

        log.info("Servico[Empresa] - Iniciando atualização de empresa. empresaId={}, usuarioId={}, ip={}",
                id, usuarioLogado.getId(), ip);

        Empresa empresa = buscarEntidadePorIdEUsuario(id, usuarioLogado.getId());

        String cnpjAtualizado = EmpresaUtil.limparCnpj(requisicao.cnpj());
        if (cnpjAtualizado != null && !cnpjAtualizado.equals(empresa.getCnpj())) {
            validarCnpjDuplicadoParaAtualizacao(cnpjAtualizado, id);
        }

        empresaMapper.atualizarEntidade(empresa, requisicao, usuarioLogado.getEmail());

        Empresa empresaAtualizada = empresaRepositorio.save(empresa);

        log.info("Servico[Empresa] - Empresa atualizada com sucesso. empresaId={}, usuarioId={}, ip={}",
                empresaAtualizada.getId(), usuarioLogado.getId(), ip);

        return empresaMapper.paraResponse(empresaAtualizada);
    }

    @Transactional
    public void deletarEmpresa(Long id, String emailUsuario, String ip) {
        validarId(id);
        Usuario usuarioLogado = buscarUsuarioOuFalhar(emailUsuario);

        log.info("Servico[Empresa] - Iniciando exclusão de empresa. empresaId={}, usuarioId={}, ip={}",
                id, usuarioLogado.getId(), ip);

        Empresa empresa = buscarEntidadePorIdEUsuario(id, usuarioLogado.getId());
        empresaRepositorio.delete(empresa);
    }

    private Usuario buscarUsuarioOuFalhar(String email) {
        return usuarioRepositorio.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Servico[Empresa] - Usuário não encontrado para o email: {}", email);
                    return new IllegalArgumentException("O usuário autenticado é obrigatório.");
                });
    }

    private Empresa buscarEntidadePorIdEUsuario(Long empresaId, Long usuarioId) {
        return empresaRepositorio.findByIdAndUsuarioDonoId(empresaId, usuarioId)
                .orElseThrow(() -> {
                    log.warn("Servico[Empresa] - Empresa não encontrada ou não pertence ao usuário. empresaId={}, usuarioId={}",
                            empresaId, usuarioId);
                    return EmpresaNaoEncontradaException.porUsuario(empresaId, usuarioId);
                });
    }

    private void validarCnpjDuplicado(String cnpj) {
        if (empresaRepositorio.existsByCnpj(cnpj)) {
            log.warn("Servico[Empresa] - Tentativa de uso de CNPJ já cadastrado. cnpj={}", cnpj);
            throw DocumentoEmpresaJaCadastradoException.comCnpj(cnpj);
        }
    }

    private void validarCnpjDuplicadoParaAtualizacao(String cnpj, Long empresaId) {
        if (empresaRepositorio.existsByCnpjAndIdNot(cnpj, empresaId)) {
            log.warn("Servico[Empresa] - Tentativa de atualização com CNPJ já cadastrado. empresaId={}, cnpj={}",
                    empresaId, cnpj);
            throw DocumentoEmpresaJaCadastradoException.comCnpj(cnpj);
        }
    }

    private void validarRequisicaoCriacao(EmpresaCriacaoRequisicao requisicao) {
        if (requisicao == null) throw new IllegalArgumentException("Os dados da empresa são obrigatórios.");
    }

    private void validarRequisicaoAtualizacao(EmpresaAtualizacaoRequisicao requisicao) {
        if (requisicao == null) throw new IllegalArgumentException("Os dados da atualização da empresa são obrigatórios.");
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("O id da empresa é inválido.");
    }

    private BrasilApiCnpj buscarDadosExternos(String cnpj) {
        try {
            log.info("Servico[Empresa] - Consultando BrasilAPI para CNPJ: {}", cnpj);
            return brasilApiClient.buscarCnpj(cnpj);
        } catch (feign.FeignException.NotFound e) {
            log.warn("Servico[Empresa] - CNPJ {} não encontrado na base da Receita.", cnpj);
            return null;
        } catch (Exception e) {
            log.error("Servico[Empresa] - Falha na integração com BrasilAPI para o CNPJ {}. Motivo: {}", cnpj, e.getMessage());
            return null;
        }
    }
}