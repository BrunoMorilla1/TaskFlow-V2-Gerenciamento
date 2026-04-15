package br.com.taskflow.gerenciamento.pagamento.dto.resposta;

import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StatusPagamentoResposta(
        String asaasId,
        StatusPagamento status,
        BigDecimal valor,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataPagamento,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataVencimento,

        String comprovanteUrl,
        String mensagemMotivo,
        boolean acessoLiberado
) {

    public static StatusPagamentoResposta ativa(String asaasId, BigDecimal valor, LocalDateTime vencimento) {
        return new StatusPagamentoResposta(
                asaasId,
                StatusPagamento.RECEBIDO,
                valor,
                LocalDateTime.now(),
                vencimento,
                null,
                "Pagamento confirmado e processado.",
                true
        );
    }

    public static StatusPagamentoResposta pendente(String asaasId, BigDecimal valor, String url) {
        return new StatusPagamentoResposta(
                asaasId,
                StatusPagamento.PENDENTE,
                valor,
                null,
                null,
                url,
                "Aguardando confirmação do gateway.",
                false
        );
    }
}