package org.proyecto.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.proyecto.data.NotificacionRepository;
import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.EstadoNotificacion;
import org.proyecto.entities.Notificacion;
import org.proyecto.entities.Ticket;
import org.proyecto.entities.UsuarioTicket;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock private NotificacionRepository notificacionRepository;
    @Mock private CanalNotificacion canal;

    private NotificacionService notificacionService;
    private Ticket ticket;
    private UsuarioTicket usuario;

    @BeforeEach
    void setUp() {
        notificacionService = new NotificacionService(notificacionRepository, canal, 3);
        ticket = new Ticket();
        ticket.setId(7L);
        usuario = new UsuarioTicket();
        usuario.setId(1L);
        lenient().when(canal.nombre()).thenReturn("IN_APP");
        lenient().when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Notificacion notificacionEnviada() {
        notificacionService.notificarUsuario(ticket, usuario, "Tu ticket #7 fue resuelto");
        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, times(2)).save(guardada.capture());
        return guardada.getValue();
    }

    @Test
    void seRegistraPendienteYQuedaEnviadaAsociadaAlTicket() {
        Notificacion notificacion = notificacionEnviada();

        assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstadoEnvio());
        assertEquals(1, notificacion.getIntentos());
        assertEquals(ticket, notificacion.getTicket());
        assertEquals(usuario, notificacion.getDestinatarioUsuario());
        assertEquals("IN_APP", notificacion.getTipoCanal());
        verify(canal).enviar(notificacion);
    }

    @Test
    void siElCanalFallaQuedaFallida() {
        doThrow(new IllegalStateException("canal caído")).when(canal).enviar(any(Notificacion.class));

        Notificacion notificacion = notificacionEnviada();

        assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstadoEnvio());
        assertEquals(1, notificacion.getIntentos());
    }

    @Test
    void reintentaLasFallidasHastaAgotarLosIntentos() {
        Notificacion conReintentos = new Notificacion();
        conReintentos.setEstadoEnvio(EstadoNotificacion.FALLIDA);
        conReintentos.setIntentos(1);
        Notificacion agotada = new Notificacion();
        agotada.setEstadoEnvio(EstadoNotificacion.FALLIDA);
        agotada.setIntentos(3);
        when(notificacionRepository.findByEstadoEnvio(EstadoNotificacion.FALLIDA)).thenReturn(List.of(conReintentos, agotada));

        notificacionService.reintentarFallidas();

        assertEquals(EstadoNotificacion.ENVIADA, conReintentos.getEstadoEnvio());
        assertEquals(2, conReintentos.getIntentos());
        assertEquals(EstadoNotificacion.FALLIDA, agotada.getEstadoEnvio());
        verify(canal, times(1)).enviar(any(Notificacion.class));
    }

    @Test
    void unaFallaAlPersistirNoInterrumpeLaOperacion() {
        when(notificacionRepository.save(any(Notificacion.class))).thenThrow(new IllegalStateException("base caída"));

        notificacionService.notificarUsuario(ticket, usuario, "mensaje");
    }

    @Test
    void elMensajeSeRecortaAlLargoDeLaColumna() {
        notificacionService.notificarUsuario(ticket, usuario, "x".repeat(1500));

        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, times(2)).save(guardada.capture());
        assertEquals(500, guardada.getValue().getMensaje().length());
    }

    @Test
    void elDestinatarioMarcaSuNotificacionComoLeida() {
        CuentaAcceso cuenta = new CuentaAcceso();
        cuenta.setUsuarioTicket(usuario);
        Notificacion notificacion = new Notificacion();
        notificacion.setDestinatarioUsuario(usuario);
        notificacion.setEstadoEnvio(EstadoNotificacion.ENVIADA);
        when(notificacionRepository.findById(5L)).thenReturn(Optional.of(notificacion));

        Notificacion leida = notificacionService.marcarLeida(5L, cuenta);

        assertEquals(EstadoNotificacion.LEIDA, leida.getEstadoEnvio());
        assertNotNull(leida.getFechaLectura());
    }

    @Test
    void nadieMarcaLeidaUnaNotificacionAjena() {
        UsuarioTicket otro = new UsuarioTicket();
        otro.setId(2L);
        CuentaAcceso cuentaAjena = new CuentaAcceso();
        cuentaAjena.setUsuarioTicket(otro);
        Notificacion notificacion = new Notificacion();
        notificacion.setDestinatarioUsuario(usuario);
        when(notificacionRepository.findById(5L)).thenReturn(Optional.of(notificacion));

        assertThrows(RecursoNoEncontradoException.class, () -> notificacionService.marcarLeida(5L, cuentaAjena));
    }

    @Test
    void cuentaLasNoLeidasDelDestinatario() {
        CuentaAcceso cuenta = new CuentaAcceso();
        cuenta.setUsuarioTicket(usuario);
        Notificacion nueva = mock(Notificacion.class);
        when(nueva.getEstadoEnvio()).thenReturn(EstadoNotificacion.ENVIADA);
        Notificacion leida = mock(Notificacion.class);
        when(leida.getEstadoEnvio()).thenReturn(EstadoNotificacion.LEIDA);
        when(notificacionRepository.findByDestinatarioUsuarioIdOrderByIdDesc(1L)).thenReturn(List.of(nueva, leida));

        assertEquals(1, notificacionService.contarNoLeidas(cuenta));
    }
}
