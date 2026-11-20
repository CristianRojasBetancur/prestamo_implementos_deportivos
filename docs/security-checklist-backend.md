# Checklist de seguridad del backend

| Control | Evidencia |
|---|---|
| Autenticación Bearer JWT | `ConfiguracionSeguridad`, `ConfiguracionJwt`, `SeguridadAutenticacionTest` |
| Autorización deny-by-default | Matriz de `ConfiguracionSeguridad` y `@PreAuthorize` en controladores |
| Roles no confiados desde el cliente | `ConversorJwtAUsuario` carga el rol desde PostgreSQL |
| Contraseñas | `ConfiguracionContrasenas` usa BCrypt con factor 12 y prefijo delegable |
| Expiración y revocación de tokens | `PropiedadesJwt`, `EmisorTokens`, `AutenticacionServicio` |
| CORS | `PropiedadesCorsTest` y `ConfiguracionCors`; orígenes explícitos, sin `*` |
| Headers | Content-Type Options, frame deny, cache control, Referrer-Policy y HSTS para HTTPS |
| CSRF | Deshabilitado solo por API stateless con Bearer token y sin cookies de sesión |
| Validación de entrada | Bean Validation, máximo de 100 elementos por página, máximo de 80 caracteres en búsquedas |
| Exposición de secretos | Configuración por variables de entorno; errores sin mensajes técnicos |
| Logs | No se registran contraseñas, tokens, hashes ni payloads; correlación mediante MDC |
| Dependencias | Health/readiness reporta PostgreSQL no disponible como 503 sin revelar credenciales |
| Integridad transaccional | Pruebas de rollback y concurrencia en servicios de préstamo, devolución e incidencia |

La revisión no incluye autenticación multifactor, rate limiting distribuido,
WAF, gestión externa de secretos ni escaneo de infraestructura, porque no
pertenecen al alcance local del MVP.
