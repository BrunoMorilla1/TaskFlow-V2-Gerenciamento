package br.com.taskflow.gerenciamento.pagamento.controlador;

import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.WebhookAsaasRequisicao;
import br.com.taskflow.gerenciamento.pagamento.servico.ProcessadorPagamentoServico;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/asaas")
@RequiredArgsConstructor
@Slf4j
public class WebhookAsaasControlador {

    private final ProcessadorPagamentoServico processadorPagamentoServico;


    @PostMapping
    public ResponseEntity<Void> receberEvento(@RequestBody WebhookAsaasRequisicao requisicao,
                                              HttpServletRequest request) {

        String ip = request.getRemoteAddr();
        String eventoId = requisicao.evento();

        log.info("Controlador[Webhook] - Notificacao recebida do Asaas. evento={}, ip={}",
                eventoId, ip);

        try {
            // Delegamos o processamento pesado para o serviço (que lida com idempotência)
            processadorPagamentoServico.processarWebhook(requisicao, ip);

            log.info("Controlador[Webhook] - Evento processado com sucesso. evento={}, ip={}",
                    eventoId, ip);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Controlador[Webhook] - Falha ao processar notificacao. evento={}, ip={}, erro={}",
                    eventoId, ip, e.getMessage());
            return ResponseEntity.ok().build();
        }
    }
}