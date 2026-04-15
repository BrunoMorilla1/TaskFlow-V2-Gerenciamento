package br.com.taskflow.gerenciamento.empresa.repositorio;

import br.com.taskflow.gerenciamento.empresa.entidade.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRepositorio extends JpaRepository<Empresa, Long> {

    List<Empresa> findAllByUsuarioDonoId(Long usuarioId);

    Optional<Empresa> findByIdAndUsuarioDonoId(Long id, Long usuarioId);

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, Long id);

    List<Empresa> findAllByUsuarioDonoIdAndAtivoTrue(Long usuarioId);

    Optional<Empresa> findByIdAndUsuarioDonoIdAndAtivoTrue(Long id, Long usuarioId);
}