package br.com.taskflow.gerenciamento.pagamento.dto.requisicao;

import br.com.taskflow.gerenciamento.pagamento.enums.FormaPagamento;
import br.com.taskflow.gerenciamento.pagamento.enums.NomePlano;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CheckoutRequisicao(

        @NotNull(message = "O ID do plano é obrigatório")
        NomePlano nomePlano,

        @NotNull(message = "A forma de pagamento é obrigatória")
        FormaPagamento formaPagamento,

        @NotBlank(message = "O CPF ou CNPJ do pagador é obrigatório")
        @Pattern(regexp = "(^\\d{11}$|^\\d{14}$)", message = "CPF ou CNPJ deve conter apenas números (11 ou 14 dígitos)")
        String documentoCliente,

        @Valid
        DadosCartaoRequisicao cartao
) {

    public record DadosCartaoRequisicao(
            @NotBlank(message = "O token do cartão é obrigatório para pagamentos via crédito")
            String tokenCartao,

            @NotBlank(message = "O nome impresso no cartão é obrigatório")
            String nomeNoCartao
    ) {}
}