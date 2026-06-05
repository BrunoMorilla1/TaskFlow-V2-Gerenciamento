package br.com.taskflow.gerenciamento.pagamento.mapper;

import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.CheckoutRequisicao;
import br.com.taskflow.gerenciamento.pagamento.dto.resposta.CheckoutResposta;
import br.com.taskflow.gerenciamento.pagamento.dto.resposta.StatusPagamentoResposta;
import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import br.com.taskflow.gerenciamento.pagamento.entidade.Transacao;
import br.com.taskflow.gerenciamento.pagamento.enums.NomePlano;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Slf4j
public class AssinaturaMapper {


    public CheckoutResposta paraCheckoutResposta(Assinatura assinatura, String invoiceUrl, String mensagem) {
        if (assinatura == null) {
            log.error("Mapper[Assinatura] - Tentativa de mapear assinatura nula para CheckoutResposta.");
            throw new IllegalArgumentException("A assinatura é obrigatória para gerar a resposta de checkout.");
        }

        return new CheckoutResposta(
                assinatura.getId().toString(),
                assinatura.getAsaasId(),
                assinatura.getStatus(),
                assinatura.getValor() != null ? assinatura.getValor() : new BigDecimal("49.90"),
                invoiceUrl,
                null,
                null,
                mensagem != null ? mensagem : "Assinatura processada com sucesso."
        );
    }

    public StatusPagamentoResposta paraStatusResposta(Transacao transacao) {
        if (transacao == null) {
            log.error("Mapper[Assinatura] - Tentativa de mapear transação nula para StatusPagamentoResposta.");
            throw new IllegalArgumentException("A transação é obrigatória para gerar a resposta de status.");
        }

        return new StatusPagamentoResposta(
                transacao.getAsaasId(),
                transacao.getStatus(),
                transacao.getValor(),
                transacao.getDataPagamento(),
                transacao.getDataVencimento(),
                transacao.getUrlPagamento(),
                gerarMensagemStatus(transacao.getStatus()),
                transacao.estaPaga()
        );
    }


    public Assinatura paraEntidadeInicial(
            Usuario usuario,
            CheckoutRequisicao requisicao,
            String usuarioAuditoria) {

        log.info("Mapper[Assinatura] - Preparando entidade para o usuário: {} e Plano: {}",
                usuario.getEmail(), requisicao.nomePlano());

        return Assinatura.builder()
                .usuario(usuario)
                .nomePlano(NomePlano.PRO)
                .valor(new BigDecimal("49.90"))
                .status(StatusPagamento.PENDENTE)
                .proximoVencimento(LocalDate.now().plusDays(3))
                .criadoPor(usuarioAuditoria)
                .atualizadoPor(usuarioAuditoria)
                .deletado(false)
                .build();
    }

    private String gerarMensagemStatus(StatusPagamento status) {

        return switch (status) {
            case CONFIRMADO, RECEBIDO -> "Pagamento identificado com sucesso. Acesso liberado.";
            case VENCIDO -> "O pagamento está atrasado. Regularize para evitar suspensão.";
            case ESTORNADO -> "O pagamento foi devolvido.";
            case CANCELADO -> "A assinatura foi encerrada.";
            default -> "Aguardando confirmação do gateway de pagamento.";
        };
    }
}