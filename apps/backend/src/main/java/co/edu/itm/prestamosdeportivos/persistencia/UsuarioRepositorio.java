package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import co.edu.itm.prestamosdeportivos.dominio.RolUsuario;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {
    boolean existsByIdentificacion(String identificacion);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByIdentificacionAndIdNot(String identificacion, UUID id);
    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, UUID id);
    Optional<Usuario> findByIdAndRol(UUID id, RolUsuario rol);
    Optional<Usuario> findFirstByIdentificacionOrCorreoIgnoreCase(String identificacion, String correo);
}
