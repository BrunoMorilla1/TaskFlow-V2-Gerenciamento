package br.com.taskflow.gerenciamento.pagamento.dto.requisicao;

import br.com.taskflow.gerenciamento.pagamento.enums.FormaPagamento;
import br.com.taskflow.gerenciamento.pagamento.enums.NomePlano;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CheckoutRequisicao(

        NomePlano nomePlano,

        @NotNull(message = "A forma de pagamento é obrigatória")
                FormaPagamento formaPagamento,

        @NotBlank(message = "O CPF ou CNPJ do pagador é obrigatório")
                @Pattern(regexp = "(^\\d{11}$|^\\d{14}$)", message = "CPF ou CNPJ deve conter 11 ou 14 dígitos")
                String documentoCliente,

        @Valid
        DadosCartaoRequisicao cartao
) {

        public CheckoutRequisicao {
                if (documentoCliente != null) {
                        documentoCliente = documentoCliente.replaceAll("\\D", "");
                }
        }

    public record DadosCartaoRequisicao(
            @NotBlank(message = "Nome no cartão obrigatório")
            String nomeNoCartao,

            @NotBlank(message = "Número do cartão obrigatório")
                    @Pattern(regexp = "\\d{13,19}", message = "Número de cartão inválido")
                    String numeroCartao,

            @NotBlank(message = "Mês de validade obrigatório")
                    @Pattern(regexp = "\\d{2}", message = "Mês deve ter 2 dígitos")
                    String mesValidade,

            @NotBlank(message = "Ano de validade obrigatório")
                    @Pattern(regexp = "\\d{4}", message = "Ano deve ter 4 dígitos")
                    String anoValidade,

            @NotBlank(message = "CVV obrigatório")
                    @Pattern(regexp = "\\d{3,4}", message = "CVV inválido")
                    String cvv,

            @NotBlank(message = "CEP obrigatório")
                    @Pattern(regexp = "\\d{8}", message = "CEP deve ter 8 dígitos")
                    String cep,

            @NotBlank(message = "Número do endereço obrigatório")
                    String numeroEndereco,

            @NotBlank(message = "Telefone obrigatório")
                    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve ter 10 ou 11 dígitos")
                    String telefone
    ) {}
}

//Observation: Overwrite successful: /app/taskflow/src/main/java/br/com/taskflow/gerenciamento/pagamento/dto/requisicao/checkoutRequisicao.java