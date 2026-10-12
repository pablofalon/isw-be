package org.proyecto.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.proyecto.data.ComponentePermisoRepository;
import org.proyecto.data.CuentaAccesoRepository;
import org.proyecto.entities.ComponentePermiso;
import org.proyecto.entities.ComposicionInvalidaException;
import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.GrupoPermisos;
import org.proyecto.entities.Permiso;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermisoServiceTest {

    @Mock
    private ComponentePermisoRepository componenteRepository;

    @Mock
    private CuentaAccesoRepository cuentaAccesoRepository;

    private PermisoService permisoService;
    private Permiso seguridad;
    private Permiso clasificar;
    private GrupoPermisos administrador;
    private CuentaAcceso admin;

    @BeforeEach
    void setUp() {
        permisoService = new PermisoService(componenteRepository, cuentaAccesoRepository);

        seguridad = conId(new Permiso(Permisos.SEGURIDAD_ADMINISTRAR, null), 1L);
        clasificar = conId(new Permiso(Permisos.TICKET_CLASIFICAR, null), 2L);
        GrupoPermisos gestion = conId(new GrupoPermisos("GESTION_TICKETS", null), 3L);
        gestion.agregar(clasificar);
        administrador = conId(new GrupoPermisos("ADMINISTRADOR", null), 4L);
        administrador.agregar(gestion);
        administrador.agregar(seguridad);

        admin = new CuentaAcceso();
        admin.setId(10L);
        admin.setActiva(true);
        admin.setComponentes(new ArrayList<>(List.of(administrador)));

        for (ComponentePermiso c : List.of(seguridad, clasificar, gestion, administrador)) {
            lenient().when(componenteRepository.findById(c.getId())).thenReturn(Optional.of(c));
        }
        lenient().when(cuentaAccesoRepository.findById(10L)).thenReturn(Optional.of(admin));
        lenient().when(cuentaAccesoRepository.findAll()).thenReturn(List.of(admin));
    }

    @Test
    void permisosDeRecorreElArbolDeLaCuenta() {
        assertEquals(Set.of(Permisos.SEGURIDAD_ADMINISTRAR, Permisos.TICKET_CLASIFICAR), permisoService.permisosDe(10L));
    }

    @Test
    void asignarComponentesReemplazaLosAnteriores() {
        CuentaAcceso operador = new CuentaAcceso();
        operador.setId(11L);
        operador.setActiva(true);
        when(cuentaAccesoRepository.findById(11L)).thenReturn(Optional.of(operador));
        when(cuentaAccesoRepository.findAll()).thenReturn(List.of(admin, operador));

        permisoService.asignarComponentes(11L, List.of(2L, 2L));

        assertEquals(List.of(clasificar), operador.getComponentes());
        assertTrue(permisoService.tienePermiso(operador, Permisos.TICKET_CLASIFICAR));
    }

    @Test
    void noSePuedeDejarAlSistemaSinAdministrador() {
        assertThrows(OperacionSeguridadInvalidaException.class,
                () -> permisoService.asignarComponentes(10L, List.of(2L)));
        assertThrows(OperacionSeguridadInvalidaException.class,
                () -> permisoService.quitarHijo(4L, 1L));
    }

    @Test
    void agregarHijoAUnPermisoSeRechaza() {
        assertThrows(OperacionSeguridadInvalidaException.class, () -> permisoService.agregarHijo(1L, 2L));
    }

    @Test
    void agregarHijoQueFormaCicloSeRechaza() {
        assertThrows(ComposicionInvalidaException.class, () -> permisoService.agregarHijo(3L, 4L));
    }

    @Test
    void componenteInexistenteDevuelveNoEncontrado() {
        when(componenteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> permisoService.agregarHijo(4L, 99L));
    }

    @Test
    void crearGrupoNormalizaElCodigoYRechazaDuplicados() {
        when(componenteRepository.existsByCodigo("MESA_DE_AYUDA")).thenReturn(false);
        when(componenteRepository.save(any(GrupoPermisos.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(componenteRepository.existsByCodigo("ADMINISTRADOR")).thenReturn(true);

        GrupoPermisos grupo = permisoService.crearGrupo(" mesa de ayuda ", "Primer nivel");

        assertEquals("MESA_DE_AYUDA", grupo.getCodigo());
        assertThrows(OperacionSeguridadInvalidaException.class, () -> permisoService.crearGrupo("administrador", null));
        assertThrows(OperacionSeguridadInvalidaException.class, () -> permisoService.crearGrupo("  ", null));
    }

    @Test
    void unaCuentaInactivaNoCuentaComoAdministrador() {
        admin.setActiva(false);
        CuentaAcceso otra = new CuentaAcceso();
        otra.setId(12L);
        otra.setActiva(true);
        when(cuentaAccesoRepository.findById(12L)).thenReturn(Optional.of(otra));
        when(cuentaAccesoRepository.findAll()).thenReturn(List.of(admin, otra));

        assertThrows(OperacionSeguridadInvalidaException.class, () -> permisoService.asignarComponentes(12L, List.of(2L)));
    }

    private static <T extends ComponentePermiso> T conId(T componente, Long id) {
        componente.setId(id);
        return componente;
    }
}
