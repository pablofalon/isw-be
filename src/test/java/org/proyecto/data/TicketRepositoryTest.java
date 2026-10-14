package org.proyecto.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.proyecto.entities.Categoria;
import org.proyecto.entities.HistorialEstado;
import org.proyecto.entities.NivelPrioridad;
import org.proyecto.entities.Operador;
import org.proyecto.entities.Prioridad;
import org.proyecto.entities.Ticket;
import org.proyecto.entities.TicketEstado;
import org.proyecto.entities.TicketEstadoCodigo;
import org.proyecto.entities.UsuarioTicket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Valida contra la base real las consultas con filtros opcionales de CU10, CU11, CU12 y CU20-CU22. */
@DataJpaTest
class TicketRepositoryTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 10, 5, 10, 0);

    @Autowired private TicketRepository ticketRepository;
    @Autowired private HistorialEstadoRepository historialEstadoRepository;
    @Autowired private TestEntityManager entityManager;

    private TicketEstado enAnalisis;
    private TicketEstado cerrado;
    private TicketEstado nuevo;
    private Categoria acceso;
    private Categoria reportes;
    private Prioridad alta;
    private Operador ana;
    private Operador beto;
    private UsuarioTicket usuario;

    @BeforeEach
    void setUp() {
        nuevo = entityManager.persist(new TicketEstado(TicketEstadoCodigo.NUEVO, "Nuevo", null, false, false));
        enAnalisis = entityManager.persist(new TicketEstado(TicketEstadoCodigo.EN_ANALISIS, "En análisis", null, false, false));
        cerrado = entityManager.persist(new TicketEstado(TicketEstadoCodigo.CERRADO, "Cerrado", null, true, true));
        acceso = entityManager.persist(new Categoria("Acceso", null));
        reportes = entityManager.persist(new Categoria("Reportes", null));
        alta = entityManager.persist(new Prioridad(NivelPrioridad.ALTA, null, 24));
        ana = entityManager.persist(operador("ana@empresa.com"));
        beto = entityManager.persist(operador("beto@empresa.com"));
        usuario = new UsuarioTicket();
        usuario.setNombre("Juan");
        usuario.setApellido("Pérez");
        usuario.setCorreoElectronico("juan@empresa.com");
        usuario.setFechaRegistro(BASE);
        usuario = entityManager.persist(usuario);
    }

    private Operador operador(String correo) {
        Operador operador = new Operador();
        operador.setNombre("Op");
        operador.setApellido(correo);
        operador.setCorreoElectronico(correo);
        return operador;
    }

    private Ticket ticket(TicketEstado estado, Categoria categoria, Prioridad prioridad, Operador asignado,
                          LocalDateTime creado) {
        Ticket ticket = new Ticket();
        ticket.setTitulo("T");
        ticket.setDescripcion("D");
        ticket.setUsuario(usuario);
        ticket.setEstadoActual(estado);
        ticket.setCategoria(categoria);
        ticket.setPrioridad(prioridad);
        ticket.setOperador(asignado);
        ticket.setFechaCreacion(creado);
        return entityManager.persist(ticket);
    }

    private HistorialEstado registro(Ticket ticket, TicketEstado estado, LocalDateTime fecha) {
        HistorialEstado historial = new HistorialEstado();
        historial.setTicket(ticket);
        historial.setTicketEstado(estado);
        historial.setFechaCambio(fecha);
        return entityManager.persist(historial);
    }

    private List<Ticket> operativos(Long operadorId, TicketEstadoCodigo codigo, Long categoria, Long prioridad,
                                    Long operador, Long numero, LocalDateTime desde, LocalDateTime hasta) {
        entityManager.flush();
        return ticketRepository.buscarOperativos(operadorId, codigo, categoria, prioridad, operador, numero, desde, hasta);
    }

    @Test
    void buscarOperativosSinFiltrosExcluyeLosEstadosFinalesYConPermisosIncluyeLosAsignadosONo() {
        Ticket abierto = ticket(enAnalisis, acceso, alta, ana, BASE);
        Ticket sinAsignar = ticket(nuevo, null, null, null, BASE);
        ticket(cerrado, acceso, alta, ana, BASE);

        List<Ticket> todos = operativos(null, null, null, null, null, null, null, null);

        assertEquals(List.of(abierto.getId(), sinAsignar.getId()),
                todos.stream().map(Ticket::getId).sorted().toList());
    }

    @Test
    void buscarOperativosAcotaPorOperadorYPorElRestoDeLosFiltros() {
        Ticket deAna = ticket(enAnalisis, acceso, alta, ana, BASE);
        Ticket deBeto = ticket(enAnalisis, reportes, alta, beto, BASE.plusDays(3));

        assertEquals(List.of(deAna.getId()),
                operativos(ana.getId(), null, null, null, null, null, null, null).stream().map(Ticket::getId).toList());
        assertEquals(List.of(deBeto.getId()),
                operativos(null, null, reportes.getId(), null, null, null, null, null).stream().map(Ticket::getId).toList());
        assertEquals(List.of(deBeto.getId()),
                operativos(null, null, null, null, beto.getId(), null, null, null).stream().map(Ticket::getId).toList());
        assertEquals(List.of(deAna.getId()),
                operativos(null, null, null, null, null, deAna.getId(), null, null).stream().map(Ticket::getId).toList());
        assertEquals(List.of(deBeto.getId()),
                operativos(null, null, null, alta.getId(), null, null, BASE.plusDays(1), null).stream().map(Ticket::getId).toList());
        assertEquals(List.of(deAna.getId()),
                operativos(null, null, null, null, null, null, null, BASE.plusDays(1)).stream().map(Ticket::getId).toList());
    }

    @Test
    void buscarOperativosConEstadoFinalExplicitoIncorporaLosCerrados() {
        ticket(enAnalisis, acceso, alta, ana, BASE);
        Ticket cerradoTicket = ticket(cerrado, acceso, alta, ana, BASE);

        List<Ticket> resultado = operativos(null, TicketEstadoCodigo.CERRADO, null, null, null, null, null, null);

        assertEquals(List.of(cerradoTicket.getId()), resultado.stream().map(Ticket::getId).toList());
    }

    @Test
    void buscarPorPeriodoIncluyeTodosLosEstadosYRespetaLosLimitesDelRango() {
        Ticket dentro = ticket(cerrado, acceso, alta, ana, BASE);
        ticket(enAnalisis, acceso, alta, ana, BASE.minusDays(10));
        ticket(enAnalisis, acceso, alta, ana, BASE.plusDays(10));
        entityManager.flush();

        List<Ticket> resultado = ticketRepository.buscarPorPeriodo(BASE.toLocalDate().atStartOfDay(),
                BASE.toLocalDate().plusDays(1).atStartOfDay(), null, null, null, null);
        assertEquals(List.of(dentro.getId()), resultado.stream().map(Ticket::getId).toList());

        assertTrue(ticketRepository.buscarPorPeriodo(BASE.toLocalDate().atStartOfDay(),
                BASE.toLocalDate().plusDays(1).atStartOfDay(), reportes.getId(), null, null, null).isEmpty());
        assertEquals(1, ticketRepository.buscarPorPeriodo(BASE.toLocalDate().atStartOfDay(),
                BASE.toLocalDate().plusDays(1).atStartOfDay(), acceso.getId(), alta.getId(),
                TicketEstadoCodigo.CERRADO, ana.getId()).size());
    }

    @Test
    void elHistorialSeDevuelveDelMasRecienteAlMasAntiguoYSeFiltraPorEstadoYFechas() {
        Ticket ticket = ticket(enAnalisis, acceso, alta, ana, BASE);
        registro(ticket, nuevo, BASE);
        registro(ticket, enAnalisis, BASE.plusDays(1));
        registro(ticket, enAnalisis, BASE.plusDays(2));
        entityManager.flush();

        List<HistorialEstado> todos = historialEstadoRepository.buscarPorTicket(ticket.getId(), null, null, null);
        assertEquals(List.of(BASE.plusDays(2), BASE.plusDays(1), BASE),
                todos.stream().map(HistorialEstado::getFechaCambio).toList());

        assertEquals(2, historialEstadoRepository.buscarPorTicket(ticket.getId(), TicketEstadoCodigo.EN_ANALISIS, null, null).size());
        assertEquals(1, historialEstadoRepository.buscarPorTicket(ticket.getId(), null,
                BASE.plusDays(1), BASE.plusDays(2)).size());
    }

    @Test
    void laPrimeraAtencionEsLaPrimeraVezQueElTicketPasoAEnAnalisis() {
        Ticket atendido = ticket(enAnalisis, acceso, alta, ana, BASE);
        registro(atendido, nuevo, BASE);
        registro(atendido, enAnalisis, BASE.plusHours(5));
        registro(atendido, enAnalisis, BASE.plusHours(9));
        Ticket sinAtender = ticket(nuevo, acceso, alta, null, BASE);
        registro(sinAtender, nuevo, BASE);
        entityManager.flush();

        assertEquals(BASE.plusHours(5), historialEstadoRepository.buscarFechaPrimeraAtencion(atendido.getId()).orElseThrow());
        assertTrue(historialEstadoRepository.buscarFechaPrimeraAtencion(sinAtender.getId()).isEmpty());
    }
}
