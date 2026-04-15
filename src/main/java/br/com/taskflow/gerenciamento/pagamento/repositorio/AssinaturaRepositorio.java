package br.com.taskflow.gerenciamento.pagamento.repositorio;

import br.com.taskflow.gerenciamento.pagamento.entidade.Assinatura;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssinaturaRepositorio extends JpaRepository<Assinatura, Long> {

    Optional<Assinatura> findByUsuarioId(Long usuarioId);

    Optional<Assinatura> findByAsaasId(String asaasId);

    @Query("SELECT a FROM Assinatura a WHERE a.usuario.id = :usuarioId AND a.deletado = false")
    Optional<Assinatura> buscarAssinaturaAtivaPorUsuario(@Param("usuarioId") Long usuarioId);

    @Query("SELECT a FROM Assinatura a WHERE a.usuario.id = :usuarioId AND a.deletado = false")
    Optional<Assinatura> findByUsuarioIdAndStatusAtivo(@Param("usuarioId") Long usuarioId);

    List<Assinatura> findAllByStatus(StatusPagamento status);

    @Query("SELECT COUNT(a) > 0 FROM Assinatura a WHERE a.usuario.id = :usuarioId " +
            "AND a.status IN (br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento.RECEBIDO, " +
            "br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento.CONFIRMADO) " +
            "AND a.deletado = false")
    boolean possuiAssinaturaAtiva(@Param("usuarioId") Long usuarioId);
}