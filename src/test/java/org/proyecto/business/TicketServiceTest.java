package org.proyecto.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.proyecto.data.AdjuntoRepository;
import org.proyecto.data.CategoriaRepository;
import org.proyecto.data.ComentarioRepository;
import org.proyecto.data.HistorialAsignacionRepository;
import org.proyecto.data.HistorialEstadoRepository;
import org.proyecto.data.OperadorRepository;
import org.proyecto.data.PrioridadRepository;
import org.proyecto.data.TicketEstadoRepository;
import org.proyecto.data.TicketRepository;
import org.proyecto.entities.Adjunto;
import org.proyecto.entities.Categoria;
import org.proyecto.entities.Comentario;
import org.proyecto.entities.HistorialEstado;
import org.proyecto.entities.NivelPrioridad;
import org.proyecto.entities.Operador;
import org.proyecto.entities.Prioridad;
import org.proyecto.entities.Ticket;
import org.proyecto.entities.TicketEstado;
import org.proyecto.entities.TicketEstadoCodigo;
import org.proyecto.entities.TipoRegistroHistorial;
import org.proyecto.entities.UsuarioTicket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    @Mock private NotificacionService notificacionService;
    @Mock private ComentarioRepository comentarioRepository;
    @Mock private HistorialEstadoRepository historialEstadoRepository;

    private TicketService ticketService;
    private UsuarioTicket usuario;
    private Operador operador;
    private Operador otroOperador;
    private Categoria categoria;
    private Prioridad prioridad;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, adjuntoRepository, historialAsignacionRepository,
                ticketEstadoRepository, categoriaRepository, prioridadRepository, operadorRepository, notificacionService,
                new LimitesAdjuntos(5, 2), comentarioRepository, historialEstadoRepository);

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
        Ticket ticket = ticketService.registrarTicket(usuario, "Titulo", "Descripcion", 100L, null);

        assertEquals(TicketEstadoCodigo.NUEVO, ticket.getEstadoActual().getCodigo());
        assertEquals(1, ticket.getHistorialEstados().size());
    }

    @Test
    void registrarTicketSinTituloFalla() {
        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.registrarTicket(usuario, " ", "Descripcion", 100L, null));
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

        ticketService.clasificar(1L, 100L, operador);
        assertEquals(categoria, ticket.getCategoria());

        ticketService.asignarPrioridad(1L, 200L, operador);
        assertEquals(prioridad, ticket.getPrioridad());

        Ticket validado = ticketService.validarTicket(1L, operador);
        assertEquals(TicketEstadoCodigo.VALIDADO, validado.getEstadoActual().getCodigo());
    }

    @Test
    void asignarPrioridadSinClasificarFalla() {
        ticketEnEstado(TicketEstadoCodigo.NUEVO);

        assertThrows(InvalidTicketOperationException.class, () -> ticketService.asignarPrioridad(1L, 200L, operador));
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

        Ticket cerrado = ticketService.cerrarTicket(1L, operador, "Se solucionó", false);
        assertEquals(TicketEstadoCodigo.CERRADO, cerrado.getEstadoActual().getCodigo());

        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.cerrarTicket(1L, otroOperador, "Otra solución", false));
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

    private HistorialEstado ultimoHistorial(Ticket ticket) {
        List<HistorialEstado> historial = ticket.getHistorialEstados();
        return historial.get(historial.size() - 1);
    }

    @Test
    void registrarTicketSinCategoriaFalla() {
        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.registrarTicket(usuario, "Titulo", "Descripcion", null, null));
    }

    @Test
    void registrarTicketConCategoriaInactivaFalla() {
        categoria.setActiva(false);

        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.registrarTicket(usuario, "Titulo", "Descripcion", 100L, null));
    }

    @Test
    void registrarTicketNotificaSoloAlAreaDeLaCategoria() {
        categoria.setArea("Soporte técnico");
        operador.setArea("soporte técnico");
        otroOperador.setArea("reportes");
        when(operadorRepository.findByActivoTrue()).thenReturn(List.of(operador, otroOperador));

        ticketService.registrarTicket(usuario, "Titulo", "Descripcion", 100L, null);

        verify(notificacionService).notificarOperador(any(Ticket.class), eq(operador), anyString());
        verify(notificacionService, never()).notificarOperador(any(Ticket.class), eq(otroOperador), anyString());
    }

    @Test
    void registrarTicketSinOperadoresDelAreaNotificaATodos() {
        categoria.setArea("desarrollo");
        operador.setArea("soporte técnico");
        when(operadorRepository.findByActivoTrue()).thenReturn(List.of(operador, otroOperador));

        ticketService.registrarTicket(usuario, "Titulo", "Descripcion", 100L, null);

        verify(notificacionService, times(2)).notificarOperador(any(Ticket.class), any(Operador.class), anyString());
    }

    @Test
    void adjuntarArchivoMasGrandeQueElLimiteConfiguradoFalla() {
        ticketEnEstado(TicketEstadoCodigo.NUEVO);

        InvalidTicketOperationException error = assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.adjuntarArchivo(1L, usuario.getId(), "manual.pdf", "application/pdf",
                        6L * 1024 * 1024, new byte[0], null));
        assertEquals("El archivo supera el tamaño máximo permitido (5 MB)", error.getMessage());
    }

    @Test
    void adjuntarArchivoConElMaximoDeAdjuntosAlcanzadoFalla() {
        ticketEnEstado(TicketEstadoCodigo.NUEVO);
        when(adjuntoRepository.countByTicketId(1L)).thenReturn(2L);

        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.adjuntarArchivo(1L, usuario.getId(), "captura.png", "image/png", 1024, new byte[0], null));
    }

    @Test
    void eliminarAdjuntoLoBorraYRegistraElEvento() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.NUEVO);
        Adjunto adjunto = adjuntoDe(ticket, 50L, "captura.png");

        ticketService.eliminarAdjunto(1L, 50L, usuario.getId());

        verify(adjuntoRepository).delete(adjunto);
        assertEquals("Eliminó el archivo captura.png", ultimoHistorial(ticket).getObservacion());
    }

    @Test
    void eliminarAdjuntoDeTicketNoEditableFalla() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.CERRADO);
        adjuntoDe(ticket, 50L, "captura.png");

        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.eliminarAdjunto(1L, 50L, usuario.getId()));
    }

    @Test
    void eliminarAdjuntoDeOtroTicketFalla() {
        ticketEnEstado(TicketEstadoCodigo.NUEVO);
        Ticket otroTicket = new Ticket();
        otroTicket.setId(2L);
        adjuntoDe(otroTicket, 50L, "captura.png");

        assertThrows(RecursoNoEncontradoException.class,
                () -> ticketService.eliminarAdjunto(1L, 50L, usuario.getId()));
    }

    private Adjunto adjuntoDe(Ticket ticket, Long id, String nombre) {
        Adjunto adjunto = new Adjunto();
        adjunto.setId(id);
        adjunto.setTicket(ticket);
        adjunto.setNombreArchivo(nombre);
        lenient().when(adjuntoRepository.findById(id)).thenReturn(Optional.of(adjunto));
        return adjunto;
    }

    @Test
    void adjuntarArchivoRegistraElEventoEnElHistorial() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.NUEVO);
        when(adjuntoRepository.save(any(Adjunto.class))).thenAnswer(inv -> inv.getArgument(0));

        ticketService.adjuntarArchivo(1L, usuario.getId(), "captura.png", "image/png", 1024, new byte[0], null);

        HistorialEstado entrada = ultimoHistorial(ticket);
        assertEquals("Adjuntó el archivo captura.png", entrada.getObservacion());
        assertEquals(usuario, entrada.getActorUsuario());
        assertEquals(TicketEstadoCodigo.NUEVO, entrada.getTicketEstado().getCodigo());
    }

    @Test
    void clasificarYPriorizarQuedanEnElHistorialConElOperador() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.NUEVO);

        ticketService.clasificar(1L, 100L, operador);
        assertEquals("Clasificado en Problemas de acceso", ultimoHistorial(ticket).getObservacion());
        assertEquals(operador, ultimoHistorial(ticket).getActorOperador());

        ticketService.asignarPrioridad(1L, 200L, operador);
        assertEquals("Prioridad asignada: ALTA", ultimoHistorial(ticket).getObservacion());
        assertEquals(TicketEstadoCodigo.NUEVO, ticket.getEstadoActual().getCodigo());
    }

    @Test
    void reclasificarYRepriorizarLoIndicanEnElHistorial() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.NUEVO);
        Categoria anterior = new Categoria("Facturación", "desc");
        ticket.setCategoria(anterior);
        ticket.setPrioridad(new Prioridad(NivelPrioridad.BAJA, "desc", 72));

        ticketService.clasificar(1L, 100L, operador);
        assertEquals("Reclasificado de Facturación a Problemas de acceso", ultimoHistorial(ticket).getObservacion());

        ticketService.asignarPrioridad(1L, 200L, operador);
        assertEquals("Repriorizado de BAJA a ALTA", ultimoHistorial(ticket).getObservacion());
    }

    @Test
    void cerrarTicketDeOtroOperadorConPermisoDeGestionTotal() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.RESUELTO);
        ticket.setOperador(operador);

        Ticket cerrado = ticketService.cerrarTicket(1L, otroOperador, "Cerrado por el administrador", true);

        assertEquals(TicketEstadoCodigo.CERRADO, cerrado.getEstadoActual().getCodigo());
        assertEquals(otroOperador, ultimoHistorial(ticket).getActorOperador());
    }

    @Test
    void validarResolucionRegistraLaConformidadEnElHistorial() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.CERRADO);

        ticketService.validarResolucion(1L, usuario);

        assertEquals(true, ticket.isValidado());
        assertEquals("El usuario confirmó la resolución", ultimoHistorial(ticket).getObservacion());
        assertEquals(usuario, ultimoHistorial(ticket).getActorUsuario());
        assertEquals(TicketEstadoCodigo.CERRADO, ticket.getEstadoActual().getCodigo());
    }

    // ---- CU07 / CU08 / CU09 ----

    private static final Set<String> OPERADOR = Set.of(Permisos.TICKET_RESOLVER, Permisos.TICKET_REABRIR_OPERATIVO);
    private static final Set<String> ADMINISTRADOR = Set.of(Permisos.TICKET_RESOLVER, Permisos.TICKET_GESTIONAR_TODOS);

    private Ticket ticketAsignadoEn(TicketEstadoCodigo codigo) {
        Ticket ticket = ticketEnEstado(codigo);
        ticket.setOperador(operador);
        return ticket;
    }

    /** Para consultas que reciben el ticket ya cargado (sin pasar por el repositorio). */
    private Ticket ticketAsignadoSinRepositorio(TicketEstadoCodigo codigo) {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setUsuario(usuario);
        ticket.setOperador(operador);
        ticket.setEstadoActual(estadoFalso(codigo));
        return ticket;
    }

    @Test
    void maquinaDeEstadosSoloPermiteLasTransicionesDocumentadas() {
        assertTrue(TicketEstadoCodigo.EN_ANALISIS.permiteTransicionA(TicketEstadoCodigo.RESUELTO));
        assertTrue(TicketEstadoCodigo.CERRADO.permiteTransicionA(TicketEstadoCodigo.REABIERTO));
        assertFalse(TicketEstadoCodigo.EN_ANALISIS.permiteTransicionA(TicketEstadoCodigo.CERRADO));
        assertFalse(TicketEstadoCodigo.REABIERTO.permiteTransicionA(TicketEstadoCodigo.EN_ANALISIS));
        assertFalse(TicketEstadoCodigo.RESUELTO.permiteTransicionA(TicketEstadoCodigo.EN_ANALISIS));
    }

    @Test
    void transicionesDisponiblesParaElOperadorAsignado() {
        Ticket ticket = ticketAsignadoSinRepositorio(TicketEstadoCodigo.EN_ANALISIS);
        assertEquals(List.of(TicketEstadoCodigo.RESUELTO), ticketService.transicionesDisponibles(ticket, operador, OPERADOR));

        ticket.setEstadoActual(estadoFalso(TicketEstadoCodigo.CERRADO));
        assertEquals(List.of(TicketEstadoCodigo.REABIERTO), ticketService.transicionesDisponibles(ticket, operador, OPERADOR));
    }

    @Test
    void transicionesDisponiblesVaciasParaOtroOperadorYCompletasParaElAdministrador() {
        Ticket ticket = ticketAsignadoSinRepositorio(TicketEstadoCodigo.RESUELTO);

        assertEquals(List.of(), ticketService.transicionesDisponibles(ticket, otroOperador, OPERADOR));
        assertEquals(List.of(TicketEstadoCodigo.CERRADO), ticketService.transicionesDisponibles(ticket, otroOperador, ADMINISTRADOR));
    }

    @Test
    void transicionesDisponiblesRespetanElPermisoDelDestino() {
        Ticket ticket = ticketAsignadoSinRepositorio(TicketEstadoCodigo.CERRADO);

        assertEquals(List.of(), ticketService.transicionesDisponibles(ticket, operador, Set.of(Permisos.TICKET_RESOLVER)));
    }

    @Test
    void actualizarEstadoAUnaTransicionNoPermitidaInformaLosEstadosPosibles() {
        Ticket ticket = ticketAsignadoEn(TicketEstadoCodigo.EN_ANALISIS);

        InvalidTicketOperationException error = assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.actualizarEstado(1L, TicketEstadoCodigo.CERRADO, "texto", operador, OPERADOR));
        assertTrue(error.getMessage().contains("Estados posibles: RESUELTO"));
        assertEquals(TicketEstadoCodigo.EN_ANALISIS, ticket.getEstadoActual().getCodigo());
    }

    @Test
    void actualizarEstadoExigeLaObservacionDelDestino() {
        ticketAsignadoEn(TicketEstadoCodigo.RESUELTO);

        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.actualizarEstado(1L, TicketEstadoCodigo.CERRADO, " ", operador, OPERADOR));
    }

    @Test
    void actualizarEstadoDelegaEnElCasoDeUsoDelDestino() {
        Ticket ticket = ticketAsignadoEn(TicketEstadoCodigo.EN_ANALISIS);

        ticketService.actualizarEstado(1L, TicketEstadoCodigo.RESUELTO, "Se reinició el servicio", operador, OPERADOR);
        assertEquals(TicketEstadoCodigo.RESUELTO, ticket.getEstadoActual().getCodigo());

        ticketService.actualizarEstado(1L, TicketEstadoCodigo.CERRADO, "Servicio estable", operador, OPERADOR);
        assertEquals(TicketEstadoCodigo.CERRADO, ticket.getEstadoActual().getCodigo());

        ticketService.actualizarEstado(1L, TicketEstadoCodigo.REABIERTO, "Volvió a fallar", operador, OPERADOR);
        assertEquals(TicketEstadoCodigo.REABIERTO, ticket.getEstadoActual().getCodigo());
    }

    @Test
    void resolverRegistraLaResolucionYNotificaAlUsuario() {
        Ticket ticket = ticketAsignadoEn(TicketEstadoCodigo.EN_ANALISIS);

        ticketService.resolver(1L, operador, "Se reinició el servicio", false);

        assertEquals(TicketEstadoCodigo.RESUELTO, ticket.getEstadoActual().getCodigo());
        assertEquals("Se reinició el servicio", ticket.getResolucion());
        assertEquals("Se reinició el servicio", ultimoHistorial(ticket).getMotivoCambio());
        assertEquals(TipoRegistroHistorial.CAMBIO_ESTADO, ultimoHistorial(ticket).getTipo());
        verify(notificacionService).notificarUsuario(eq(ticket), eq(usuario), anyString());
    }

    @Test
    void resolverSinDescripcionFalla() {
        Ticket ticket = ticketAsignadoEn(TicketEstadoCodigo.EN_ANALISIS);

        assertThrows(InvalidTicketOperationException.class, () -> ticketService.resolver(1L, operador, "", false));
        assertEquals(TicketEstadoCodigo.EN_ANALISIS, ticket.getEstadoActual().getCodigo());
    }

    @Test
    void resolverDesdeUnEstadoNoActivoFalla() {
        ticketAsignadoEn(TicketEstadoCodigo.RESUELTO);

        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.resolver(1L, operador, "Otra vez", false));
    }

    @Test
    void resolverTicketDeOtroOperadorSoloConGestionTotal() {
        ticketAsignadoEn(TicketEstadoCodigo.REABIERTO);

        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.resolver(1L, otroOperador, "Solución", false));
        Ticket resuelto = ticketService.resolver(1L, otroOperador, "Solución", true);
        assertEquals(TicketEstadoCodigo.RESUELTO, resuelto.getEstadoActual().getCodigo());
    }

    @Test
    void registrarAccionQuedaEnElHistorialSinCambiarElEstado() {
        Ticket ticket = ticketAsignadoEn(TicketEstadoCodigo.EN_ANALISIS);

        ticketService.registrarAccion(1L, operador, "Se revisaron los logs", null, null, 0, null);

        HistorialEstado entrada = ultimoHistorial(ticket);
        assertEquals(TipoRegistroHistorial.ACCION, entrada.getTipo());
        assertEquals("Se revisaron los logs", entrada.getObservacion());
        assertEquals(operador, entrada.getActorOperador());
        assertEquals(TicketEstadoCodigo.EN_ANALISIS, ticket.getEstadoActual().getCodigo());
        verify(adjuntoRepository, never()).save(any(Adjunto.class));
    }

    @Test
    void registrarAccionConEvidenciaGuardaElAdjuntoDelOperador() {
        Ticket ticket = ticketAsignadoEn(TicketEstadoCodigo.EN_ANALISIS);
        when(adjuntoRepository.save(any(Adjunto.class))).thenAnswer(inv -> inv.getArgument(0));

        ticketService.registrarAccion(1L, operador, "Captura del error", "error.png", "image/png", 2048, new byte[0]);

        ArgumentCaptor<Adjunto> adjunto = ArgumentCaptor.forClass(Adjunto.class);
        verify(adjuntoRepository).save(adjunto.capture());
        assertEquals(operador, adjunto.getValue().getOperadorAutor());
        assertEquals("Evidencia adjunta: error.png", ultimoHistorial(ticket).getMotivoCambio());
    }

    @Test
    void registrarAccionVaciaFalla() {
        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.registrarAccion(1L, operador, "  ", null, null, 0, null));
    }

    @Test
    void registrarAccionSobreTicketCerradoODeOtroOperadorFalla() {
        ticketAsignadoEn(TicketEstadoCodigo.CERRADO);
        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.registrarAccion(1L, operador, "Acción", null, null, 0, null));

        ticketAsignadoEn(TicketEstadoCodigo.EN_ANALISIS);
        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.registrarAccion(1L, otroOperador, "Acción", null, null, 0, null));
    }

    @Test
    void elUsuarioNoPuedeEliminarLaEvidenciaDelOperador() {
        Ticket ticket = ticketEnEstado(TicketEstadoCodigo.EN_ANALISIS);
        Adjunto evidencia = adjuntoDe(ticket, 60L, "error.png");
        evidencia.setOperadorAutor(operador);

        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.eliminarAdjunto(1L, 60L, usuario.getId()));
        verify(adjuntoRepository, never()).delete(any(Adjunto.class));
    }

    // ---- CU15 ----

    @Test
    void elTitularComentaYSeNotificaAlOperadorAsignado() {
        Ticket ticket = ticketAsignadoEn(TicketEstadoCodigo.EN_ANALISIS);

        ticketService.agregarComentario(1L, usuario, null, "¿Hay novedades?");

        ArgumentCaptor<Comentario> comentario = ArgumentCaptor.forClass(Comentario.class);
        verify(comentarioRepository).save(comentario.capture());
        assertEquals("¿Hay novedades?", comentario.getValue().getContenido());
        assertEquals(usuario, comentario.getValue().getUsuarioAutor());
        verify(notificacionService).notificarOperador(eq(ticket), eq(operador), anyString());
        assertEquals(TicketEstadoCodigo.EN_ANALISIS, ticket.getEstadoActual().getCodigo());
    }

    @Test
    void comentarioDelTitularSinOperadorAsignadoNoNotifica() {
        ticketEnEstado(TicketEstadoCodigo.NUEVO);

        ticketService.agregarComentario(1L, usuario, null, "Agrego un dato");

        verify(comentarioRepository).save(any(Comentario.class));
        verify(notificacionService, never()).notificarOperador(any(Ticket.class), any(Operador.class), anyString());
    }

    @Test
    void elOperadorAsignadoComentaYSeNotificaAlTitular() {
        Ticket ticket = ticketAsignadoEn(TicketEstadoCodigo.RESUELTO);

        ticketService.agregarComentario(1L, null, operador, "¿Podés confirmar si funciona?");

        ArgumentCaptor<Comentario> comentario = ArgumentCaptor.forClass(Comentario.class);
        verify(comentarioRepository).save(comentario.capture());
        assertEquals(operador, comentario.getValue().getOperadorAutor());
        verify(notificacionService).notificarUsuario(eq(ticket), eq(usuario), anyString());
    }

    @Test
    void comentarioVacioOLargoFalla() {
        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.agregarComentario(1L, usuario, null, "  "));
        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.agregarComentario(1L, usuario, null, "x".repeat(2001)));
        verify(comentarioRepository, never()).save(any(Comentario.class));
    }

    @Test
    void comentarioSobreTicketCerradoFalla() {
        ticketAsignadoEn(TicketEstadoCodigo.CERRADO);

        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.agregarComentario(1L, usuario, null, "Sigue fallando"));
    }

    @Test
    void comentarioDeOtroOperadorODeOtroUsuarioFalla() {
        ticketAsignadoEn(TicketEstadoCodigo.EN_ANALISIS);
        assertThrows(InvalidTicketOperationException.class,
                () -> ticketService.agregarComentario(1L, null, otroOperador, "Comentario"));

        UsuarioTicket otroUsuario = new UsuarioTicket();
        otroUsuario.setId(2L);
        assertThrows(TicketNotFoundException.class,
                () -> ticketService.agregarComentario(1L, otroUsuario, null, "Comentario"));
    }
    // ---- CU10 / CU11 / CU12 ----

    private Ticket ticketOperativo(long id, NivelPrioridad nivel, Integer horasObjetivo, LocalDateTime creado) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setUsuario(usuario);
        ticket.setOperador(operador);
        ticket.setFechaCreacion(creado);
        ticket.setEstadoActual(estadoFalso(TicketEstadoCodigo.EN_ANALISIS));
        if (nivel != null) {
            ticket.setPrioridad(new Prioridad(nivel, null, horasObjetivo));
        }
        return ticket;
    }

    @Test
    void listarOperativosOrdenaPorPrioridadYAntiguedadConLosSinPrioridadAlFinal() {
        LocalDateTime base = LocalDateTime.of(2026, 10, 1, 9, 0);
        Ticket altaVieja = ticketOperativo(1L, NivelPrioridad.ALTA, 24, base);
        Ticket altaNueva = ticketOperativo(2L, NivelPrioridad.ALTA, 24, base.plusDays(1));
        Ticket critica = ticketOperativo(3L, NivelPrioridad.CRITICA, 4, base.plusDays(2));
        Ticket sinPrioridad = ticketOperativo(4L, null, null, base.minusDays(5));
        when(ticketRepository.buscarOperativos(eq(10L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(altaNueva, sinPrioridad, altaVieja, critica));

        List<Ticket> resultado = ticketService.listarOperativos(operador, VistaTickets.MIS_TICKETS, FiltroTickets.sinFiltros());

        assertEquals(List.of(3L, 1L, 2L, 4L), resultado.stream().map(Ticket::getId).toList());
    }

    @Test
    void listarOperativosEnVistaTodosNoAcotaPorOperadorYTraduceLosFiltros() {
        when(ticketRepository.buscarOperativos(isNull(), eq(TicketEstadoCodigo.CERRADO), eq(100L), eq(200L), eq(20L),
                eq(34L), eq(LocalDateTime.of(2026, 10, 1, 0, 0)), eq(LocalDateTime.of(2026, 10, 16, 0, 0))))
                .thenReturn(List.of());

        List<Ticket> resultado = ticketService.listarOperativos(operador, VistaTickets.TODOS, new FiltroTickets(
                TicketEstadoCodigo.CERRADO, 100L, 200L, 20L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15), "TK-000034"));

        assertTrue(resultado.isEmpty());
        verify(ticketRepository).buscarOperativos(isNull(), eq(TicketEstadoCodigo.CERRADO), eq(100L), eq(200L), eq(20L),
                eq(34L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void listarOperativosConNumeroSinDigitosOPeriodoInvalidoNoConsultaOFalla() {
        assertTrue(ticketService.listarOperativos(operador, VistaTickets.TODOS,
                new FiltroTickets(null, null, null, null, null, null, "abc")).isEmpty());
        verify(ticketRepository, never()).buscarOperativos(any(), any(), any(), any(), any(), any(), any(), any());

        assertThrows(InvalidTicketOperationException.class, () -> ticketService.listarOperativos(operador,
                VistaTickets.TODOS, new FiltroTickets(null, null, null, null,
                        LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 1), null)));
    }

    @Test
    void monitorearTiemposClasificaContraElObjetivoDeLaPrioridadYOrdenaPorUrgencia() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 10, 12, 0);
        Ticket enPlazo = ticketOperativo(1L, NivelPrioridad.MEDIA, 48, ahora.minusHours(10));
        Ticket proximo = ticketOperativo(2L, NivelPrioridad.ALTA, 24, ahora.minusHours(20));
        Ticket vencido = ticketOperativo(3L, NivelPrioridad.CRITICA, 4, ahora.minusHours(6));
        Ticket sinPrioridad = ticketOperativo(4L, null, null, ahora.minusHours(1));
        when(ticketRepository.buscarOperativos(eq(10L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(enPlazo, sinPrioridad, proximo, vencido));
        when(historialEstadoRepository.buscarFechaPrimeraAtencion(anyLong())).thenReturn(Optional.empty());

        MonitoreoTiempos monitoreo = ticketService.monitorearTiempos(operador, VistaTickets.MIS_TICKETS,
                FiltroTiempos.sinFiltros(), ahora);

        assertEquals(List.of(3L, 2L, 1L, 4L), monitoreo.items().stream().map(i -> i.ticket().getId()).toList());
        assertEquals(List.of(CumplimientoTiempo.VENCIDO, CumplimientoTiempo.PROXIMO_A_VENCER,
                CumplimientoTiempo.EN_PLAZO, CumplimientoTiempo.SIN_PRIORIDAD),
                monitoreo.items().stream().map(MonitoreoTiempos.Item::cumplimiento).toList());
        MonitoreoTiempos.Resumen resumen = monitoreo.resumen();
        assertEquals(1, resumen.vencidos());
        assertEquals(1, resumen.proximosAVencer());
        assertEquals(1, resumen.enPlazo());
        assertEquals(1, resumen.sinPrioridad());
        // El promedio no incluye al ticket sin prioridad: (10 + 20 + 6) / 3
        assertEquals(12.0, resumen.promedioHoras());
        assertEquals(150.0, monitoreo.items().get(0).porcentajeConsumido());
    }

    @Test
    void monitorearTiemposUsaLaPrimeraAtencionYNoElMomentoActualSiElTicketYaSeAtendio() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 10, 12, 0);
        LocalDateTime creado = ahora.minusHours(30);
        Ticket atendido = ticketOperativo(1L, NivelPrioridad.ALTA, 24, creado);
        when(ticketRepository.buscarOperativos(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(atendido));
        when(historialEstadoRepository.buscarFechaPrimeraAtencion(1L)).thenReturn(Optional.of(creado.plusHours(2)));

        MonitoreoTiempos.Item item = ticketService.monitorearTiempos(operador, VistaTickets.TODOS,
                FiltroTiempos.sinFiltros(), ahora).items().get(0);

        assertEquals(2.0, item.horasTranscurridas());
        assertTrue(item.atendido());
        assertEquals(CumplimientoTiempo.EN_PLAZO, item.cumplimiento());
    }

    @Test
    void monitorearTiemposFiltraPorCumplimientoYSinTicketsDevuelveResumenEnCero() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 10, 12, 0);
        Ticket enPlazo = ticketOperativo(1L, NivelPrioridad.MEDIA, 48, ahora.minusHours(1));
        Ticket vencido = ticketOperativo(2L, NivelPrioridad.CRITICA, 4, ahora.minusHours(9));
        when(ticketRepository.buscarOperativos(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(enPlazo, vencido));
        when(historialEstadoRepository.buscarFechaPrimeraAtencion(anyLong())).thenReturn(Optional.empty());

        MonitoreoTiempos soloVencidos = ticketService.monitorearTiempos(operador, VistaTickets.TODOS,
                new FiltroTiempos(null, null, CumplimientoTiempo.VENCIDO, null, null), ahora);
        assertEquals(List.of(2L), soloVencidos.items().stream().map(i -> i.ticket().getId()).toList());

        when(ticketRepository.buscarOperativos(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        MonitoreoTiempos vacio = ticketService.monitorearTiempos(operador, VistaTickets.TODOS,
                FiltroTiempos.sinFiltros(), ahora);
        assertTrue(vacio.items().isEmpty());
        assertEquals(0.0, vacio.resumen().promedioHoras());
    }

    @Test
    void obtenerHistorialDelTicketInexistenteFalla() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class,
                () -> ticketService.obtenerHistorial(99L, FiltroHistorial.sinFiltros()));
        verify(historialEstadoRepository, never()).buscarPorTicket(any(), any(), any(), any());
    }

    @Test
    void obtenerHistorialTraduceLosFiltrosYNoEscribeNada() {
        ticketEnEstado(TicketEstadoCodigo.EN_ANALISIS);
        HistorialEstado registro = new HistorialEstado();
        when(historialEstadoRepository.buscarPorTicket(eq(1L), eq(TicketEstadoCodigo.EN_ANALISIS),
                eq(LocalDateTime.of(2026, 10, 1, 0, 0)), eq(LocalDateTime.of(2026, 10, 3, 0, 0))))
                .thenReturn(List.of(registro));

        List<HistorialEstado> historial = ticketService.obtenerHistorial(1L, new FiltroHistorial(
                TicketEstadoCodigo.EN_ANALISIS, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)));

        assertEquals(List.of(registro), historial);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }
}
