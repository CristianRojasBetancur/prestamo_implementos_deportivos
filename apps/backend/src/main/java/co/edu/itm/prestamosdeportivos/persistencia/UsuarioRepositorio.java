package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {
    boolean existsByIdentificacion(String identificacion);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByIdentificacionAndIdNot(String identificacion, UUID id);
    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, UUID id);
    Optional<Usuario> findByIdAndRol(UUID id, Rol rol);
    Optional<Usuario> findByIdentificacion(String identificacion);
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    default Optional<Usuario> buscarPorCorreo(String correo) {
        return findByCorreoIgnoreCase(correo);
    }

    default boolean existeCorreo(String correo) {
        return existsByCorreoIgnoreCase(correo);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id and u.rol = :rol")
    Optional<Usuario> bloquearPorIdAndRol(@Param("id") UUID id, @Param("rol") Rol rol);
    Optional<Usuario> findFirstByIdentificacionOrCorreoIgnoreCase(String identificacion, String correo);
}
