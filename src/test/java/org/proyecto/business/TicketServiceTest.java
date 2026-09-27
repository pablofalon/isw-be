package org.proyecto.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.proyecto.data.AdjuntoRepository;
import org.proyecto.data.CategoriaRepository;
import org.proyecto.data.HistorialAsignacionRepository;
import org.proyecto.data.NotificacionRepository;
import org.proyecto.data.OperadorRepository;
import org.proyecto.data.PrioridadRepository;
import org.proyecto.data.TicketEstadoRepository;
import org.proyecto.data.TicketRepository;
import org.proyecto.entities.Categoria;
import org.proyecto.entities.NivelPrioridad;
import org.proyecto.entities.Operador;
import org.proyecto.entities.Prioridad;
import org.proyecto.entities.Ticket;
import org.proyecto.entities.TicketEstado;
import org.proyecto.entities.TicketEstadoCodigo;
import org.proyecto.entities.UsuarioTicket;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private AdjuntoRepository adjuntoRepository;
    @Mock private HistorialAsignacionRepository historialAsignacionRepository;
    @Mock private TicketEstadoRepository ticketEstadoRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private PrioridadRepository prioridadRepository;
    @Mock private OperadorRepository operadorRepository;
    @Mock private NotificacionRepository notificacionRepository;

    private TicketService ticketService;
    private UsuarioTicket usuario;
    private Operador operador;
    private Operador otroOperador;
    private Categoria categoria;
    private Prioridad prioridad;

    @BeforeEach
    void setUp() {
        NotificacionService notificacionService = new NotificacionService(notificacionRepository);
        ticketService = new TicketService(ticketRepository, adjuntoRepository, historialAsignacionRepository,
                ticketEstadoRepository, categoriaRepository, prioridadRepository, operadorRepository, notificacionService);

        usuario = new UsuarioTicket();
        usuario.setId(1L);

        operador = new Operador();
        operador.setId(10L);
        operador.setActivo(true);

        otroOperador = new Operador();
        otroOperador.setId(20L);
        otroOperador.setActivo(true);

        categoria = new Categoria("Problemas de acceso", "desc");
        categoria.setId(100L);

        prioridad = new Prioridad(NivelPrioridad.ALTA, "desc", 24);
        prioridad.setId(200L);

        lenient().when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(categoriaRepository.findById(100L)).thenReturn(Optional.of(categoria));
        lenient().when(prioridadRepository.findById(200L)).thenReturn(Optional.of(prioridad));
        lenient().when(operadorRepository.findById(10L)).thenReturn(Optional.of(operador));
        lenient().when(operadorRepository.findById(20L)).thenReturn(Optional.of(otroOperador));
        lenient().when(ticketEstadoRepository.findByCodigo(any(TicketEstadoCodigo.class)))
                .thenAnswer(inv -> Optional.of(estadoFalso(inv.getArgument(0))));
    }

    private TicketEstado estadoFalso(TicketEstadoCodigo codigo) {
        boolean permiteReapertura = codigo == TicketEstadoCodigo.CERRADO;
        return new TicketEstado(codigo, codigo.name(), null, false, permiteReapertura);
    }

    private Ticket ticketEnEstado(TicketEstadoCodigo codigo) {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setUsuario(usuario);
        ticket.setEstadoActual(estadoFalso(codigo));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        return ticket;
    }

    @Test
    void registrarTicketConDatosValidosQuedaEnNuevo() {
        Ticket ticket = ticketService.registrarTicket(usuario, "Titulo", "Descripcion", null, null);

        assertEquals(TicketEstadoCodigo.NUEVO, ticket.getEstadoActual().getCodigo());
        assertEquals(1, ticket.getHistorialEstados().size());
    }

    @Test
    void registrarTicketSinTituloFalla() {
        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.registrarTicket(usuario, " ", "Descripcion", null, null));
    }

    @Test
    void validarTicketSinClasificarNiPriorizarFalla() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.NUEVO);

        assertThrows(InvalidTicketOperationException.class, () -> ticketService.validarTicket(1L, operador));
        assertEquals(TicketEstadoCodigo.NUEVO, ticket.getEstadoActual().getCodigo());
    }

    @Test
    void flujoCompletoDeClasificacionPriorizacionYValidacion() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.NUEVO);

        ticketService.clasificar(1L, 100L);
        assertEquals(categoria, ticket.getCategoria());

        ticketService.asignarPrioridad(1L, 200L);
        assertEquals(prioridad, ticket.getPrioridad());

        Ticket validado = ticketService.validarTicket(1L, operador);
        assertEquals(TicketEstadoCodigo.VALIDADO, validado.getEstadoActual().getCodigo());
    }

    @Test
    void asignarPrioridadSinClasificarFalla() {
        ticketEnEstado(TicketEstadoCodigo.NUEVO);

        assertThrows(InvalidTicketOperationException.class, () -> ticketService.asignarPrioridad(1L, 200L));
    }

    @Test
    void asignarOperadorDesdeValidadoPasaAEnAnalisis() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.VALIDADO);

        Ticket asignado = ticketService.asignarOperador(1L, 10L, operador);

        assertEquals(TicketEstadoCodigo.EN_ANALISIS, asignado.getEstadoActual().getCodigo());
        assertEquals(operador, ticket.getOperador());
    }

    @Test
    void asignarOperadorSinEstarValidadoFalla() {
        ticketEnEstado(TicketEstadoCodigo.NUEVO);

        assertThrows(InvalidTicketOperationException.class, () -> ticketService.asignarOperador(1L, 10L, operador));
    }

    @Test
    void reasignarANuevoOperadorRegistraHistorial() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.EN_ANALISIS);
        ticket.setOperador(operador);

        Ticket reasignado = ticketService.reasignarOperador(1L, 20L, "Redistribución de carga", operador);

        assertEquals(otroOperador, reasignado.getOperador());
    }

    @Test
    void reasignarAlMismoOperadorFalla() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.EN_ANALISIS);
        ticket.setOperador(operador);

        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.reasignarOperador(1L, 10L, "motivo", operador));
    }

    @Test
    void cerrarTicketRequiereEstarResueltoYSerElOperadorAsignado() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.RESUELTO);
        ticket.setOperador(operador);

        Ticket cerrado = ticketService.cerrarTicket(1L, operador, "Se solucionó");
        assertEquals(TicketEstadoCodigo.CERRADO, cerrado.getEstadoActual().getCodigo());

        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.cerrarTicket(1L, otroOperador, "Otra solución"));
    }

    @Test
    void reabrirTicketCerradoPorElUsuarioDueño() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.CERRADO);
        ticket.setOperador(operador);

        Ticket reabierto = ticketService.reabrirTicketPorUsuario(1L, usuario, "El problema persiste");

        assertEquals(TicketEstadoCodigo.REABIERTO, reabierto.getEstadoActual().getCodigo());
    }

    @Test
    void reabrirTicketNoCerradoFalla() {
        ticketEnEstado(TicketEstadoCodigo.EN_ANALISIS);

        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.reabrirTicketPorUsuario(1L, usuario, "motivo"));
    }
}
