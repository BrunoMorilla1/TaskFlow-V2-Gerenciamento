package br.com.taskflow.gerenciamento.pagamento.cliente;

import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.AsaasPagamentoDTO;
import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.AsaasPixQrCodeDTO;
import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.AsaasSubscricaoResultadoDTO;
import br.com.taskflow.gerenciamento.pagamento.dto.requisicao.CheckoutRequisicao;
import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;

public interface PortaoPagamento {

    String criarOuRecuperarCliente(Usuario usuario, String cpfCnpj);

    AsaasSubscricaoResultadoDTO gerarAssinatura(Assinatura assinatura, CheckoutRequisicao requisicao, String remoteIp);

    AsaasPagamentoDTO buscarPrimeiroPagamentoAssinatura(String asaasSubscriptionId);

    AsaasPixQrCodeDTO buscarPixQrCode(String asaasPaymentId);

    StatusPagamento consultarStatusPagamento(String asaasId);

    void cancelarAssinatura(String asaasId);
}
