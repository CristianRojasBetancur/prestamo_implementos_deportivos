/**
 * Autenticación y autorización (RF-01, RF-02, RNF-03; BE-01 §5): emisión y
 * validación de JWT HS256 con Nimbus, hash de contraseñas con BCrypt,
 * conversión del token en un {@link co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado}
 * verificado contra la base de datos en cada solicitud, y revocación por
 * versión de credenciales. Decisiones: docs/specs/BE-01-api-backend-mvp.md §5.
 */
package co.edu.itm.prestamosdeportivos.seguridad;
