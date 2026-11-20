package co.edu.itm.prestamosdeportivos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import co.edu.itm.prestamosdeportivos.ContenedoresPrueba;
import co.edu.itm.prestamosdeportivos.dominio.Categoria;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.EventoEstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.persistencia.CategoriaRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.HistorialEstadoImplementoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.PrestamoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/** PostgreSQL real para rollback y serialización pesimista de RN-06. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PrestamoServicioIntegracionTest {
    @Autowired private PrestamoServicio servicio;
    @Autowired private UsuarioRepositorio usuarios;
    @Autowired private CategoriaRepositorio categorias;
    @Autowired private ImplementoDeportivoRepositorio implementos;
    @Autowired private PrestamoRepositorio prestamos;
    @Autowired private HistorialEstadoImplementoRepositorio historial;
    @Autowired private MockMvc mvc;

    @Test
    @DisplayName("POST canónico crea préstamo con operador y responde 201")
    void controladorRegistraConRutaCanonica() throws Exception {
        var f=fixture(EstadoImplemento.DISPONIBLE);
        var auth=UsernamePasswordAuthenticationToken.authenticated(f.operador(),"test",
                List.of(new SimpleGrantedAuthority("ROLE_PRESTADOR")));
        mvc.perform(post("/api/v1/prestamos").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":\""+f.usuario().getId()+"\",\"implementoIds\":[\""+f.unidades().get(0).getId()+"\"]}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/prestamos/")))
                .andExpect(jsonPath("$.estado").value("VIGENTE"))
                .andExpect(jsonPath("$.prestador.id").value(f.operador().id().toString()))
                .andExpect(jsonPath("$.detalles[0].implemento.id").value(f.unidades().get(0).getId().toString()));
    }

    @Test
    @DisplayName("préstamo mueve unidades con auditoría y una validación fallida no deja cambios")
    void registraAtomicoYRevierteAlFallar() {
        var f=fixture(EstadoImplemento.DISPONIBLE,EstadoImplemento.EN_MANTENIMIENTO);
        long inicial=prestamos.count();
        assertThatThrownBy(()->servicio.registrar(f.usuario().getId(),List.of(f.unidades().get(0).getId(),f.unidades().get(1).getId()),f.operador()))
                .isInstanceOf(ExcepcionDominio.class);
        assertThat(prestamos.count()).isEqualTo(inicial);
        assertThat(implementos.findById(f.unidades().get(0).getId()).orElseThrow().getEstado()).isEqualTo(EstadoImplemento.DISPONIBLE);

        var respuesta=servicio.registrar(f.usuario().getId(),List.of(f.unidades().get(0).getId()),f.operador());
        assertThat(respuesta.estado().name()).isEqualTo("VIGENTE");
        assertThat(respuesta.fechaLimiteDevolucion().toInstant()).isEqualTo(respuesta.fechaPrestamo().toInstant().plusSeconds(86400));
        assertThat(respuesta.prestador().id()).isEqualTo(f.operador().id());
        assertThat(implementos.findById(f.unidades().get(0).getId()).orElseThrow().getEstado()).isEqualTo(EstadoImplemento.PRESTADO);
        assertThat(historial.findAll()).anySatisfy(h->{assertThat(h.getEvento()).isEqualTo(EventoEstadoImplemento.PRESTAMO);assertThat(h.getEstadoNuevo()).isEqualTo(EstadoImplemento.PRESTADO);assertThat(h.getOperador().getId()).isEqualTo(f.operador().id());});
    }

    @Test
    @DisplayName("dos operaciones concurrentes por la misma unidad solo crean un préstamo")
    void serializaPrestamosConcurrentesPorUnidad() throws Exception {
        var f=fixture(EstadoImplemento.DISPONIBLE);
        long prestamosIniciales=prestamos.count();
        var segundo=usuarios.saveAndFlush(Usuario.solicitante("u"+UUID.randomUUID().toString().replace("-", "").substring(0, 20),"Segundo","Solicitante"));
        CountDownLatch inicio=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            var uno=pool.submit(()->ejecutar(inicio,f.usuario().getId(),f));
            var dos=pool.submit(()->ejecutar(inicio,segundo.getId(),f));
            inicio.countDown();
            var resultados=List.of(uno.get(30,TimeUnit.SECONDS),dos.get(30,TimeUnit.SECONDS));
            assertThat(resultados).containsExactlyInAnyOrder(true,false);
        }
        assertThat(implementos.findById(f.unidades().get(0).getId()).orElseThrow().getEstado()).isEqualTo(EstadoImplemento.PRESTADO);
        assertThat(prestamos.count()).isEqualTo(prestamosIniciales+1);
    }

    private boolean ejecutar(CountDownLatch inicio,UUID usuario,Fixture f){
        try{inicio.await();servicio.registrar(usuario,List.of(f.unidades().get(0).getId()),f.operador());return true;}
        catch(ExcepcionDominio conflicto){return false;}
        catch(DataIntegrityViolationException | PessimisticLockingFailureException conflicto){return false;}
        catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException(ex);}
    }

    private Fixture fixture(EstadoImplemento... estados){
        String sufijo=UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        var usuario=usuarios.saveAndFlush(Usuario.solicitante("u"+sufijo,"Usuario","Prueba"));
        var prestador=usuarios.saveAndFlush(Usuario.conAcceso("p"+sufijo,"Prestador","Prueba","p"+sufijo+"@itm.edu.co","hash",Rol.PRESTADOR));
        var categoria=categorias.saveAndFlush(new Categoria("Cat "+sufijo,null));
        var unidades=java.util.Arrays.stream(estados).map(estado->implementos.saveAndFlush(new ImplementoDeportivo("COD-"+UUID.randomUUID().toString().substring(0, 8),"Unidad de prueba",null,categoria,estado))).toList();
        return new Fixture(usuario,unidades,new UsuarioAutenticado(prestador.getId(),Rol.PRESTADOR));
    }
    private record Fixture(Usuario usuario,List<ImplementoDeportivo> unidades,UsuarioAutenticado operador){}
}
