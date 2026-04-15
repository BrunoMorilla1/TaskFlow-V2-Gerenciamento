package br.com.taskflow.gerenciamento.pagamento.entidade;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "registro_eventos_webhook",
        indexes = {
                @Index(name = "idx_webhook_evento_id", columnList = "evento_id"),
                @Index(name = "idx_webhook_processado", columnList = "processado"),
                @Index(name = "idx_webhook_recebido_em", columnList = "recebido_em")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_webhook_evento_id", columnNames = "evento_id")
        }
)
public class RegistroEventoWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, length = 100)
    private String eventoId;

    @Column(name = "tipo_evento", nullable = false, length = 100)
    private String tipoEvento;

    @Builder.Default
    @Column(name = "processado", nullable = false)
    private boolean processado = false;

    @Column(name = "payload_bruto", columnDefinition = "TEXT")
    private String payloadBruto;

    @Column(name = "erro_processamento", columnDefinition = "TEXT")
    private String erroProcessamento;

    @CreationTimestamp
    @Column(name = "recebido_em", nullable = false, updatable = false)
    private LocalDateTime recebidoEm;

    @Column(name = "processado_em")
    private LocalDateTime processadoEm;

    @Version
    private Long versao;

    @PrePersist
    protected void aoPersistir() {
        if (this.eventoId != null) {
            this.eventoId = this.eventoId.trim();
        }
        if (this.tipoEvento != null) {
            this.tipoEvento = this.tipoEvento.trim().toUpperCase();
        }
    }

    public void marcarComoProcessado() {
        this.processado = true;
        this.processadoEm = LocalDateTime.now();
        this.erroProcessamento = null;
    }


    public void setDataProcessamento(LocalDateTime data) {
        this.processadoEm = data;
    }

    public void registrarErro(String mensagemErro) {
        this.processado = false;
        this.erroProcessamento = mensagemErro;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegistroEventoWebhook that)) return false;
        return eventoId != null && Objects.equals(eventoId, that.getEventoId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventoId);
    }
}