package br.com.taskflow.gerenciamento.pagamento.servico;

import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.CheckoutRequisicao;
import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import br.com.taskflow.gerenciamento.pagamento.enums.FormaPagamento;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import br.com.taskflow.gerenciamento.pagamento.repositorio.AssinaturaRepositorio;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidadorPagamentoServico {

    private final AssinaturaRepositorio assinaturaRepositorio;

    /**
     * Valida todos os pré-requisitos de um checkout antes de qualquer chamada ao gateway.
     * Falha rápido para evitar cobranças indevidas ou estados inconsistentes.
     */
    public void validarCheckout(CheckoutRequisicao requisicao, Usuario usuario, String ip) {
        log.info("Validador[Checkout] - Iniciando triagem. usuarioId={} ip={}", usuario.getId(), ip);

        validarUsuarioApto(usuario, ip);
        validarDuplicidade(usuario.getId(), ip);
        validarDadosPagamento(requisicao, ip);

        log.info("Validador[Checkout] - Triagem aprovada. usuarioId={} ip={}", usuario.getId(), ip);
    }

    /**
     * Valida se o cancelamento é permitido para a assinatura informada.
     */
    public void validarCancelamento(Assinatura assinatura, String ip) {
        log.info("Validador[Cancelamento] - Validando. assinaturaId={} ip={}", assinatura.getId(), ip);

        if (assinatura.getStatus() == StatusPagamento.CANCELADO) {
            throw new IllegalStateException("Esta assinatura já está cancelada.");
        }

        if (assinatura.getAsaasId() == null || assinatura.getAsaasId().isBlank()) {
            log.error("Validador[Cancelamento] - Assinatura sem asaasId. assinaturaId={}", assinatura.getId());
            throw new IllegalStateException("Assinatura sem vínculo no gateway. Contate o suporte.");
        }
    }

    // ── VALIDAÇÕES PRIVADAS ────────────────────────────────────────────────────

    private void validarUsuarioApto(Usuario usuario, String ip) {
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            log.warn("Validador[Usuario] - E-mail ausente. usuarioId={} ip={}", usuario.getId(), ip);
            throw new IllegalArgumentException("Cadastro incompleto: e-mail é obrigatório para cobrança.");
        }
    }

    private void validarDuplicidade(Long usuarioId, String ip) {
        boolean possuiAtiva = assinaturaRepositorio.buscarAssinaturaAtivaPorUsuario(usuarioId).isPresent();
        if (possuiAtiva) {
            log.warn("Validador[Duplicidade] - Bloqueio: assinatura ativa existente. usuarioId={} ip={}", usuarioId, ip);
            throw new IllegalStateException("Você já possui um plano TaskFlow ativo.");
        }
    }

    private void validarDadosPagamento(CheckoutRequisicao requisicao, String ip) {
        if (requisicao.formaPagamento() == null) {
            log.error("Validador[Pagamento] - Forma de pagamento nula. ip={}", ip);
            throw new IllegalArgumentException("Selecione uma forma de pagamento válida.");
        }

        if (requisicao.formaPagamento() == FormaPagamento.INDEFINIDO) {
            log.error("Validador[Pagamento] - Forma INDEFINIDO recebida. ip={}", ip);
            throw new IllegalArgumentException("Forma de pagamento inválida.");
        }

        // O Asaas NÃO aceita PIX para assinaturas recorrentes (/subscriptions).
        // PIX só é suportado em cobranças avulsas (/payments).
        if (requisicao.formaPagamento().isPix()) {
            log.warn("Validador[Pagamento] - PIX rejeitado para assinatura recorrente. ip={}", ip);
            throw new IllegalArgumentException(
                    "PIX não está disponível para assinaturas recorrentes. " +
                            "Utilize cartão de crédito ou boleto bancário.");
        }

        if (requisicao.formaPagamento().isCartao()) {
            if (requisicao.cartao() == null) {
                throw new IllegalArgumentException("Dados do cartão são obrigatórios para pagamento com cartão.");
            }
            validarDadosCartao(requisicao.cartao(), ip);
        }

        if (requisicao.documentoCliente() == null || requisicao.documentoCliente().isBlank()) {
            throw new IllegalArgumentException("CPF ou CNPJ é obrigatório para cobrança.");
        }
    }

    private void validarDadosCartao(CheckoutRequisicao.DadosCartaoRequisicao cartao, String ip) {
        if (cartao.nomeNoCartao() == null || cartao.nomeNoCartao().isBlank()) {
            throw new IllegalArgumentException("Nome no cartão é obrigatório.");
        }
        if (cartao.numeroCartao() == null || cartao.numeroCartao().isBlank()) {
            throw new IllegalArgumentException("Número do cartão é obrigatório.");
        }
        if (cartao.mesValidade() == null || cartao.mesValidade().isBlank()) {
            throw new IllegalArgumentException("Mês de validade do cartão é obrigatório.");
        }
        if (cartao.anoValidade() == null || cartao.anoValidade().isBlank()) {
            throw new IllegalArgumentException("Ano de validade do cartão é obrigatório.");
        }
        if (cartao.cvv() == null || cartao.cvv().isBlank()) {
            throw new IllegalArgumentException("CVV do cartão é obrigatório.");
        }
    }
}