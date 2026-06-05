package br.com.taskflow.gerenciamento.pagamento.servico;

import br.com.taskflow.gerenciamento.pagamento.mapper.AssinaturaMapper;
import br.com.taskflow.gerenciamento.pagamento.cliente.PortaoPagamento;
import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.CheckoutRequisicao;
import br.com.taskflow.gerenciamento.pagamento.dto.resposta.CheckoutResposta;
import br.com.taskflow.gerenciamento.pagamento.dto.resposta.StatusPagamentoResposta;
import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import br.com.taskflow.gerenciamento.pagamento.entidade.Transacao;
import br.com.taskflow.gerenciamento.pagamento.repositorio.AssinaturaRepositorio;
import br.com.taskflow.gerenciamento.pagamento.repositorio.TransacaoRepositorio;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import br.com.taskflow.gerenciamento.usuarios.repositorio.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssinaturaServico {

    private final AssinaturaRepositorio assinaturaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final TransacaoRepositorio transacaoRepositorio;
    private final PortaoPagamento portaoPagamento;
    private final AssinaturaMapper assinaturaMapper;

    @Transactional
    public CheckoutResposta contratarPlano(CheckoutRequisicao requisicao, String emailUsuario, String ip) {
        log.info("CONTRATACAO[INICIO] - Email: {}, Plano: {}", emailUsuario, requisicao.nomePlano());

        validarRequisicao(requisicao);
        Usuario usuarioLogado = buscarUsuarioOuFalhar(emailUsuario);
        verificarAssinaturaExistente(usuarioLogado.getId(), ip);
        vincularUsuarioAoGateway(usuarioLogado, ip);

        Assinatura assinatura = assinaturaMapper.paraEntidadeInicial(usuarioLogado, requisicao, emailUsuario);
        Assinatura assinaturaSalva = assinaturaRepositorio.save(assinatura);

        try {
            String asaasId = portaoPagamento.gerarAssinatura(assinaturaSalva);
            assinaturaSalva.setAsaasId(asaasId);
            assinaturaRepositorio.save(assinaturaSalva);

            return assinaturaMapper.paraCheckoutResposta(assinaturaSalva, null, "Assinatura registrada com sucesso.");
        } catch (Exception e) {
            log.error("CONTRATACAO[FALHA] - Erro no gateway: {}", e.getMessage());
            throw new RuntimeException("Erro ao processar pagamento no provedor externo.");
        }
    }

    @Transactional(readOnly = true)
    public StatusPagamentoResposta buscarStatusPorUsuario(String emailUsuario, String ip) {
        log.info("STATUS[BUSCA] - Usuario: {}, IP: {}", emailUsuario, ip);

        Usuario usuario = buscarUsuarioOuFalhar(emailUsuario);

        Transacao últimaTransacao = transacaoRepositorio.findFirstByAssinaturaUsuarioIdOrderByCriadoEmDesc(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Nenhuma transação encontrada para este usuário."));

        return assinaturaMapper.paraStatusResposta(últimaTransacao);
    }

    @Transactional
    public void cancelarAssinatura(String emailUsuario, String ip) {
        log.info("CANCELAMENTO[SOLICITACAO] - Usuario: {}, IP: {}", emailUsuario, ip);

        Usuario usuario = buscarUsuarioOuFalhar(emailUsuario);
        Assinatura assinaturaAtiva = assinaturaRepositorio.findByUsuarioIdAndStatusAtivo(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("Não foi encontrada uma assinatura ativa para cancelar."));

        try {
            portaoPagamento.cancelarAssinatura(assinaturaAtiva.getAsaasId());

            assinaturaAtiva.setDeletado(true);
            assinaturaRepositorio.save(assinaturaAtiva);

            log.info("CANCELAMENTO[SUCESSO] - Assinatura {} cancelada.", assinaturaAtiva.getAsaasId());
        } catch (Exception e) {
            log.error("CANCELAMENTO[ERRO] - Falha ao cancelar no gateway: {}", e.getMessage());
            throw new RuntimeException("Não foi possível cancelar a assinatura junto ao provedor.");
        }
    }

    private void vincularUsuarioAoGateway(Usuario usuario, String ip) {
        if (usuario.getAsaasId() == null || usuario.getAsaasId().isBlank()) {
            try {
                String asaasId = portaoPagamento.criarOuRecuperarCliente(usuario);
                usuario.setAsaasId(asaasId);
                usuarioRepositorio.save(usuario);
            } catch (Exception e) {
                throw new RuntimeException("Falha ao sincronizar cliente com gateway.");
            }
        }
    }

    private void verificarAssinaturaExistente(Long usuarioId, String ip) {
        if (assinaturaRepositorio.possuiAssinaturaAtiva(usuarioId)) {
            throw new IllegalStateException("Você já possui uma assinatura ativa.");
        }
    }

    private Usuario buscarUsuarioOuFalhar(String email) {
        return usuarioRepositorio.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }

    private void validarRequisicao(CheckoutRequisicao requisicao) {
        if (Objects.isNull(requisicao) || Objects.isNull(requisicao.nomePlano())) {
            throw new IllegalArgumentException("Requisição inválida.");
        }
    }
}