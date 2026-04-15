package br.com.taskflow.gerenciamento.seguranca.autenticacao.usuario;

import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Getter
public final class UsuarioAutenticado implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final String senha;
    private final String tenantId;
    private final Collection<? extends GrantedAuthority> authorities;

    private final boolean ativo;
    private final boolean contaNaoExpirada;
    private final boolean credencialNaoExpirada;
    private final boolean contaNaoBloqueada;
    private final boolean habilitado;
    private final boolean deletado;

    public UsuarioAutenticado(Usuario usuario) {
        Objects.requireNonNull(usuario, "O usuário é obrigatório para autenticação.");

        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.senha = usuario.getSenha();
        this.tenantId = null;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()));

        this.ativo = usuario.isAtivo();
        this.contaNaoExpirada = usuario.isAccountNonExpired();
        this.credencialNaoExpirada = usuario.isCredentialsNonExpired();
        this.contaNaoBloqueada = usuario.isAccountNonLocked();
        this.habilitado = usuario.isEnabled();
        this.deletado = usuario.isDeletado();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
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
        return ativo && habilitado && !deletado;
    }
}