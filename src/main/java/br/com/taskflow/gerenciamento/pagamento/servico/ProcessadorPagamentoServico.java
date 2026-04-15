package br.com.taskflow.gerenciamento.pagamento.servico;

import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.WebhookAsaasRequisicao;
import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import br.com.taskflow.gerenciamento.pagamento.entidade.RegistroEventoWebhook;
import br.com.taskflow.gerenciamento.pagamento.entidade.Transacao;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import br.com.taskflow.gerenciamento.pagamento.eventos.PagamentoConfirmadoEvento;
import br.com.taskflow.gerenciamento.pagamento.eventos.PagamentoFalhoEvento;
import br.com.taskflow.gerenciamento.pagamento.repositorio.AssinaturaRepositorio;
import br.com.taskflow.gerenciamento.pagamento.repositorio.RegistroEventoWebhookRepositorio;
import br.com.taskflow.gerenciamento.pagamento.repositorio.TransacaoRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessadorPagamentoServico {

    private final RegistroEventoWebhookRepositorio eventoRepositorio;
    private final TransacaoRepositorio transacaoRepositorio;
    private final AssinaturaRepositorio assinaturaRepositorio;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processarWebhook(WebhookAsaasRequisicao requisicao, String ip) {
        String webhookId = requisicao.id();
        String tipoEvento = requisicao.evento(); // CORREÇÃO: Nome em português conforme DTO

        log.info("WEBHOOK[RECEBIDO] - Evento: {}, ID: {}, IP Origem: {}", tipoEvento, webhookId, ip);

        if (eventoRepositorio.existsByEventoId(webhookId)) {
            log.warn("WEBHOOK[IGNORADO] - Duplicidade detectada. ID: {}", webhookId);
            return;
        }

        RegistroEventoWebhook registro = registrarEntrada(requisicao);

        try {
            var dados = requisicao.pagamento(); // CORREÇÃO: .pagamento() conforme DTO

            switch (tipoEvento) {
                case "PAYMENT_RECEIVED", "PAYMENT_CONFIRMED" -> executarFluxoAprovacao(dados, ip);
                case "PAYMENT_OVERDUE" -> executarFluxoInadimplencia(dados, ip);
                case "PAYMENT_REFUNDED", "PAYMENT_CHARGEBACK_REQUESTED" -> executarFluxoEstorno(dados, ip);
                case "PAYMENT_DELETED" -> log.warn("WEBHOOK[ALERTA] - Pagamento deletado no Asaas. ID: {}", dados.id());
                default -> log.info("WEBHOOK[INFO] - Evento sem trigger de negócio: {}", tipoEvento);
            }

            finalizarRegistro(registro);
            log.info("WEBHOOK[SUCESSO] - Processamento concluído. ID: {}", webhookId);

        } catch (Exception e) {
            log.error("WEBHOOK[ERRO CRITICO] - Falha ao processar ID: {}. Motivo: {}", webhookId, e.getMessage());
            registro.registrarErro(e.getMessage());
            eventoRepositorio.save(registro);
            throw e; // Lança para o controlador, mas o controlador retornará 200 OK
        }
    }

    private void executarFluxoAprovacao(WebhookAsaasRequisicao.DadosPagamentoWebhook dados, String ip) {
        Transacao transacao = transacaoRepositorio.findByAsaasId(dados.id())
                .orElseGet(() -> criarTransacaoBase(dados));

        transacao.setStatus(StatusPagamento.CONFIRMADO);
        transacao.setDataPagamento(LocalDateTime.now());
        transacao.setValorLiquido(BigDecimal.valueOf(dados.netValue()));
        transacaoRepositorio.save(transacao);

        Assinatura assinatura = transacao.getAssinatura();
        assinatura.setStatus(StatusPagamento.CONFIRMADO);
        assinatura.setProximoVencimento(LocalDateTime.now().plusDays(30).toLocalDate());
        assinaturaRepositorio.save(assinatura);

        eventPublisher.publishEvent(new PagamentoConfirmadoEvento(assinatura, ip));
    }

    private void executarFluxoInadimplencia(WebhookAsaasRequisicao.DadosPagamentoWebhook dados, String ip) {
        transacaoRepositorio.findByAsaasId(dados.id()).ifPresent(t -> {
            t.setStatus(StatusPagamento.VENCIDO);
            transacaoRepositorio.save(t);

            Assinatura a = t.getAssinatura();
            a.setStatus(StatusPagamento.VENCIDO);
            assinaturaRepositorio.save(a);

            eventPublisher.publishEvent(new PagamentoFalhoEvento(
                    a.getUsuario().getId(),
                    a.getUsuario().getEmail(),
                    ip,
                    "PAGAMENTO_VENCIDO",
                    dados.id()));
        });
    }

    private void executarFluxoEstorno(WebhookAsaasRequisicao.DadosPagamentoWebhook dados, String ip) {
        transacaoRepositorio.findByAsaasId(dados.id()).ifPresent(t -> {
            t.setStatus(StatusPagamento.ESTORNADO);
            transacaoRepositorio.save(t);

            Assinatura a = t.getAssinatura();
            a.setStatus(StatusPagamento.ESTORNADO);
            assinaturaRepositorio.save(a);
        });
    }

    private Transacao criarTransacaoBase(WebhookAsaasRequisicao.DadosPagamentoWebhook dados) {
        Assinatura assinatura = assinaturaRepositorio.findByAsaasId(dados.subscription())
                .orElseThrow(() -> new RuntimeException("Assinatura não vinculada ao pagamento: " + dados.id()));

        return Transacao.builder()
                .asaasId(dados.id())
                .assinatura(assinatura)
                .valor(BigDecimal.valueOf(dados.value()))
                .status(StatusPagamento.PENDENTE)
                .build();
    }

    private RegistroEventoWebhook registrarEntrada(WebhookAsaasRequisicao req) {
        return eventoRepositorio.save(RegistroEventoWebhook.builder()
                .eventoId(req.id())
                .tipoEvento(req.evento()) // CORREÇÃO: .evento()
                .processado(false)
                .build());
    }

    private void finalizarRegistro(RegistroEventoWebhook registro) {
        registro.marcarComoProcessado();
        eventoRepositorio.save(registro);
    }
}