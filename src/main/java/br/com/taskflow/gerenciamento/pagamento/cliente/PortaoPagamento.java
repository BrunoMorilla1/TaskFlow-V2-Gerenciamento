package br.com.taskflow.gerenciamento.pagamento.cliente;

import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;

public interface PortaoPagamento {

    String criarOuRecuperarCliente(Usuario usuario);

    String gerarAssinatura(Assinatura assinatura);

    StatusPagamento consultarStatusPagamento(String asaasId);

    void cancelarAssinatura(String asaasId);

}