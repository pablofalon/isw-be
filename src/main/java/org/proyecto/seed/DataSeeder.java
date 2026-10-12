package org.proyecto.seed;

import org.proyecto.business.Permisos;
import org.proyecto.data.CategoriaRepository;
import org.proyecto.data.ComponentePermisoRepository;
import org.proyecto.data.PrioridadRepository;
import org.proyecto.data.TicketEstadoRepository;
import org.proyecto.entities.Categoria;
import org.proyecto.entities.ComponentePermiso;
import org.proyecto.entities.GrupoPermisos;
import org.proyecto.entities.NivelPrioridad;
import org.proyecto.entities.Permiso;
import org.proyecto.entities.Prioridad;
import org.proyecto.entities.TicketEstado;
import org.proyecto.entities.TicketEstadoCodigo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Carga los catálogos que el sistema necesita para funcionar: estados, categorías, prioridades y el
 * árbol de permisos. Es idempotente (solo crea lo que falta), porque la base persiste entre
 * reinicios. Las cuentas no se crean acá: ver DemoDataSeeder (perfil "demo") y
 * AdministradorInicialSeeder.
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final TicketEstadoRepository ticketEstadoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PrioridadRepository prioridadRepository;
    private final ComponentePermisoRepository componentePermisoRepository;

    public DataSeeder(TicketEstadoRepository ticketEstadoRepository, CategoriaRepository categoriaRepository,
                       PrioridadRepository prioridadRepository, ComponentePermisoRepository componentePermisoRepository) {
        this.ticketEstadoRepository = ticketEstadoRepository;
        this.categoriaRepository = categoriaRepository;
        this.prioridadRepository = prioridadRepository;
        this.componentePermisoRepository = componentePermisoRepository;
    }

    @Override
    public void run(String... args) {
        seedEstados();
        seedCategorias();
        seedPrioridades();
        seedPermisos();
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

    /**
     * Árbol de permisos inicial (Composite). Reproduce exactamente lo que antes permitía cada rol,
     * y reutiliza GESTION_TICKETS dentro de OPERADOR y de ADMINISTRADOR:
     *
     *   USUARIO_FINAL   = {TICKET_CREAR, TICKET_VER_PROPIOS, TICKET_REENVIAR,
     *                      TICKET_VALIDAR_RESOLUCION, TICKET_REABRIR_PROPIO}
     *   GESTION_TICKETS = {TICKET_VER_TODOS, TICKET_CLASIFICAR, TICKET_ASIGNAR, TICKET_RESOLVER}
     *   OPERADOR        = {GESTION_TICKETS, TICKET_REABRIR_OPERATIVO}
     *   ADMINISTRADOR   = {GESTION_TICKETS, SEGURIDAD_ADMINISTRAR, USUARIOS_ADMINISTRAR}
     */
    private void seedPermisos() {

        Permiso crear = permiso(Permisos.TICKET_CREAR, "Registrar tickets y adjuntar archivos");
        Permiso verPropios = permiso(Permisos.TICKET_VER_PROPIOS, "Consultar los tickets propios");
        Permiso verTodos = permiso(Permisos.TICKET_VER_TODOS, "Consultar el detalle de cualquier ticket");
        Permiso reenviar = permiso(Permisos.TICKET_REENVIAR, "Corregir y reenviar un ticket observado");
        Permiso validarResolucion = permiso(Permisos.TICKET_VALIDAR_RESOLUCION, "Confirmar la resolución de un ticket propio");
        Permiso reabrirPropio = permiso(Permisos.TICKET_REABRIR_PROPIO, "Reabrir un ticket propio cerrado");
        Permiso clasificar = permiso(Permisos.TICKET_CLASIFICAR, "Clasificar, priorizar, validar u observar tickets");
        Permiso asignar = permiso(Permisos.TICKET_ASIGNAR, "Asignar y reasignar operadores");
        Permiso resolver = permiso(Permisos.TICKET_RESOLVER, "Resolver y cerrar tickets asignados");
        Permiso reabrirOperativo = permiso(Permisos.TICKET_REABRIR_OPERATIVO, "Reabrir un ticket cerrado que tuvo asignado");
        Permiso administrarSeguridad = permiso(Permisos.SEGURIDAD_ADMINISTRAR, "Administrar grupos, permisos y su asignación");
        Permiso administrarUsuarios = permiso(Permisos.USUARIOS_ADMINISTRAR, "Alta, modificación y baja de usuarios");

        grupo("USUARIO_FINAL", "Usuario que registra y sigue sus reclamos",
                crear, verPropios, reenviar, validarResolucion, reabrirPropio);
        GrupoPermisos gestion = grupo("GESTION_TICKETS", "Tratamiento operativo de tickets",
                verTodos, clasificar, asignar, resolver);
        grupo("OPERADOR", "Operador de soporte", gestion, reabrirOperativo);
        grupo("ADMINISTRADOR", "Administrador del sistema", gestion, administrarSeguridad, administrarUsuarios);
    }

    /** Devuelve el permiso existente o lo crea si falta (la base persiste entre reinicios). */
    private Permiso permiso(String codigo, String descripcion) {
        return (Permiso) componentePermisoRepository.findByCodigo(codigo)
                .orElseGet(() -> componentePermisoRepository.save(new Permiso(codigo, descripcion)));
    }

    /**
     * Crea el grupo solo si no existe. Si ya existe no se toca: el administrador pudo haberlo
     * modificado desde el ABM de seguridad.
     */
    private GrupoPermisos grupo(String codigo, String descripcion, ComponentePermiso... hijos) {
        return (GrupoPermisos) componentePermisoRepository.findByCodigo(codigo).orElseGet(() -> {
            GrupoPermisos grupo = new GrupoPermisos(codigo, descripcion);
            for (ComponentePermiso hijo : hijos) {
                grupo.agregar(hijo);
            }
            return componentePermisoRepository.save(grupo);
        });
    }
}
