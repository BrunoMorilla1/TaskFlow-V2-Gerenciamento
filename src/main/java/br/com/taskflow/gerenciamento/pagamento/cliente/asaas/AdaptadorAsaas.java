package br.com.taskflow.gerenciamento.pagamento.cliente.asaas;

import br.com.taskflow.gerenciamento.pagamento.cliente.PortaoPagamento;
import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.AsaasPagamentoDTO;
import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.AsaasPixQrCodeDTO;
import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.AsaasSubscricaoResultadoDTO;
import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.CheckoutRequisicao;
import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import br.com.taskflow.gerenciamento.pagamento.enums.FormaPagamento;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AdaptadorAsaas implements PortaoPagamento {

    private final RestTemplate asaasRestTemplate;
    private final TradutorErroAsaas tradutorErroAsaas;

    public AdaptadorAsaas(@Qualifier("asaasRestTemplate") RestTemplate asaasRestTemplate,
                          TradutorErroAsaas tradutorErroAsaas) {
        this.asaasRestTemplate = asaasRestTemplate;
        this.tradutorErroAsaas = tradutorErroAsaas;
        log.info("Asaas[Adaptador] - Inicializado. handler='{}'",
                asaasRestTemplate.getUriTemplateHandler().getClass().getSimpleName());
    }

    @Override
    public String criarOuRecuperarCliente(Usuario usuario, String cpfCnpj) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("name", usuario.getNome());
        corpo.put("email", usuario.getEmail());
        corpo.put("cpfCnpj", cpfCnpj);
        corpo.put("externalReference", usuario.getId().toString());
        corpo.put("notificationDisabled", false);

        log.info("Asaas[Cliente] - Criando/recuperando cliente. usuarioId={}", usuario.getId());

        try {
            ResponseEntity<Map> resposta = asaasRestTemplate.postForEntity("/customers", corpo, Map.class);
            String customerId = (String) resposta.getBody().get("id");
            log.info("Asaas[Cliente] - Sucesso. asaasId={}", customerId);
            return customerId;
        } catch (HttpStatusCodeException e) {
            throw tradutorErroAsaas.traduzir("Asaas[Cliente]", e);
        }
    }

    @Override
    public AsaasSubscricaoResultadoDTO gerarAssinatura(Assinatura assinatura,
                                                       CheckoutRequisicao requisicao,
                                                       String remoteIp) {
        FormaPagamento forma = requisicao.formaPagamento();

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("customer", assinatura.getUsuario().getAsaasId());
        corpo.put("billingType", forma.getCodigoAsaas());
        corpo.put("value", assinatura.getValor());
        corpo.put("nextDueDate", assinatura.getProximoVencimento().toString());
        corpo.put("cycle", "MONTHLY");
        corpo.put("description", "Plano TaskFlow Pro - Assinatura mensal");
        corpo.put("externalReference", assinatura.getId().toString());

        if (forma.isCartao() && requisicao.cartao() != null) {
            CheckoutRequisicao.DadosCartaoRequisicao c = requisicao.cartao();

            Map<String, Object> creditCard = new LinkedHashMap<>();
            creditCard.put("holderName", c.nomeNoCartao());
            creditCard.put("number", c.numeroCartao());
            creditCard.put("expiryMonth", c.mesValidade());
            creditCard.put("expiryYear", c.anoValidade());
            creditCard.put("ccv", c.cvv());
            corpo.put("creditCard", creditCard);

            Map<String, Object> holder = new LinkedHashMap<>();
            holder.put("name", c.nomeNoCartao());
            holder.put("email", assinatura.getUsuario().getEmail());
            holder.put("cpfCnpj", requisicao.documentoCliente());
            holder.put("postalCode", c.cep());
            holder.put("addressNumber", c.numeroEndereco());
            holder.put("phone", c.telefone());
            corpo.put("creditCardHolderInfo", holder);
            corpo.put("remoteIp", remoteIp);
        }

        log.info("Asaas[Assinatura] - Gerando assinatura. assinaturaId={} forma={}",
                assinatura.getId(), forma);

        try {
            AsaasSubscricaoResultadoDTO resultado = asaasRestTemplate
                    .postForEntity("/subscriptions", corpo, AsaasSubscricaoResultadoDTO.class)
                    .getBody();
            log.info("Asaas[Assinatura] - Sucesso. asaasSubscriptionId={}", resultado.id());
            return resultado;
        } catch (HttpStatusCodeException e) {
            throw tradutorErroAsaas.traduzir("Asaas[Assinatura]", e);
        }
    }

    @Override
    public AsaasPagamentoDTO buscarPrimeiroPagamentoAssinatura(String asaasSubscriptionId) {
        log.info("Asaas[Pagamento] - Buscando primeiro pagamento. asaasSubscriptionId={}", asaasSubscriptionId);
        try {
            ListaPagamentosResposta resposta = asaasRestTemplate
                    .getForEntity("/subscriptions/{id}/payments", ListaPagamentosResposta.class, asaasSubscriptionId)
                    .getBody();

            if (resposta == null || resposta.data() == null || resposta.data().isEmpty()) {
                throw new IllegalStateException(
                        "Nenhum pagamento encontrado para a assinatura: " + asaasSubscriptionId);
            }

            return resposta.data().get(0);
        } catch (HttpStatusCodeException e) {
            throw tradutorErroAsaas.traduzir("Asaas[Pagamento]", e);
        }
    }

    @Override
    public AsaasPixQrCodeDTO buscarPixQrCode(String asaasPaymentId) {
        log.info("Asaas[PIX] - Buscando QR Code. asaasPaymentId={}", asaasPaymentId);
        try {
            return asaasRestTemplate
                    .getForEntity("/payments/{id}/pixQrCode", AsaasPixQrCodeDTO.class, asaasPaymentId)
                    .getBody();
        } catch (HttpStatusCodeException e) {
            throw tradutorErroAsaas.traduzir("Asaas[PIX]", e);
        }
    }

    @Override
    public StatusPagamento consultarStatusPagamento(String asaasPaymentId) {
        log.info("Asaas[Status] - Consultando status. asaasPaymentId={}", asaasPaymentId);
        try {
            ResponseEntity<Map> resp = asaasRestTemplate
                    .getForEntity("/payments/{id}/status", Map.class, asaasPaymentId);

            if (resp.getBody() == null) {
                log.warn("Asaas[Status] - Resposta vazia. Retornando PENDENTE. asaasPaymentId={}", asaasPaymentId);
                return StatusPagamento.PENDENTE;
            }

            StatusPagamento status = StatusPagamento.deCodigoAsaas((String) resp.getBody().get("status"));
            log.info("Asaas[Status] - Status obtido: {}. asaasPaymentId={}", status, asaasPaymentId);
            return status;

        } catch (HttpStatusCodeException e) {
            log.warn("Asaas[Status] - Falha HTTP ao consultar status. Retornando PENDENTE. asaasPaymentId={} status={}",
                    asaasPaymentId, e.getStatusCode().value());
            return StatusPagamento.PENDENTE;
        }
    }

    @Override
    public void cancelarAssinatura(String asaasId) {
        log.info("Asaas[Cancelamento] - Cancelando assinatura. asaasId={}", asaasId);
        try {
            asaasRestTemplate.delete("/subscriptions/{id}", asaasId);
            log.info("Asaas[Cancelamento] - Assinatura cancelada com sucesso. asaasId={}", asaasId);
        } catch (HttpStatusCodeException e) {
            throw tradutorErroAsaas.traduzir("Asaas[Cancelamento]", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ListaPagamentosResposta(List<AsaasPagamentoDTO> data) {}
}