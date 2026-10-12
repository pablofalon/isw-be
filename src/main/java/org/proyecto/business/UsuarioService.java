package org.proyecto.business;

import org.proyecto.data.ComponentePermisoRepository;
import org.proyecto.data.CuentaAccesoRepository;
import org.proyecto.data.OperadorRepository;
import org.proyecto.data.TicketRepository;
import org.proyecto.data.UsuarioTicketRepository;
import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.Operador;
import org.proyecto.entities.Rol;
import org.proyecto.entities.TicketEstadoCodigo;
import org.proyecto.entities.UsuarioTicket;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ABM de usuarios (CU24): alta, modificación, baja lógica, reactivación y blanqueo de contraseña, más
 * el cambio de contraseña propio. Cada usuario del sistema es una persona (UsuarioTicket u Operador
 * según el tipo) vinculada a una CuentaAcceso; ambas se crean y modifican juntas en la misma
 * transacción. No hay borrado físico: tickets, historial y notificaciones referencian a la persona.
 */
@Service
public class UsuarioService {

    private static final Pattern FORMATO_LOGIN = Pattern.compile("[a-z0-9._-]{3,50}");
    private static final Pattern FORMATO_CORREO = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final int LARGO_MINIMO_PASSWORD = 8;

    private final CuentaAccesoRepository cuentaAccesoRepository;
    private final UsuarioTicketRepository usuarioTicketRepository;
    private final OperadorRepository operadorRepository;
    private final ComponentePermisoRepository componenteRepository;
    private final TicketRepository ticketRepository;
    private final PermisoService permisoService;
    private final CryptoManager cryptoManager;

    public UsuarioService(CuentaAccesoRepository cuentaAccesoRepository, UsuarioTicketRepository usuarioTicketRepository,
                          OperadorRepository operadorRepository, ComponentePermisoRepository componenteRepository,
                          TicketRepository ticketRepository, PermisoService permisoService, CryptoManager cryptoManager) {
        this.cuentaAccesoRepository = cuentaAccesoRepository;
        this.usuarioTicketRepository = usuarioTicketRepository;
        this.operadorRepository = operadorRepository;
        this.componenteRepository = componenteRepository;
        this.ticketRepository = ticketRepository;
        this.permisoService = permisoService;
        this.cryptoManager = cryptoManager;
    }

    @Transactional(readOnly = true)
    public List<CuentaAcceso> listar(Rol tipo, Boolean activa) {
        return cuentaAccesoRepository.findAll().stream()
                .filter(cuenta -> tipo == null || cuenta.getRol() == tipo)
                .filter(cuenta -> activa == null || cuenta.isActiva() == activa)
                .sorted(Comparator.comparing(CuentaAcceso::getUsuarioLogin))
                .toList();
    }

    @Transactional(readOnly = true)
    public CuentaAcceso obtener(Long cuentaId) {
        return buscarCuenta(cuentaId);
    }

    /**
     * Crea la persona según el tipo, su cuenta con contraseña temporal y le asigna el grupo de
     * permisos inicial del tipo (USUARIO_FINAL, OPERADOR o ADMINISTRADOR). Los ajustes finos de
     * permisos se hacen después desde el ABM de seguridad.
     */
    @Transactional
    public CuentaAcceso crear(Rol tipo, String usuarioLogin, String passwordTemporal, DatosPersona datos) {
        if (tipo == null) {
            throw new OperacionSeguridadInvalidaException("El tipo de usuario es obligatorio");
        }
        String login = normalizarLogin(usuarioLogin);
        if (cuentaAccesoRepository.findByUsuarioLogin(login).isPresent()) {
            throw new OperacionSeguridadInvalidaException("Ya existe una cuenta con el usuario " + login);
        }
        validarPolitica(passwordTemporal);
        validarDatos(datos);
        String correo = normalizarCorreo(datos.correoElectronico());
        validarCorreoDisponible(correo, null);

        CuentaAcceso cuenta = new CuentaAcceso();
        cuenta.setUsuarioLogin(login);
        cuenta.setPasswordHash(cryptoManager.hash(passwordTemporal));
        cuenta.setRol(tipo);
        cuenta.setDebeCambiarPassword(true);
        if (tipo == Rol.USUARIO) {
            UsuarioTicket usuario = new UsuarioTicket();
            usuario.setFechaRegistro(LocalDateTime.now());
            aplicarDatos(usuario, datos, correo);
            cuenta.setUsuarioTicket(usuarioTicketRepository.save(usuario));
        } else {
            Operador operador = new Operador();
            aplicarDatos(operador, datos, correo);
            cuenta.setOperador(operadorRepository.save(operador));
        }
        componenteRepository.findByCodigo(grupoInicialDe(tipo))
                .ifPresent(grupo -> cuenta.setComponentes(new ArrayList<>(List.of(grupo))));
        return cuentaAccesoRepository.save(cuenta);
    }

    /** Modifica los datos personales. El tipo no cambia: define con qué entidad se vincula la cuenta. */
    @Transactional
    public CuentaAcceso modificar(Long cuentaId, DatosPersona datos) {
        CuentaAcceso cuenta = buscarCuenta(cuentaId);
        validarDatos(datos);
        String correo = normalizarCorreo(datos.correoElectronico());
        validarCorreoDisponible(correo, cuenta);
        if (cuenta.getUsuarioTicket() != null) {
            aplicarDatos(cuenta.getUsuarioTicket(), datos, correo);
        } else if (cuenta.getOperador() != null) {
            aplicarDatos(cuenta.getOperador(), datos, correo);
        }
        return cuenta;
    }

    /**
     * Baja lógica: la cuenta deja de poder ingresar y la persona queda inactiva (un operador inactivo ya
     * no aparece para asignar tickets). Se cierran sus sesiones abiertas.
     */
    @Transactional
    public CuentaAcceso desactivar(Long cuentaId, Long cuentaSolicitanteId) {
        if (cuentaId.equals(cuentaSolicitanteId)) {
            throw new OperacionSeguridadInvalidaException("No podés dar de baja tu propia cuenta");
        }
        CuentaAcceso cuenta = buscarCuenta(cuentaId);
        if (cuenta.getOperador() != null) {
            long abiertos = ticketRepository.findByOperadorIdOrderByFechaCreacionDesc(cuenta.getOperador().getId()).stream()
                    .filter(ticket -> ticket.getEstadoActual().getCodigo() != TicketEstadoCodigo.CERRADO)
                    .count();
            if (abiertos > 0) {
                throw new OperacionSeguridadInvalidaException(
                        "El operador tiene " + abiertos + " ticket(s) asignado(s) sin cerrar: reasignalos antes de darlo de baja");
            }
            cuenta.getOperador().setActivo(false);
        }
        if (cuenta.getUsuarioTicket() != null) {
            cuenta.getUsuarioTicket().setActivo(false);
        }
        cuenta.setActiva(false);
        permisoService.validarQueQuedeUnAdministrador();
        SessionManager.getInstance().invalidarSesionesDe(cuentaId);
        return cuenta;
    }

    /** Reactiva una cuenta dada de baja o bloqueada por intentos fallidos. */
    @Transactional
    public CuentaAcceso activar(Long cuentaId) {
        CuentaAcceso cuenta = buscarCuenta(cuentaId);
        cuenta.setActiva(true);
        cuenta.setIntentosFallidos(0);
        if (cuenta.getOperador() != null) {
            cuenta.getOperador().setActivo(true);
        }
        if (cuenta.getUsuarioTicket() != null) {
            cuenta.getUsuarioTicket().setActivo(true);
        }
        return cuenta;
    }

    /** El administrador asigna una contraseña temporal nueva; el próximo ingreso exige cambiarla. */
    @Transactional
    public CuentaAcceso blanquearPassword(Long cuentaId, String passwordTemporal) {
        CuentaAcceso cuenta = buscarCuenta(cuentaId);
        validarPolitica(passwordTemporal);
        cuenta.setPasswordHash(cryptoManager.hash(passwordTemporal));
        cuenta.setDebeCambiarPassword(true);
        cuenta.setIntentosFallidos(0);
        SessionManager.getInstance().invalidarSesionesDe(cuentaId);
        return cuenta;
    }

    /** Cambio de contraseña del propio usuario; también es la salida obligatoria de una contraseña temporal. */
    @Transactional
    public void cambiarPasswordPropia(Long cuentaId, String passwordActual, String passwordNueva) {
        CuentaAcceso cuenta = buscarCuenta(cuentaId);
        if (passwordActual == null || !cryptoManager.compare(passwordActual, cuenta.getPasswordHash())) {
            throw new OperacionSeguridadInvalidaException("La contraseña actual no es correcta");
        }
        validarPolitica(passwordNueva);
        if (passwordNueva.equals(passwordActual)) {
            throw new OperacionSeguridadInvalidaException("La contraseña nueva debe ser distinta de la actual");
        }
        cuenta.setPasswordHash(cryptoManager.hash(passwordNueva));
        cuenta.setDebeCambiarPassword(false);
        SessionManager.getInstance().marcarPasswordVigente(cuentaId);
    }

    public static void validarPolitica(String password) {
        if (password == null || password.length() < LARGO_MINIMO_PASSWORD
                || !password.chars().anyMatch(Character::isLetter)
                || !password.chars().anyMatch(Character::isDigit)) {
            throw new OperacionSeguridadInvalidaException(
                    "La contraseña debe tener al menos " + LARGO_MINIMO_PASSWORD + " caracteres e incluir letras y números");
        }
    }

    public static String grupoInicialDe(Rol tipo) {
        return switch (tipo) {
            case USUARIO -> "USUARIO_FINAL";
            case OPERADOR -> "OPERADOR";
            case ADMINISTRADOR -> "ADMINISTRADOR";
        };
    }

    private String normalizarLogin(String usuarioLogin) {
        String login = usuarioLogin == null ? "" : usuarioLogin.trim().toLowerCase();
        if (!FORMATO_LOGIN.matcher(login).matches()) {
            throw new OperacionSeguridadInvalidaException(
                    "El usuario debe tener entre 3 y 50 caracteres: letras minúsculas, números, punto, guion o guion bajo");
        }
        return login;
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase();
    }

    private void validarDatos(DatosPersona datos) {
        if (datos == null || estaVacio(datos.nombre()) || estaVacio(datos.apellido())) {
            throw new OperacionSeguridadInvalidaException("Nombre y apellido son obligatorios");
        }
        if (estaVacio(datos.correoElectronico()) || !FORMATO_CORREO.matcher(datos.correoElectronico().trim()).matches()) {
            throw new OperacionSeguridadInvalidaException("El correo electrónico no es válido");
        }
    }

    /** El correo es único entre usuarios y operadores, para que identifique a una sola persona. */
    private void validarCorreoDisponible(String correo, CuentaAcceso propia) {
        Long propioUsuarioId = propia != null && propia.getUsuarioTicket() != null ? propia.getUsuarioTicket().getId() : null;
        Long propioOperadorId = propia != null && propia.getOperador() != null ? propia.getOperador().getId() : null;
        boolean usadoPorUsuario = usuarioTicketRepository.findByCorreoElectronico(correo)
                .filter(usuario -> !usuario.getId().equals(propioUsuarioId))
                .isPresent();
        boolean usadoPorOperador = operadorRepository.findByCorreoElectronico(correo)
                .filter(operador -> !operador.getId().equals(propioOperadorId))
                .isPresent();
        if (usadoPorUsuario || usadoPorOperador) {
            throw new OperacionSeguridadInvalidaException("El correo " + correo + " ya está registrado");
        }
    }

    private void aplicarDatos(UsuarioTicket usuario, DatosPersona datos, String correo) {
        usuario.setNombre(datos.nombre().trim());
        usuario.setApellido(datos.apellido().trim());
        usuario.setCorreoElectronico(correo);
        usuario.setTelefono(textoOpcional(datos.telefono()));
    }

    private void aplicarDatos(Operador operador, DatosPersona datos, String correo) {
        operador.setNombre(datos.nombre().trim());
        operador.setApellido(datos.apellido().trim());
        operador.setCorreoElectronico(correo);
        operador.setArea(textoOpcional(datos.area()));
        operador.setCargo(textoOpcional(datos.cargo()));
    }

    private CuentaAcceso buscarCuenta(Long cuentaId) {
        return cuentaAccesoRepository.findById(cuentaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la cuenta " + cuentaId));
    }

    private static boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static String textoOpcional(String valor) {
        return estaVacio(valor) ? null : valor.trim();
    }
}
