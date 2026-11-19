package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.Optional;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByIdentificacion(String identificacion);

    boolean existsByIdentificacion(String identificacion);

    /** Usa el índice único {@code uq_usuario_correo} sobre {@code lower(correo)}. */
    @Query("select u from Usuario u where lower(u.correo) = lower(:correo)")
    Optional<Usuario> buscarPorCorreo(@Param("correo") String correo);

    default boolean existeCorreo(String correo) {
        return buscarPorCorreo(correo).isPresent();
    }

    Optional<Usuario> findByIdAndRol(UUID id, Rol rol);

    /** Serializa préstamos dirigidos al mismo solicitante para proteger RN-01. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id and u.rol = :rol")
    Optional<Usuario> bloquearPorIdAndRol(@Param("id") UUID id, @Param("rol") Rol rol);
}
