package org.proyecto.seed;

import org.proyecto.business.CryptoManager;
import org.proyecto.business.UsuarioService;
import org.proyecto.data.ComponentePermisoRepository;
import org.proyecto.data.CuentaAccesoRepository;
import org.proyecto.data.OperadorRepository;
import org.proyecto.data.UsuarioTicketRepository;
import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.Operador;
import org.proyecto.entities.Rol;
import org.proyecto.entities.UsuarioTicket;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuentas de demostración (juan.perez, ana.gomez, operador1, operador2, admin). Solo se cargan con
 * el perfil "demo" (ej. mvn spring-boot:run -Dspring-boot.run.profiles=demo), para la presentación
 * y la regresión automática; en un arranque normal las cuentas se dan de alta desde el ABM de
 * usuarios. Sus contraseñas no son temporales para que la demo no pida cambiarlas.
 */
@Component
@Profile("demo")
@Order(2)
public class DemoDataSeeder implements CommandLineRunner {

    private final UsuarioTicketRepository usuarioTicketRepository;
    private final OperadorRepository operadorRepository;
    private final CuentaAccesoRepository cuentaAccesoRepository;
    private final ComponentePermisoRepository componentePermisoRepository;
    private final CryptoManager cryptoManager;

    public DemoDataSeeder(UsuarioTicketRepository usuarioTicketRepository, OperadorRepository operadorRepository,
                          CuentaAccesoRepository cuentaAccesoRepository,
                          ComponentePermisoRepository componentePermisoRepository, CryptoManager cryptoManager) {
        this.usuarioTicketRepository = usuarioTicketRepository;
        this.operadorRepository = operadorRepository;
        this.cuentaAccesoRepository = cuentaAccesoRepository;
        this.componentePermisoRepository = componentePermisoRepository;
        this.cryptoManager = cryptoManager;
    }

    @Override
    public void run(String... args) {
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
        cuenta.setComponentes(new ArrayList<>(List.of(
                componentePermisoRepository.findByCodigo(UsuarioService.grupoInicialDe(rol)).orElseThrow())));
        cuentaAccesoRepository.save(cuenta);
    }
}
