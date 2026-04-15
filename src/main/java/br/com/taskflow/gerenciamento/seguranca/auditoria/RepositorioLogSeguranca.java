package br.com.taskflow.gerenciamento.seguranca.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface RepositorioLogSeguranca
        extends JpaRepository<EntidadeLogSeguranca, Long> {

    Page<EntidadeLogSeguranca> findByEmailUsuarioOrderByDataEventoDesc(
            String emailUsuario,
            Pageable pageable
    );

    Page<EntidadeLogSeguranca> findByIpOrderByDataEventoDesc(
            String ip,
            Pageable pageable
    );

    Page<EntidadeLogSeguranca> findByTipoEventoOrderByDataEventoDesc(
            String tipoEvento,
            Pageable pageable
    );

    List<EntidadeLogSeguranca> findByDataEventoBetween(
            Instant inicio,
            Instant fim
    );

    List<EntidadeLogSeguranca> findByEmailUsuarioAndDataEventoBetween(
            String emailUsuario,
            Instant inicio,
            Instant fim
    );

    List<EntidadeLogSeguranca> findByIpAndDataEventoBetween(
            String ip,
            Instant inicio,
            Instant fim
    );

}