package br.com.taskflow.gerenciamento.pagamento.entidade;

import br.com.taskflow.gerenciamento.pagamento.enums.FormaPagamento;
import br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "transacoes_pagamento",
        indexes = {
                @Index(name = "idx_transacao_asaas_id", columnList = "asaas_id"),
                @Index(name = "idx_transacao_status", columnList = "status"),
                @Index(name = "idx_transacao_data_pagamento", columnList = "data_pagamento"),
                @Index(name = "idx_transacao_assinatura_id", columnList = "assinatura_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transacao_asaas_id", columnNames = {"asaas_id"})
        }
)
@SQLDelete(sql = "UPDATE transacoes_pagamento SET deletado = true, atualizado_em = NOW() WHERE id = ?")
@Where(clause = "deletado = false")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transacao_assinatura"))
    private Assinatura assinatura;

    @Column(name = "asaas_id", length = 100)
    private String asaasId;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "valor_liquido", precision = 10, scale = 2)
    private BigDecimal valorLiquido;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusPagamento status;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 30)
    private FormaPagamento formaPagamento;

    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDateTime dataVencimento;

    @Column(name = "url_pagamento", columnDefinition = "TEXT")
    private String urlPagamento;

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
        validarEstado();
    }

    @PreUpdate
    protected void aoAtualizar() {
        validarEstado();
    }

    private void validarEstado() {
        if (assinatura == null) {
            throw new IllegalStateException("Integridade Financeira: Uma transacao deve pertencer a uma assinatura.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Integridade Financeira: Valor invalido para transacao.");
        }
        if (status == null) {
            this.status = StatusPagamento.PENDENTE;
        }
    }

    public boolean isFinalizadaComSucesso() {
        return !deletado && (StatusPagamento.RECEBIDO.equals(status) || StatusPagamento.CONFIRMADO.equals(status));
    }

    public boolean estaPaga() {
        return br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento.RECEBIDO.equals(this.status) ||
                br.com.taskflow.gerenciamento.pagamento.enums.StatusPagamento.CONFIRMADO.equals(this.status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transacao that = (Transacao) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}