package br.com.taskflow.gerenciamento.seguranca.auditoria;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "log_seguranca",
        indexes = {
                @Index(name = "idx_log_seg_tipo", columnList = "tipo_evento"),
                @Index(name = "idx_log_seg_usuario", columnList = "email_usuario"),
                @Index(name = "idx_log_seg_ip", columnList = "ip"),
                @Index(name = "idx_log_seg_data", columnList = "data_evento")
        }
)
@Getter
@Setter
public class EntidadeLogSeguranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_evento", nullable = false, length = 50)
    private String tipoEvento;

    @Column(name = "email_usuario", length = 150)
    private String emailUsuario;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "detalhes", columnDefinition = "TEXT")
    private String detalhes;

    @CreationTimestamp
    @Column(name = "data_evento", nullable = false, updatable = false)
    private Instant dataEvento;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(name = "sucesso", nullable = false)
    private Boolean sucesso = true;

    @Column(name = "codigo_erro", length = 50)
    private String codigoErro;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntidadeLogSeguranca)) return false;
        EntidadeLogSeguranca that = (EntidadeLogSeguranca) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}