package br.com.taskflow.gerenciamento.usuarios.repositorio;

import br.com.taskflow.gerenciamento.usuarios.entidade.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCaseAndDeletadoFalse(String email);

    boolean existsByEmailIgnoreCaseAndDeletadoFalse(String email);

    Optional<Usuario> findByIdAndDeletadoFalse(Long id);

    Optional<Usuario> findByEmail(String email);
}