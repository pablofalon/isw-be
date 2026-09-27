package org.proyecto.seed;

import org.proyecto.business.CryptoManager;
import org.proyecto.data.CategoriaRepository;
import org.proyecto.data.CuentaAccesoRepository;
import org.proyecto.data.OperadorRepository;
import org.proyecto.data.PrioridadRepository;
import org.proyecto.data.TicketEstadoRepository;
import org.proyecto.data.UsuarioTicketRepository;
import org.proyecto.entities.Categoria;
import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.NivelPrioridad;
import org.proyecto.entities.Operador;
import org.proyecto.entities.Prioridad;
import org.proyecto.entities.Rol;
import org.proyecto.entities.TicketEstado;
import org.proyecto.entities.TicketEstadoCodigo;
import org.proyecto.entities.UsuarioTicket;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Carga el catálogo de estados/categorías/prioridades y cuentas de demostración al arrancar,
 * ya que la base H2 es en memoria (se pierde en cada reinicio) y no hay pantalla de alta de
 * usuarios en el alcance de esta iteración (CU24 queda fuera).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final TicketEstadoRepository ticketEstadoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PrioridadRepository prioridadRepository;
    private final UsuarioTicketRepository usuarioTicketRepository;
    private final OperadorRepository operadorRepository;
    private final CuentaAccesoRepository cuentaAccesoRepository;
    private final CryptoManager cryptoManager;

    public DataSeeder(TicketEstadoRepository ticketEstadoRepository, CategoriaRepository categoriaRepository,
                       PrioridadRepository prioridadRepository, UsuarioTicketRepository usuarioTicketRepository,
                       OperadorRepository operadorRepository, CuentaAccesoRepository cuentaAccesoRepository,
                       CryptoManager cryptoManager) {
        this.ticketEstadoRepository = ticketEstadoRepository;
        this.categoriaRepository = categoriaRepository;
        this.prioridadRepository = prioridadRepository;
        this.usuarioTicketRepository = usuarioTicketRepository;
        this.operadorRepository = operadorRepository;
        this.cuentaAccesoRepository = cuentaAccesoRepository;
        this.cryptoManager = cryptoManager;
    }

    @Override
    public void run(String... args) {
        seedEstados();
        seedCategorias();
        seedPrioridades();
        seedCuentasDemo();
    }

    private void seedEstados() {
        if (ticketEstadoRepository.count() > 0) {
            return;
        }
        ticketEstadoRepository.save(new TicketEstado(TicketEstadoCodigo.NUEVO, "Nuevo",
                "Reclamo recién registrado, pendiente de clasificación", false, false));
        ticketEstadoRepository.save(new TicketEstado(TicketEstadoCodigo.PENDIENTE_CORRECCION, "Pendiente de corrección",
                "El operador detectó información incompleta o inconsistente", false, false));
        ticketEstadoRepository.save(new TicketEstado(TicketEstadoCodigo.VALIDADO, "Validado",
                "Clasificado, priorizado y validado, pendiente de asignación", false, false));
        ticketEstadoRepository.save(new TicketEstado(TicketEstadoCodigo.EN_ANALISIS, "En análisis",
                "Asignado a un operador y en tratamiento", false, false));
        ticketEstadoRepository.save(new TicketEstado(TicketEstadoCodigo.RESUELTO, "Resuelto",
                "El operador aplicó una solución, pendiente de cierre formal", false, false));
        ticketEstadoRepository.save(new TicketEstado(TicketEstadoCodigo.CERRADO, "Cerrado",
                "Cierre formal registrado, disponible para validación del usuario", true, true));
        ticketEstadoRepository.save(new TicketEstado(TicketEstadoCodigo.REABIERTO, "Reabierto",
                "El usuario u operador reabrió el caso", false, false));
    }

    private void seedCategorias() {
        if (categoriaRepository.count() > 0) {
            return;
        }
        categoriaRepository.save(new Categoria("Problemas de acceso", "Inconvenientes de login o permisos"));
        categoriaRepository.save(new Categoria("Reportes", "Errores o solicitudes sobre reportes"));
        categoriaRepository.save(new Categoria("Rendimiento", "Lentitud o demoras del sistema"));
        categoriaRepository.save(new Categoria("Solicitudes", "Pedidos de nuevas funcionalidades"));
        categoriaRepository.save(new Categoria("Notificaciones", "Problemas con el envío de notificaciones"));
    }

    private void seedPrioridades() {
        if (prioridadRepository.count() > 0) {
            return;
        }
        prioridadRepository.save(new Prioridad(NivelPrioridad.BAJA, "Impacto bajo, sin urgencia", 72));
        prioridadRepository.save(new Prioridad(NivelPrioridad.MEDIA, "Impacto moderado", 48));
        prioridadRepository.save(new Prioridad(NivelPrioridad.ALTA, "Impacto alto, requiere atención pronta", 24));
        prioridadRepository.save(new Prioridad(NivelPrioridad.CRITICA, "Bloquea la operación del usuario", 4));
    }

    private void seedCuentasDemo() {
        if (cuentaAccesoRepository.count() > 0) {
            return;
        }

        UsuarioTicket juan = nuevoUsuario("Juan", "Pérez", "juan.perez@empresa.com");
        UsuarioTicket ana = nuevoUsuario("Ana", "Gómez", "ana.gomez@empresa.com");
        crearCuenta("juan.perez", "usuario123", Rol.USUARIO, juan, null);
        crearCuenta("ana.gomez", "usuario123", Rol.USUARIO, ana, null);

        Operador soporte1 = nuevoOperador("Operador", "Soporte 1", "soporte1@empresa.com", "soporte técnico");
        Operador soporte2 = nuevoOperador("Operador", "Soporte 2", "soporte2@empresa.com", "reportes");
        crearCuenta("operador1", "operador123", Rol.OPERADOR, null, soporte1);
        crearCuenta("operador2", "operador123", Rol.OPERADOR, null, soporte2);

        Operador admin = nuevoOperador("Admin", "Sistema", "admin@empresa.com", "administración");
        crearCuenta("admin", "admin123", Rol.ADMINISTRADOR, null, admin);
    }

    private UsuarioTicket nuevoUsuario(String nombre, String apellido, String correo) {
        UsuarioTicket usuario = new UsuarioTicket();
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setCorreoElectronico(correo);
        usuario.setFechaRegistro(LocalDateTime.now());
        return usuarioTicketRepository.save(usuario);
    }

    private Operador nuevoOperador(String nombre, String apellido, String correo, String area) {
        Operador operador = new Operador();
        operador.setNombre(nombre);
        operador.setApellido(apellido);
        operador.setCorreoElectronico(correo);
        operador.setArea(area);
        return operadorRepository.save(operador);
    }

    private void crearCuenta(String login, String password, Rol rol, UsuarioTicket usuario, Operador operador) {
        CuentaAcceso cuenta = new CuentaAcceso();
        cuenta.setUsuarioLogin(login);
        cuenta.setPasswordHash(cryptoManager.hash(password));
        cuenta.setRol(rol);
        cuenta.setUsuarioTicket(usuario);
        cuenta.setOperador(operador);
        cuentaAccesoRepository.save(cuenta);
    }
}
