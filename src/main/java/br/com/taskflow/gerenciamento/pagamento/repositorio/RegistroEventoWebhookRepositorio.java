package br.com.taskflow.gerenciamento.pagamento.repositorio;

import br.com.taskflow.gerenciamento.pagamento.entidade.RegistroEventoWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegistroEventoWebhookRepositorio extends JpaRepository<RegistroEventoWebhook, Long> {

    boolean existsByEventoId(String eventoId);

    Optional<RegistroEventoWebhook> findByEventoId(String eventoId);

    @Query("SELECT r.processado FROM RegistroEventoWebhook r WHERE r.eventoId = :eventoId")
    boolean isEventoProcessado(@Param("eventoId") String eventoId);
}