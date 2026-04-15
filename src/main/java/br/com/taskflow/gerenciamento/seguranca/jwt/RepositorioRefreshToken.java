package br.com.taskflow.gerenciamento.seguranca.jwt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RepositorioRefreshToken extends JpaRepository<EntidadeRefreshToken, Long> {

    Optional<EntidadeRefreshToken> findByTokenId(String tokenId);

    Optional<EntidadeRefreshToken> findByTokenIdAndRevogadoFalse(String tokenId);

    boolean existsByTokenIdAndRevogadoFalse(String tokenId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE EntidadeRefreshToken t
               SET t.revogado = true
             WHERE t.emailUsuario = :email
               AND t.revogado = false
            """)
    int revogarTokensAtivosPorUsuario(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            DELETE
              FROM EntidadeRefreshToken t
             WHERE t.expiracao < :agora
            """)
    int deletarTokensExpirados(Instant agora);
}