package br.com.taskflow.gerenciamento.pagamento.servico;

import br.com.taskflow.gerenciamento.excecao.FalhaComunicacaoGatewayExcecao;
import br.com.taskflow.gerenciamento.pagamento.cliente.PortaoPagamento;
import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.AsaasPagamentoDTO;
import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.AsaasPixQrCodeDTO;
import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.AsaasSubscricaoResultadoDTO;
import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.CheckoutRequisicao;
import br.com.taskflow.gerenciamento.pagamento.dto.resposta.CheckoutResposta;
import br.com.taskflow.gerenciamento.pagamento.dto.resposta.StatusPagamentoResposta;
import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import br.com.taskflow.gerenciamento.pagamento.entidade.Transacao;
import br.com.taskflow.gerenciamento.pagamento.enums.FormaPagamento;
import br.com.taskflow.gerenciamento.pagamento.enums.NomePlano;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import br.com.taskflow.gerenciamento.pagamento.mapper.AssinaturaMapper;
import br.com.taskflow.gerenciamento.pagamento.repositorio.AssinaturaRepositorio;
import br.com.taskflow.gerenciamento.pagamento.repositorio.TransacaoRepositorio;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import br.com.taskflow.gerenciamento.usuarios.repositorio.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    public CheckoutResposta contratarPlano(CheckoutRequisicao req, String emailUsuario, String ip) {
        log.info("ASSINATURA[INICIO] - email={} forma={} plano={}", emailUsuario, req.formaPagamento(), req.nomePlano());

                validar(req);
        Usuario usuario = buscarUsuario(emailUsuario);
        if (assinaturaRepositorio.possuiAssinaturaAtiva(usuario.getId())) {
            throw new IllegalStateException("Você já possui uma assinatura ativa.");
        }

        if (usuario.getAsaasId() == null || usuario.getAsaasId().isBlank()) {
            String customerId = portaoPagamento.criarOuRecuperarCliente(usuario, req.documentoCliente());
            usuario.setAsaasId(customerId);
            usuarioRepositorio.save(usuario);
        }

        Assinatura assinatura = Assinatura.builder()
                .usuario(usuario)
                .nomePlano(req.nomePlano() != null ? req.nomePlano() : NomePlano.PRO)
                .valor(new BigDecimal("49.90"))
                        .status(StatusPagamento.PENDENTE)
                        .proximoVencimento(LocalDate.now().plusDays(1))
                        .criadoPor(emailUsuario)
                        .atualizadoPor(emailUsuario)
                        .deletado(false)
                        .build();
        assinatura = assinaturaRepositorio.save(assinatura);

        AsaasSubscricaoResultadoDTO subscriptionAsaas;
        try {
            subscriptionAsaas = portaoPagamento.gerarAssinatura(assinatura, req, ip);
        } catch (FalhaComunicacaoGatewayExcecao e) {
            log.error("ASSINATURA[FALHA-GATEWAY] - {}", e.getMessage());
            throw e;
        }
        assinatura.setAsaasId(subscriptionAsaas.id());
        assinaturaRepositorio.save(assinatura);

        AsaasPagamentoDTO primeira = portaoPagamento.buscarPrimeiroPagamentoAssinatura(subscriptionAsaas.id());

        Transacao transacao = Transacao.builder()
                .assinatura(assinatura)
                .asaasId(primeira.id())
                .valor(BigDecimal.valueOf(primeira.value() != null ? primeira.value() : 49.90))
                .status(StatusPagamento.deCodigoAsaas(primeira.status()))
                .formaPagamento(req.formaPagamento())
                .dataVencimento(LocalDateTime.now().plusDays(1))
                .urlPagamento(primeira.invoiceUrl())
                .criadoPor(emailUsuario)
                .build();
        transacaoRepositorio.save(transacao);

        AsaasPixQrCodeDTO qr = null;
        if (req.formaPagamento().isPix()) {
            qr = portaoPagamento.buscarPixQrCode(primeira.id());
        }

        return assinaturaMapper.paraCheckoutResposta(assinatura, primeira, qr, req.formaPagamento());
    }

    @Transactional(readOnly = true)
    public StatusPagamentoResposta buscarStatusPorUsuario(String emailUsuario, String ip) {
        Usuario usuario = buscarUsuario(emailUsuario);
        Transacao ultima = transacaoRepositorio.findFirstByAssinaturaUsuarioIdOrderByCriadoEmDesc(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Nenhuma transação encontrada para este usuário."));
        return assinaturaMapper.paraStatusResposta(ultima);
    }


    @Transactional
    public StatusPagamentoResposta sincronizarStatus(String emailUsuario, String paymentId) {
        Usuario usuario = buscarUsuario(emailUsuario);
        Transacao transacao = transacaoRepositorio.findByAsaasId(paymentId)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada."));

        if (!transacao.getAssinatura().getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalStateException("Acesso negado a essa transação.");
        }

        StatusPagamento atual = portaoPagamento.consultarStatusPagamento(paymentId);
        if (atual != transacao.getStatus()) {
            transacao.setStatus(atual);
            if (atual.isPago()) {
                transacao.setDataPagamento(LocalDateTime.now());
                Assinatura a = transacao.getAssinatura();
                a.setStatus(StatusPagamento.CONFIRMADO);
                a.setProximoVencimento(LocalDate.now().plusDays(30));
                assinaturaRepositorio.save(a);
            }
            transacaoRepositorio.save(transacao);
        }
        return assinaturaMapper.paraStatusResposta(transacao);
    }

    @Transactional
    public void cancelarAssinatura(String emailUsuario, String ip) {
        Usuario usuario = buscarUsuario(emailUsuario);
        Assinatura ativa = assinaturaRepositorio.findByUsuarioIdAndStatusAtivo(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("Nenhuma assinatura ativa para cancelar."));
                        portaoPagamento.cancelarAssinatura(ativa.getAsaasId());
        ativa.setDeletado(true);
        ativa.setStatus(StatusPagamento.CANCELADO);
        assinaturaRepositorio.save(ativa);
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepositorio.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }

    private void validar(CheckoutRequisicao req) {
        if (req == null || req.formaPagamento() == null) {
            throw new IllegalArgumentException("Forma de pagamento obrigatória.");
        }
        if (req.formaPagamento().isCartao() && req.cartao() == null) {
            throw new IllegalArgumentException("Dados do cartão são obrigatórios para pagamento com cartão.");
        }
        if (req.formaPagamento() == FormaPagamento.INDEFINIDO) {
            throw new IllegalArgumentException("Forma de pagamento inválida.");
        }
    }
}