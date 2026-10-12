package org.proyecto.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.proyecto.data.ComponentePermisoRepository;
import org.proyecto.data.CuentaAccesoRepository;
import org.proyecto.data.OperadorRepository;
import org.proyecto.data.TicketRepository;
import org.proyecto.data.UsuarioTicketRepository;
import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.GrupoPermisos;
import org.proyecto.entities.Operador;
import org.proyecto.entities.Permiso;
import org.proyecto.entities.Rol;
import org.proyecto.entities.Ticket;
import org.proyecto.entities.TicketEstado;
import org.proyecto.entities.TicketEstadoCodigo;
import org.proyecto.entities.UsuarioTicket;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private CuentaAccesoRepository cuentaAccesoRepository;
    @Mock
    private UsuarioTicketRepository usuarioTicketRepository;
    @Mock
    private OperadorRepository operadorRepository;
    @Mock
    private ComponentePermisoRepository componenteRepository;
    @Mock
    private TicketRepository ticketRepository;

    private final CryptoManager cryptoManager = new CryptoManager();
    private UsuarioService usuarioService;

    private final DatosPersona datosUsuario =
            new DatosPersona("Lucía", "Fernández", "Lucia.Fernandez@empresa.com", "1144556677", null, null);
    private final DatosPersona datosOperador =
            new DatosPersona("Martín", "Ruiz", "martin.ruiz@empresa.com", null, "soporte técnico", "analista");

    @BeforeEach
    void setUp() {
        // PermisoService real sobre los repositorios mockeados (Mockito no puede mockear clases concretas en JDK 25).
        PermisoService permisoService = new PermisoService(componenteRepository, cuentaAccesoRepository);
        usuarioService = new UsuarioService(cuentaAccesoRepository, usuarioTicketRepository, operadorRepository,
                componenteRepository, ticketRepository, permisoService, cryptoManager);
        lenient().when(cuentaAccesoRepository.findAll()).thenReturn(List.of(administradorActivo()));
        lenient().when(cuentaAccesoRepository.findByUsuarioLogin(anyString())).thenReturn(Optional.empty());
        lenient().when(usuarioTicketRepository.findByCorreoElectronico(anyString())).thenReturn(Optional.empty());
        lenient().when(operadorRepository.findByCorreoElectronico(anyString())).thenReturn(Optional.empty());
        lenient().when(cuentaAccesoRepository.save(any(CuentaAcceso.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(usuarioTicketRepository.save(any(UsuarioTicket.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(operadorRepository.save(any(Operador.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void altaDeUsuarioCreaPersonaYCuentaConPasswordTemporalYGrupoInicial() {
        GrupoPermisos usuarioFinal = new GrupoPermisos("USUARIO_FINAL", null);
        when(componenteRepository.findByCodigo("USUARIO_FINAL")).thenReturn(Optional.of(usuarioFinal));

        CuentaAcceso cuenta = usuarioService.crear(Rol.USUARIO, " Lucia.Fernandez ", "Temporal1", datosUsuario);

        assertEquals("lucia.fernandez", cuenta.getUsuarioLogin());
        assertEquals(Rol.USUARIO, cuenta.getRol());
        assertTrue(cuenta.isDebeCambiarPassword());
        assertTrue(cryptoManager.compare("Temporal1", cuenta.getPasswordHash()));
        assertNotNull(cuenta.getUsuarioTicket());
        assertEquals("lucia.fernandez@empresa.com", cuenta.getUsuarioTicket().getCorreoElectronico());
        assertEquals(List.of(usuarioFinal), cuenta.getComponentes());
    }

    @Test
    void altaDeOperadorYAdministradorSeVinculanAUnOperador() {
        CuentaAcceso operador = usuarioService.crear(Rol.OPERADOR, "mruiz", "Temporal1", datosOperador);
        CuentaAcceso admin = usuarioService.crear(Rol.ADMINISTRADOR, "jefe", "Temporal1",
                new DatosPersona("Ana", "Jefa", "jefa@empresa.com", null, null, null));

        assertEquals("soporte técnico", operador.getOperador().getArea());
        assertEquals("analista", operador.getOperador().getCargo());
        assertNotNull(admin.getOperador());
        assertEquals(null, admin.getUsuarioTicket());
    }

    @Test
    void altaRechazaLoginOCorreoDuplicadosYDatosInvalidos() {
        when(cuentaAccesoRepository.findByUsuarioLogin("juan.perez")).thenReturn(Optional.of(new CuentaAcceso()));
        UsuarioTicket existente = new UsuarioTicket();
        existente.setId(1L);
        when(usuarioTicketRepository.findByCorreoElectronico("lucia.fernandez@empresa.com")).thenReturn(Optional.of(existente));

        assertThrows(OperacionSeguridadInvalidaException.class,
                () -> usuarioService.crear(Rol.USUARIO, "juan.perez", "Temporal1", datosOperador));
        assertThrows(OperacionSeguridadInvalidaException.class,
                () -> usuarioService.crear(Rol.USUARIO, "lucia", "Temporal1", datosUsuario));
        assertThrows(OperacionSeguridadInvalidaException.class,
                () -> usuarioService.crear(Rol.USUARIO, "con espacio", "Temporal1", datosOperador));
        assertThrows(OperacionSeguridadInvalidaException.class,
                () -> usuarioService.crear(null, "nuevo", "Temporal1", datosOperador));
        assertThrows(OperacionSeguridadInvalidaException.class, () -> usuarioService.crear(Rol.USUARIO, "nuevo", "Temporal1",
                new DatosPersona("A", "B", "sin-arroba", null, null, null)));
        assertThrows(OperacionSeguridadInvalidaException.class, () -> usuarioService.crear(Rol.USUARIO, "nuevo", "Temporal1",
                new DatosPersona(" ", "B", "b@b.com", null, null, null)));
        verify(cuentaAccesoRepository, never()).save(any());
    }

    @Test
    void politicaDePasswordExigeOchoCaracteresConLetrasYNumeros() {
        assertThrows(OperacionSeguridadInvalidaException.class, () -> UsuarioService.validarPolitica("corta1"));
        assertThrows(OperacionSeguridadInvalidaException.class, () -> UsuarioService.validarPolitica("sinnumeros"));
        assertThrows(OperacionSeguridadInvalidaException.class, () -> UsuarioService.validarPolitica("12345678"));
        assertThrows(OperacionSeguridadInvalidaException.class, () -> UsuarioService.validarPolitica(null));
        UsuarioService.validarPolitica("usuario123");
    }

    @Test
    void modificarPermiteConservarElPropioCorreoPeroNoUnoAjeno() {
        CuentaAcceso cuenta = cuentaDeUsuario(10L, 5L, "lucia.fernandez@empresa.com");
        when(cuentaAccesoRepository.findById(10L)).thenReturn(Optional.of(cuenta));
        when(usuarioTicketRepository.findByCorreoElectronico("lucia.fernandez@empresa.com"))
                .thenReturn(Optional.of(cuenta.getUsuarioTicket()));
        Operador otro = new Operador();
        otro.setId(9L);
        when(operadorRepository.findByCorreoElectronico("martin.ruiz@empresa.com")).thenReturn(Optional.of(otro));

        usuarioService.modificar(10L, new DatosPersona("Lucía", "Fernández Paz", "lucia.fernandez@empresa.com", null, null, null));

        assertEquals("Fernández Paz", cuenta.getUsuarioTicket().getApellido());
        assertThrows(OperacionSeguridadInvalidaException.class, () -> usuarioService.modificar(10L,
                new DatosPersona("Lucía", "F", "martin.ruiz@empresa.com", null, null, null)));
    }

    @Test
    void bajaEsLogicaYCierraLasSesiones() {
        CuentaAcceso cuenta = cuentaDeUsuario(10L, 5L, "lucia@empresa.com");
        when(cuentaAccesoRepository.findById(10L)).thenReturn(Optional.of(cuenta));
        String token = SessionManager.getInstance().createSession(cuenta, Set.of());

        usuarioService.desactivar(10L, 1L);

        assertFalse(cuenta.isActiva());
        assertFalse(cuenta.getUsuarioTicket().isActivo());
        assertTrue(SessionManager.getInstance().getSession(token).isEmpty());
    }

    @Test
    void bajaQueDejaAlSistemaSinAdministradorSeRechaza() {
        CuentaAcceso admin = administradorActivo();
        when(cuentaAccesoRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(cuentaAccesoRepository.findAll()).thenReturn(List.of(admin));
        when(ticketRepository.findByOperadorIdOrderByFechaCreacionDesc(admin.getOperador().getId())).thenReturn(List.of());

        assertThrows(OperacionSeguridadInvalidaException.class, () -> usuarioService.desactivar(admin.getId(), 1L));
    }

    @Test
    void noSePuedeDarDeBajaLaPropiaCuenta() {
        assertThrows(OperacionSeguridadInvalidaException.class, () -> usuarioService.desactivar(1L, 1L));
    }

    @Test
    void noSePuedeDarDeBajaUnOperadorConTicketsSinCerrar() {
        CuentaAcceso cuenta = cuentaDeOperador(20L, 7L);
        when(cuentaAccesoRepository.findById(20L)).thenReturn(Optional.of(cuenta));
        when(ticketRepository.findByOperadorIdOrderByFechaCreacionDesc(7L))
                .thenReturn(List.of(ticketEn(TicketEstadoCodigo.EN_ANALISIS), ticketEn(TicketEstadoCodigo.CERRADO)));

        OperacionSeguridadInvalidaException ex = assertThrows(OperacionSeguridadInvalidaException.class,
                () -> usuarioService.desactivar(20L, 1L));

        assertTrue(ex.getMessage().contains("1 ticket"));
        assertTrue(cuenta.isActiva());
    }

    @Test
    void activarReactivaYDesbloquea() {
        CuentaAcceso cuenta = cuentaDeOperador(20L, 7L);
        cuenta.setActiva(false);
        cuenta.setIntentosFallidos(5);
        cuenta.getOperador().setActivo(false);
        when(cuentaAccesoRepository.findById(20L)).thenReturn(Optional.of(cuenta));

        usuarioService.activar(20L);

        assertTrue(cuenta.isActiva());
        assertEquals(0, cuenta.getIntentosFallidos());
        assertTrue(cuenta.getOperador().isActivo());
    }

    @Test
    void blanquearDejaPasswordTemporalYCierraSesiones() {
        CuentaAcceso cuenta = cuentaDeUsuario(10L, 5L, "lucia@empresa.com");
        when(cuentaAccesoRepository.findById(10L)).thenReturn(Optional.of(cuenta));
        String token = SessionManager.getInstance().createSession(cuenta, Set.of());

        usuarioService.blanquearPassword(10L, "Blanqueo9");

        assertTrue(cuenta.isDebeCambiarPassword());
        assertTrue(cryptoManager.compare("Blanqueo9", cuenta.getPasswordHash()));
        assertTrue(SessionManager.getInstance().getSession(token).isEmpty());
    }

    @Test
    void cambiarPasswordPropiaValidaLaActualYLiberaLaSesion() {
        CuentaAcceso cuenta = cuentaDeUsuario(10L, 5L, "lucia@empresa.com");
        cuenta.setPasswordHash(cryptoManager.hash("Temporal1"));
        cuenta.setDebeCambiarPassword(true);
        when(cuentaAccesoRepository.findById(10L)).thenReturn(Optional.of(cuenta));
        String token = SessionManager.getInstance().createSession(cuenta, Set.of());

        assertThrows(OperacionSeguridadInvalidaException.class,
                () -> usuarioService.cambiarPasswordPropia(10L, "incorrecta1", "Definitiva1"));
        assertThrows(OperacionSeguridadInvalidaException.class,
                () -> usuarioService.cambiarPasswordPropia(10L, "Temporal1", "Temporal1"));
        assertTrue(SessionManager.getInstance().getSession(token).orElseThrow().debeCambiarPassword());

        usuarioService.cambiarPasswordPropia(10L, "Temporal1", "Definitiva1");

        assertFalse(cuenta.isDebeCambiarPassword());
        assertTrue(cryptoManager.compare("Definitiva1", cuenta.getPasswordHash()));
        assertFalse(SessionManager.getInstance().getSession(token).orElseThrow().debeCambiarPassword());
        SessionManager.getInstance().invalidateSession(token);
    }

    private static CuentaAcceso administradorActivo() {
        GrupoPermisos administrador = new GrupoPermisos("ADMINISTRADOR", null);
        administrador.agregar(new Permiso(Permisos.SEGURIDAD_ADMINISTRAR, null));
        CuentaAcceso admin = cuentaDeOperador(99L, 98L);
        admin.setRol(Rol.ADMINISTRADOR);
        admin.setComponentes(List.of(administrador));
        return admin;
    }

    private static CuentaAcceso cuentaDeUsuario(Long cuentaId, Long usuarioId, String correo) {
        UsuarioTicket usuario = new UsuarioTicket();
        usuario.setId(usuarioId);
        usuario.setNombre("Lucía");
        usuario.setApellido("Fernández");
        usuario.setCorreoElectronico(correo);
        CuentaAcceso cuenta = new CuentaAcceso();
        cuenta.setId(cuentaId);
        cuenta.setRol(Rol.USUARIO);
        cuenta.setUsuarioTicket(usuario);
        return cuenta;
    }

    private static CuentaAcceso cuentaDeOperador(Long cuentaId, Long operadorId) {
        Operador operador = new Operador();
        operador.setId(operadorId);
        CuentaAcceso cuenta = new CuentaAcceso();
        cuenta.setId(cuentaId);
        cuenta.setRol(Rol.OPERADOR);
        cuenta.setOperador(operador);
        return cuenta;
    }

    private static Ticket ticketEn(TicketEstadoCodigo codigo) {
        Ticket ticket = new Ticket();
        ticket.setEstadoActual(new TicketEstado(codigo, codigo.name(), null, false, false));
        return ticket;
    }
}
