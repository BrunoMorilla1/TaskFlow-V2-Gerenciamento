package br.com.taskflow.gerenciamento.seguranca.jwt;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "seg_refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_token_jti", columnList = "token_id", unique = true),
                @Index(name = "idx_refresh_token_usuario", columnList = "email_usuario"),
                @Index(name = "idx_refresh_token_expiracao", columnList = "expiracao"),
                @Index(name = "idx_refresh_token_revogado", columnList = "revogado")
        }
)
@Getter
@Setter
public class EntidadeRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_id", nullable = false, unique = true, length = 120)
    private String tokenId;

    @Column(name = "email_usuario", nullable = false, length = 150)
    private String emailUsuario;

    @Column(name = "expiracao", nullable = false)
    private Instant expiracao;

    @Column(name = "revogado", nullable = false)
    private boolean revogado = false;

    @Column(name = "ip_origem", length = 45)
    private String ipOrigem;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Instant dataCriacao;

    public boolean estaExpirado() {
        return expiracao != null && Instant.now().isAfter(expiracao);
    }

    public boolean estaAtivo() {
        return !revogado && !estaExpirado();
    }

    public void revogar() {
        this.revogado = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntidadeRefreshToken)) return false;
        EntidadeRefreshToken that = (EntidadeRefreshToken) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}