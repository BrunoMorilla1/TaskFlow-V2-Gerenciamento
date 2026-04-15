package br.com.taskflow.gerenciamento.pagamento.cliente.asaas;

import br.com.taskflow.gerenciamento.pagamento.cliente.PortaoPagamento;
import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdaptadorAsaas implements PortaoPagamento {

    private final RestTemplate asaasRestTemplate;

    @Override
    public String criarOuRecuperarCliente(Usuario usuario) {
        log.info("Asaas[Cliente] - Iniciando criacao/recuperacao. usuarioId={}", usuario.getId());

        try {
            Map<String, Object> corpo = new HashMap<>();
            corpo.put("name", usuario.getNome());
            corpo.put("email", usuario.getEmail());
            corpo.put("externalReference", usuario.getId().toString());
            corpo.put("notificationDisabled", false);

            ResponseEntity<Map> resposta = asaasRestTemplate.postForEntity("/customers", new HttpEntity<>(corpo), Map.class);

            if (resposta.getStatusCode().is2xxSuccessful() && resposta.getBody() != null) {
                return (String) resposta.getBody().get("id");
            }
            throw new RuntimeException("Resposta invalida do Asaas");
        } catch (Exception e) {
            log.error("Asaas[Cliente] - Erro: {}", e.getMessage());
            throw new RuntimeException("Falha na integracao: " + e.getMessage());
        }
    }

    @Override
    public String gerarAssinatura(Assinatura assinatura) {
        log.info("Asaas[Assinatura] - Gerando recorrencia para assinaturaId={}", assinatura.getId());

        try {
            Map<String, Object> corpo = new HashMap<>();
            corpo.put("customer", assinatura.getUsuario().getAsaasId());
            corpo.put("billingType", "PIX");
            corpo.put("value", 49.90);
            corpo.put("nextDueDate", assinatura.getProximoVencimento().toString());
            corpo.put("cycle", "MONTHLY");
            corpo.put("description", "Plano TaskFlow Pro");
            corpo.put("externalReference", assinatura.getId().toString());

            ResponseEntity<Map> resposta = asaasRestTemplate.postForEntity("/subscriptions", new HttpEntity<>(corpo), Map.class);

            if (resposta.getStatusCode().is2xxSuccessful() && resposta.getBody() != null) {
                return (String) resposta.getBody().get("id");
            }
            throw new RuntimeException("Erro ao criar assinatura");
        } catch (Exception e) {
            log.error("Asaas[Assinatura] - Erro: {}", e.getMessage());
            throw new RuntimeException("Erro no gateway: " + e.getMessage());
        }
    }

    @Override
    public StatusPagamento consultarStatusPagamento(String asaasId) {
        try {
            ResponseEntity<Map> resposta = asaasRestTemplate.getForEntity("/payments/" + asaasId, Map.class);
            if (resposta.getBody() != null) {
                return deParaStatus((String) resposta.getBody().get("status"));
            }
        } catch (Exception e) {
            log.error("Asaas[Consulta] - Erro: {}", e.getMessage());
        }
        return StatusPagamento.PENDENTE;
    }

    @Override
    public void cancelarAssinatura(String asaasId) {
        try {
            asaasRestTemplate.delete("/subscriptions/" + asaasId);
            log.info("Asaas[Cancelamento] - Sucesso: {}", asaasId);
        } catch (Exception e) {
            log.error("Asaas[Cancelamento] - Erro: {}", e.getMessage());
            throw new RuntimeException("Nao foi possivel cancelar no Asaas");
        }
    }

    private StatusPagamento deParaStatus(String statusAsaas) {
        if (statusAsaas == null) return StatusPagamento.PENDENTE;
        return switch (statusAsaas.toUpperCase()) {
            case "CONFIRMED", "RECEIVED", "RECEIVED_IN_CASH" -> StatusPagamento.CONFIRMADO;
            case "OVERDUE" -> StatusPagamento.VENCIDO;
            case "REFUNDED", "CHARGEBACK_REQUESTED" -> StatusPagamento.ESTORNADO;
            case "DELETED", "CANCELLED" -> StatusPagamento.CANCELADO;
            default -> StatusPagamento.PENDENTE;
        };
    }
}