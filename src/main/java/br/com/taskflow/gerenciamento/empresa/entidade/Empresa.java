package br.com.taskflow.gerenciamento.empresa.entidade;

import br.com.taskflow.gerenciamento.empresa.enums.Segmentos;
import br.com.taskflow.gerenciamento.empresa.enums.StatusEmpresa;
import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "empresas",
        indexes = {
                @Index(name = "idx_empresa_usuario_dono", columnList = "usuario_dono_id"),
                @Index(name = "idx_empresa_nome_fantasia", columnList = "nome_fantasia"),
                @Index(name = "idx_empresa_razao_social", columnList = "razao_social"),
                @Index(name = "idx_empresa_cnpj", columnList = "cnpj"),
                @Index(name = "idx_empresa_ativo", columnList = "ativo"),
                @Index(name = "idx_empresa_deletado", columnList = "deletado")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_empresa_cnpj", columnNames = "cnpj")
        }
)
@SQLDelete(sql = "UPDATE empresas SET deletado = true WHERE id = ?")
@SQLRestriction("deletado = false")
public class Empresa {

    private static final int MAX_RAZAO_SOCIAL = 200;
    private static final int MAX_NOME_FANTASIA = 200;
    private static final int MAX_CNPJ = 14;
    private static final int MAX_EMAIL = 150;
    private static final int MAX_TELEFONE = 20;
    private static final int MAX_INSCRICAO_ESTADUAL = 30;
    private static final int MAX_INSCRICAO_MUNICIPAL = 30;
    private static final int MAX_SITE = 200;
    private static final int MAX_AUDITORIA = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razao_social", nullable = false, length = MAX_RAZAO_SOCIAL)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = MAX_NOME_FANTASIA)
    private String nomeFantasia;

    @Column(name = "cnpj", nullable = false, unique = true, length = MAX_CNPJ)
    private String cnpj;

    @Column(name = "email", length = MAX_EMAIL)
    private String email;

    @Column(name = "telefone", length = MAX_TELEFONE)
    private String telefone;

    @Column(name = "inscricao_estadual", length = MAX_INSCRICAO_ESTADUAL)
    private String inscricaoEstadual;

    @Column(name = "inscricao_municipal", length = MAX_INSCRICAO_MUNICIPAL)
    private String inscricaoMunicipal;

    @Column(name = "site", length = MAX_SITE)
    private String site;

    @Column(name = "logradouro", length = 150)
    private String logradouro;

    @Column(name = "numero", length = 20)
    private String numero;

    @Column(name = "bairro", length = 100)
    private String bairro;

    @Column(name = "municipio", length = 100)
    private String municipio;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "cep", length = 8)
    private String cep;

    @Column(name = "segmentos", length = 30)
    @Enumerated(EnumType.STRING)
    private Segmentos segmentos;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StatusEmpresa status = StatusEmpresa.ATIVA;

    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "usuario_dono_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_empresa_usuario_dono")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Usuario usuarioDono;

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
    @Column(name = "deletado", nullable = false)
    private boolean deletado = false;

    @PrePersist
    @PreUpdate
    private void prePersistAndUpdate() {
        normalizarCampos();
        contingenciaParaFalhaDeApi();
        validarCamposObrigatorios();
    }

    private void normalizarCampos() {
        if (razaoSocial != null) razaoSocial = razaoSocial.trim();
        if (nomeFantasia != null) nomeFantasia = nomeFantasia.trim();
        if (cnpj != null) cnpj = cnpj.replaceAll("\\D", "").trim();
        if (email != null) email = email.trim().toLowerCase(Locale.ROOT);
        if (telefone != null) telefone = telefone.replaceAll("\\D", "").trim();
        if (inscricaoEstadual != null) inscricaoEstadual = inscricaoEstadual.trim();
        if (inscricaoMunicipal != null) inscricaoMunicipal = inscricaoMunicipal.trim();
        if (site != null) site = site.trim();
        if (logradouro != null) logradouro = logradouro.trim();
        if (numero != null) numero = numero.trim();
        if (bairro != null) bairro = bairro.trim();
        if (municipio != null) municipio = municipio.trim();
        if (uf != null) uf = uf.trim().toUpperCase(Locale.ROOT);
        if (cep != null) cep = cep.replaceAll("\\D", "").trim();
    }

    private void contingenciaParaFalhaDeApi() {
        if (isVazio(this.razaoSocial) && !isVazio(this.nomeFantasia)) {
            this.razaoSocial = this.nomeFantasia;
        }

        if (isVazio(this.razaoSocial) && !isVazio(this.cnpj)) {
            this.razaoSocial = "EMPRESA CNPJ: " + this.cnpj;
        }

        if (isVazio(this.nomeFantasia) && !isVazio(this.razaoSocial)) {
            this.nomeFantasia = this.razaoSocial;
        }
    }

    private void validarCamposObrigatorios() {
        if (isVazio(this.razaoSocial)) {
            throw new IllegalStateException("A razão social da empresa é obrigatória.");
        }
        if (isVazio(this.cnpj)) {
            throw new IllegalStateException("O CNPJ da empresa é obrigatório.");
        }
        if (this.cnpj != null && this.cnpj.length() != 14) {
            throw new IllegalStateException("O CNPJ deve conter exatamente 14 dígitos.");
        }
        if (this.usuarioDono == null || this.usuarioDono.getId() == null) {
            throw new IllegalStateException("O usuário dono da empresa é obrigatório.");
        }
    }

    private boolean isVazio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    public void ativar() { this.ativo = true; }
    public void inativar() { this.ativo = false; }
    public boolean estaAtiva() { return this.ativo && !this.deletado; }

    public boolean pertenceAoUsuario(Long usuarioId) {
        return usuarioId != null && this.usuarioDono != null && Objects.equals(this.usuarioDono.getId(), usuarioId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Empresa other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}