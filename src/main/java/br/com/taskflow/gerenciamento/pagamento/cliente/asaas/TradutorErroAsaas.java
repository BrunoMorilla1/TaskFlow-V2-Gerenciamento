package br.com.taskflow.gerenciamento.pagamento.cliente.asaas;

import br.com.taskflow.gerenciamento.excecao.FalhaComunicacaoGatewayExcecao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TradutorErroAsaas {

    private final ObjectMapper mapper = new ObjectMapper();

    public FalhaComunicacaoGatewayExcecao traduzir(String contexto, HttpStatusCodeException ex) {
        String body = ex.getResponseBodyAsString();
        int status = ex.getStatusCode().value();
        List<String> mensagens = extrairMensagens(body);

        String mensagemFinal = mensagens.isEmpty()
                ? "Falha ao se comunicar com o gateway de pagamento (HTTP " + status + ")."
                : String.join(" | ", mensagens);

                log.error("{} - HTTP {} - {}", contexto, status, mensagemFinal);
        return new FalhaComunicacaoGatewayExcecao(mensagemFinal);
    }

    private List<String> extrairMensagens(String body) {
        List<String> out = new ArrayList<>();
        if (body == null || body.isBlank()) return out;
        try {
            JsonNode root = mapper.readTree(body);
            JsonNode errs = root.get("errors");
            if (errs != null && errs.isArray()) {
                for (JsonNode e : errs) {
                    String desc = e.path("description").asText(null);
                    if (desc != null && !desc.isBlank()) out.add(desc);
                }
            }
        } catch (Exception ignored) {
            out.add(body.length() > 200 ? body.substring(0, 200) : body);
        }
        return out;
    }
}
