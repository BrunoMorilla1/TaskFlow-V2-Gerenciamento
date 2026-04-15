package br.com.taskflow.gerenciamento.pagamento.repositorio;

import br.com.taskflow.gerenciamento.pagamento.entidade.Transacao;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransacaoRepositorio extends JpaRepository<Transacao, Long> {

    Optional<Transacao> findByAsaasId(String asaasId);

    List<Transacao> findAllByAssinaturaId(Long assinaturaId);

    @Query("SELECT t FROM Transacao t WHERE t.assinatura.usuario.id = :usuarioId ORDER BY t.criadoEm DESC")
    List<Transacao> findTopByUsuario(@Param("usuarioId") Long usuarioId, Pageable pageable);

    // Alternativa simplificada usando convenção do Spring Data JPA:
    Optional<Transacao> findFirstByAssinaturaUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    @Query("SELECT t FROM Transacao t WHERE t.status = :status AND t.dataVencimento < :agora")
    List<Transacao> buscarTransacoesVencidas(@Param("status") StatusPagamento status,
                                             @Param("agora") LocalDateTime agora);

    @Query("SELECT SUM(t.valor) FROM Transacao t WHERE t.status = 'RECEBIDO' AND t.dataPagamento BETWEEN :inicio AND :fim")
    Double calcularFaturamentoNoPeriodo(@Param("inicio") LocalDateTime inicio,
                                        @Param("fim") LocalDateTime fim);

    @Query("SELECT t FROM Transacao t WHERE t.assinatura.usuario.id = :usuarioId ORDER BY t.criadoEm DESC")
    List<Transacao> buscarHistoricoPorUsuario(@Param("usuarioId") Long usuarioId);
}