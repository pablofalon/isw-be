package org.proyecto.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.proyecto.data.AuditoriaParametroRepository;
import org.proyecto.data.CategoriaRepository;
import org.proyecto.data.ParametroSistemaRepository;
import org.proyecto.data.PrioridadRepository;
import org.proyecto.entities.AuditoriaParametro;
import org.proyecto.entities.Categoria;
import org.proyecto.entities.NivelPrioridad;
import org.proyecto.entities.ParametroSistema;
import org.proyecto.entities.Prioridad;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParametroServiceTest {

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private PrioridadRepository prioridadRepository;
    @Mock private ParametroSistemaRepository parametroRepository;
    @Mock private AuditoriaParametroRepository auditoriaRepository;

    private ParametroService service;
    private ParametroSistema umbral;

    @BeforeEach
    void setUp() {
        service = new ParametroService(categoriaRepository, prioridadRepository, parametroRepository, auditoriaRepository);
        umbral = new ParametroSistema(ParametroSistema.UMBRAL_ALERTA_TIEMPOS, "Umbral", "%", 80, 1, 100);
        lenient().when(parametroRepository.findById(ParametroSistema.UMBRAL_ALERTA_TIEMPOS)).thenReturn(Optional.of(umbral));
        lenient().when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(prioridadRepository.save(any(Prioridad.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(parametroRepository.save(any(ParametroSistema.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void elUmbralVigenteSeExpresaComoFraccion() {
        assertEquals(0.8, service.getUmbralAlerta(), 1e-9);
        umbral.setValor(50);
        assertEquals(0.5, service.getUmbralAlerta(), 1e-9);
    }

    @Test
    void crearCategoriaLaGuardaYAudita() {
        when(categoriaRepository.findByNombreIgnoreCase("Facturación")).thenReturn(Optional.empty());

        Categoria creada = service.crearCategoria("admin", "  Facturación ", "Consultas de facturas", "finanzas");

        assertEquals("Facturación", creada.getNombre());
        assertTrue(creada.isActiva());
        AuditoriaParametro auditoria = capturarAuditoria();
        assertEquals("admin", auditoria.getAdministrador());
        assertEquals("ALTA", auditoria.getAccion());
        assertEquals("CATEGORIAS", auditoria.getGrupo());
    }

    @Test
    void crearCategoriaDuplicadaEsRechazada() {
        Categoria existente = new Categoria("Reportes", "desc");
        existente.setId(1L);
        when(categoriaRepository.findByNombreIgnoreCase("reportes")).thenReturn(Optional.of(existente));

        assertThrows(ParametroInvalidoException.class, () -> service.crearCategoria("admin", "reportes", null, null));

        verify(categoriaRepository, never()).save(any());
        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void crearCategoriaSinNombreEsRechazada() {
        assertThrows(ParametroInvalidoException.class, () -> service.crearCategoria("admin", "  ", null, null));
    }

    @Test
    void modificarCategoriaPuedeConservarSuPropioNombre() {
        Categoria categoria = new Categoria("Reportes", "vieja", "reportes");
        categoria.setId(1L);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.findByNombreIgnoreCase("Reportes")).thenReturn(Optional.of(categoria));

        Categoria modificada = service.modificarCategoria("admin", 1L, "Reportes", "nueva", "reportes");

        assertEquals("nueva", modificada.getDescripcion());
        AuditoriaParametro auditoria = capturarAuditoria();
        assertTrue(auditoria.getValorAnterior().contains("vieja"));
        assertTrue(auditoria.getValorNuevo().contains("nueva"));
    }

    @Test
    void desactivarCategoriaRegistraElCambioYNoBorra() {
        Categoria categoria = new Categoria("Reportes", "desc");
        categoria.setId(1L);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        Categoria resultado = service.cambiarEstadoCategoria("admin", 1L, false);

        assertFalse(resultado.isActiva());
        assertEquals("DESACTIVACION", capturarAuditoria().getAccion());
        verify(categoriaRepository, never()).delete(any());
    }

    @Test
    void modificarPrioridadValidaElRangoDeHoras() {
        Prioridad prioridad = new Prioridad(NivelPrioridad.ALTA, "desc", 24);
        prioridad.setId(2L);
        when(prioridadRepository.findById(2L)).thenReturn(Optional.of(prioridad));

        assertThrows(ParametroInvalidoException.class, () -> service.modificarPrioridad("admin", 2L, "x", 0));
        assertThrows(ParametroInvalidoException.class, () -> service.modificarPrioridad("admin", 2L, "x", null));

        Prioridad modificada = service.modificarPrioridad("admin", 2L, "Urgente", 12);
        assertEquals(12, modificada.getTiempoObjetivoRespuesta());
        assertTrue(capturarAuditoria().getValorAnterior().contains("24 h"));
    }

    @Test
    void modificarGeneralValidaElRangoYGuardaElValorAnterior() {
        assertThrows(ParametroInvalidoException.class,
                () -> service.modificarGeneral("admin", ParametroSistema.UMBRAL_ALERTA_TIEMPOS, 101));
        assertThrows(ParametroInvalidoException.class,
                () -> service.modificarGeneral("admin", ParametroSistema.UMBRAL_ALERTA_TIEMPOS, 0));
        assertEquals(80, umbral.getValor());

        service.modificarGeneral("admin", ParametroSistema.UMBRAL_ALERTA_TIEMPOS, 70);

        assertEquals(70, umbral.getValor());
        AuditoriaParametro auditoria = capturarAuditoria();
        assertEquals("80 %", auditoria.getValorAnterior());
        assertEquals("70 %", auditoria.getValorNuevo());
    }

    @Test
    void parametroInexistenteEsNoEncontrado() {
        when(parametroRepository.findById("inexistente")).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.modificarGeneral("admin", "inexistente", 1));
    }

    @Test
    void restaurarGeneralesVuelveAlValorPorDefecto() {
        umbral.setValor(30);
        when(parametroRepository.findAll()).thenReturn(List.of(umbral));

        service.restaurar("admin", GrupoParametros.GENERALES);

        assertEquals(80, umbral.getValor());
        assertEquals("RESTAURACION", capturarAuditoria().getAccion());
    }

    @Test
    void restaurarPrioridadesVuelveALosTiemposIniciales() {
        Prioridad critica = new Prioridad(NivelPrioridad.CRITICA, "otra", 1);
        when(prioridadRepository.findAll()).thenReturn(List.of(critica));

        service.restaurar("admin", GrupoParametros.PRIORIDADES);

        assertEquals(4, critica.getTiempoObjetivoRespuesta());
    }

    @Test
    void restaurarCategoriasReactivaIniciales_RecreaFaltantesYDesactivaAgregadas() {
        Categoria reportes = new Categoria("Reportes", "modificada", "otra área");
        reportes.setActiva(false);
        Categoria agregada = new Categoria("Facturación", "x");
        when(categoriaRepository.findAllByOrderByNombre()).thenReturn(List.of(agregada, reportes));

        service.restaurar("admin", GrupoParametros.CATEGORIAS);

        assertTrue(reportes.isActiva());
        assertEquals("Errores o solicitudes sobre reportes", reportes.getDescripcion());
        assertFalse(agregada.isActiva());
        // 4 iniciales faltantes recreadas + reportes + agregada desactivada
        verify(categoriaRepository, times(6)).save(any(Categoria.class));
    }

    private AuditoriaParametro capturarAuditoria() {
        ArgumentCaptor<AuditoriaParametro> captor = ArgumentCaptor.forClass(AuditoriaParametro.class);
        verify(auditoriaRepository).save(captor.capture());
        return captor.getValue();
    }
}
