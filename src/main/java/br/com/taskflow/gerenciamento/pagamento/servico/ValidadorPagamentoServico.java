package br.com.taskflow.gerenciamento.pagamento.servico;

import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.CheckoutRequisicao;
import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import br.com.taskflow.gerenciamento.pagamento.repositorio.AssinaturaRepositorio;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidadorPagamentoServico {

    private final AssinaturaRepositorio assinaturaRepositorio;

    public void validarCheckout(CheckoutRequisicao requisicao, Usuario usuario, String ip) {
        log.info("Servico[Validador] - Iniciando triagem de seguranca para checkout. usuarioId={}, ip={}",
                usuario.getId(), ip);

        validarUsuarioApto(usuario, ip);
        validarDuplicidade(usuario.getId(), ip);
        validarDadosPagamento(requisicao, ip);

        log.info("Servico[Validador] - Checkout autorizado para prosseguir. usuarioId={}, ip={}",
                usuario.getId(), ip);
    }

    private void validarUsuarioApto(Usuario usuario, String ip) {
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            log.warn("Servico[Validador] - Usuario com cadastro incompleto tentando assinar. usuarioId={}, ip={}",
                    usuario.getId(), ip);
            throw new IllegalArgumentException("Cadastro incompleto: e-mail é obrigatório para cobrança.");
        }

        log.debug("Servico[Validador] - Usuario apto para contratacao. usuarioId={}", usuario.getId());
    }

    private void validarDuplicidade(Long usuarioId, String ip) {
        boolean possuiAtiva = assinaturaRepositorio.buscarAssinaturaAtivaPorUsuario(usuarioId).isPresent();

        if (possuiAtiva) {
            log.warn("Servico[Validador] - Bloqueio de assinatura duplicada. usuarioId={}, ip={}",
                    usuarioId, ip);
            throw new IllegalStateException("Você já possui um plano TaskFlow ativo.");
        }
    }

    private void validarDadosPagamento(CheckoutRequisicao requisicao, String ip) {
        BigDecimal valorEsperado = new BigDecimal("49.90");

        if (requisicao.formaPagamento() == null) {
            log.error("Servico[Validador] - Forma de pagamento nao selecionada. ip={}", ip);
            throw new IllegalArgumentException("Selecione uma forma de pagamento válida.");
        }
    }

    public void validarCancelamento(Assinatura assinatura, String ip) {
        log.info("Servico[Validador] - Validando solicitacao de cancelamento. assinaturaId={}, ip={}",
                assinatura.getId(), ip);

        if (assinatura.getStatus() == StatusPagamento.CANCELADO) {
            throw new IllegalStateException("Esta assinatura já está cancelada.");
        }
    }
}