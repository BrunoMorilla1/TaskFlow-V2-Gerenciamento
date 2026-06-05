package br.com.taskflow.gerenciamento.pagamento.entidade;

import br.com.taskflow.gerenciamento.pagamento.enums.NomePlano;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "assinaturas",
        indexes = {
                @Index(name = "idx_assinatura_status", columnList = "status"),
                @Index(name = "idx_assinatura_asaas_id", columnList = "asaas_id"),
                @Index(name = "idx_assinatura_usuario_id", columnList = "usuario_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_assinatura_asaas_id", columnNames = {"asaas_id"}),
                @UniqueConstraint(name = "uk_assinatura_usuario_ativo", columnNames = {"usuario_id", "deletado"})
        }
)
@SQLDelete(sql = "UPDATE assinaturas SET deletado = true, atualizado_em = NOW() WHERE id = ?")
@Where(clause = "deletado = false")
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_assinatura_usuario"))
    private Usuario usuario;

    @Column(name = "plano_id", nullable = false)
    NomePlano nomePlano;

    @Column(name = "asaas_id", length = 100)
    private String asaasId;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusPagamento status;

    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    @Column(name = "proximo_vencimento")
    private LocalDate proximoVencimento;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "criado_por", length = 100)
    private String criadoPor;

    @Column(name = "atualizado_por", length = 100)
    private String atualizadoPor;

    @Builder.Default
    @Column(name = "deletado", nullable = false)
    private boolean deletado = false;

    @Version
    private Long versao;

    @PrePersist
    protected void aoPersistir() {
        if (this.valor == null) {
            this.valor = new BigDecimal("49.90");
        }
        if (this.status == null) {
            this.status = StatusPagamento.PENDENTE;
        }
        validarEstado();
    }

    @PreUpdate
    protected void aoAtualizar() {
        validarEstado();
    }

    private void validarEstado() {
        if (usuario == null) {
            throw new IllegalStateException("Falha de Integridade: Toda assinatura deve possuir um usuario vinculado.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Falha de Negocio: O valor da assinatura deve ser positivo.");
        }
    }

    public boolean isAtiva() {
        return !deletado && (StatusPagamento.CONFIRMADO.equals(status) || StatusPagamento.RECEBIDO.equals(status));
    }

    public void atualizarStatus(StatusPagamento novoStatus, String auditoria) {
        this.status = novoStatus;
        this.atualizadoPor = auditoria;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assinatura that = (Assinatura) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}