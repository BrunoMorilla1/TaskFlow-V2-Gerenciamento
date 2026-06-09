package br.com.taskflow.gerenciamento.pagamento.controlador;

import br.com.taskflow.gerenciamento.pagamento.cliente.asaas.ConfiguracaoAsaas;
import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.WebhookAsaasRequisicao;
import br.com.taskflow.gerenciamento.pagamento.servico.ProcessadorPagamentoServico;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks/asaas")
@RequiredArgsConstructor
@Slf4j
public class WebhookAsaasControlador {

    private final ProcessadorPagamentoServico processadorPagamentoServico;
    private final ConfiguracaoAsaas configuracao;

    @PostMapping
    public ResponseEntity<Void> receber(@RequestHeader(value = "asaas-access-token", required = false) String token,
                                                @RequestBody WebhookAsaasRequisicao req,
                                        HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        String esperado = configuracao.getWebhookToken();

        if (esperado != null && !esperado.isBlank() && !esperado.equals(token)) {
            log.warn("Webhook[ASAAS] - Token invalido. ip={}", ip);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            processadorPagamentoServico.processarWebhook(req, ip);
        } catch (Exception e) {
            log.error("Webhook[ASAAS] - Falha processando evento: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}