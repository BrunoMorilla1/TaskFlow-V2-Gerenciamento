package br.com.taskflow.gerenciamento.usuarios.entidade;

import br.com.taskflow.gerenciamento.usuarios.enums.RoleUsuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "usuarios",
        indexes = {
                @Index(name = "idx_usuario_email", columnList = "email"),
                @Index(name = "idx_usuario_ativo", columnList = "ativo"),
                @Index(name = "idx_usuario_deletado", columnList = "deletado"),
                @Index(name = "idx_usuario_expiracao", columnList = "data_expiracao_acesso")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_usuario_email", columnNames = "email")
        }
)
@SQLDelete(sql = "UPDATE usuarios SET deletado = true WHERE id = ?")
@SQLRestriction("deletado = false")
public class Usuario implements UserDetails {

    private static final int MAX_NOME = 150;
    private static final int MAX_EMAIL = 150;
    private static final int MAX_ROLE = 30;
    private static final int MAX_IP = 45;
    private static final int MAX_AUDITORIA = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = MAX_NOME)
    private String nome;

    @Column(name = "email", nullable = false, unique = true, length = MAX_EMAIL)
    private String email;

    @JsonIgnore
    @Column(name = "senha", nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = MAX_ROLE)
    private RoleUsuario role;

    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Builder.Default
    @Column(name = "conta_nao_expirada", nullable = false)
    private boolean contaNaoExpirada = true;

    @Builder.Default
    @Column(name = "conta_nao_bloqueada", nullable = false)
    private boolean contaNaoBloqueada = true;

    @Builder.Default
    @Column(name = "credencial_nao_expirada", nullable = false)
    private boolean credencialNaoExpirada = true;

    @Builder.Default
    @Column(name = "habilitado", nullable = false)
    private boolean habilitado = true;

    @Builder.Default
    @Column(name = "tentativas_login", nullable = false)
    private int tentativasLogin = 0;

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    @Column(name = "ultimo_ip", length = MAX_IP)
    private String ultimoIp;

    @Column(name = "data_bloqueio")
    private LocalDateTime dataBloqueio;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "criado_por", length = MAX_AUDITORIA)
    private String criadoPor;

    @Column(name = "atualizado_por", length = MAX_AUDITORIA)
    private String atualizadoPor;

    @Builder.Default
    @Column(name = "tipo_plano", length = 50)
    private String tipoPlano = "TASKFLOW_PRO_4990";

    @Column(name = "asaas_id", length = 50)
    private String asaasId;

    @Column(name = "data_expiracao_acesso")
    private LocalDateTime dataExpiracaoAcesso;

    @Version
    private Long versao;

    @Builder.Default
    @Column(name = "deletado", nullable = false)
    private boolean deletado = false;

    @PrePersist
    @PreUpdate
    private void prePersistAndUpdate() {
        normalizarCampos();
        aplicarDefaults();
        validarCamposObrigatorios();
        processarTrial();
    }

    private void normalizarCampos() {
        if (nome != null) {
            nome = nome.trim();
        }

        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }

        if (criadoPor != null) {
            criadoPor = criadoPor.trim();
        }

        if (atualizadoPor != null) {
            atualizadoPor = atualizadoPor.trim();
        }

        if (ultimoIp != null) {
            ultimoIp = ultimoIp.trim();
        }
    }

    private void aplicarDefaults() {
        if (tentativasLogin < 0) {
            tentativasLogin = 0;
        }
        if (tipoPlano == null) {
            tipoPlano = "TASKFLOW_PRO_4990";
        }
    }

    private void processarTrial() {
        // Se for novo usuário e for USER, ganha 7 dias. Se for ADMIN, fica null (infinito).
        if (this.id == null && this.role == RoleUsuario.USER && this.dataExpiracaoAcesso == null) {
            this.dataExpiracaoAcesso = LocalDateTime.now().plusDays(7);
        }
    }

    private void validarCamposObrigatorios() {
        if (nome == null || nome.isBlank()) {
            throw new IllegalStateException("O nome do usuário é obrigatório.");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("O email do usuário é obrigatório.");
        }

        if (senha == null || senha.isBlank()) {
            throw new IllegalStateException("A senha do usuário é obrigatória.");
        }

        if (role == null) {
            throw new IllegalStateException("O perfil do usuário é obrigatório.");
        }
    }

    public void registrarFalhaLogin(int maxTentativas) {
        if (maxTentativas <= 0) {
            throw new IllegalArgumentException("O número máximo de tentativas deve ser maior que zero.");
        }

        this.tentativasLogin++;

        if (this.tentativasLogin >= maxTentativas) {
            bloquearConta();
        }
    }

    public void registrarSucessoLogin(String ip) {
        this.tentativasLogin = 0;
        this.contaNaoBloqueada = true;
        this.dataBloqueio = null;
        this.ultimoLogin = LocalDateTime.now();
        this.ultimoIp = ip != null ? ip.trim() : null;
    }

    public void bloquearConta() {
        this.contaNaoBloqueada = false;
        this.dataBloqueio = LocalDateTime.now();
    }

    public void desbloquearConta() {
        this.contaNaoBloqueada = true;
        this.dataBloqueio = null;
        this.tentativasLogin = 0;
    }

    public boolean estaBloqueado() {
        return !this.contaNaoBloqueada;
    }

    public boolean possuiAcessoAtivo() {
        if (this.role == RoleUsuario.ADMIN) {
            return true;
        }

        return this.ativo
                && !this.deletado
                && (dataExpiracaoAcesso == null
                || dataExpiracaoAcesso.isAfter(LocalDateTime.now()));
    }

    public boolean estaAtivoParaLogin() {
        return isEnabled()
                && contaNaoBloqueada
                && contaNaoExpirada
                && credencialNaoExpirada;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return contaNaoExpirada;
    }

    @Override
    public boolean isAccountNonLocked() {
        return contaNaoBloqueada;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credencialNaoExpirada;
    }

    @Override
    public boolean isEnabled() {
        return habilitado && ativo && !deletado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Usuario other)) {
            return false;
        }
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}