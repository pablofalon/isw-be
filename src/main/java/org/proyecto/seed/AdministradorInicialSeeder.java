package org.proyecto.seed;

import org.proyecto.business.DatosPersona;
import org.proyecto.business.UsuarioService;
import org.proyecto.data.CuentaAccesoRepository;
import org.proyecto.entities.Rol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Primer arranque sin cuentas: crea un único administrador para poder entrar y dar de alta al resto
 * desde el ABM de usuarios. La contraseña sale de la configuración (SISTEMATICKET_ADMIN_PASSWORD), no
 * del código; si no está definida se genera una al azar y se muestra una sola vez en el log. En
 * ambos casos es temporal: el primer ingreso exige cambiarla.
 */
@Component
@Order(3)
public class AdministradorInicialSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicialSeeder.class);
    private static final String CARACTERES = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITOS = "23456789";

    private final CuentaAccesoRepository cuentaAccesoRepository;
    private final UsuarioService usuarioService;
    private final String usuario;
    private final String passwordConfigurada;

    public AdministradorInicialSeeder(CuentaAccesoRepository cuentaAccesoRepository, UsuarioService usuarioService,
                                      @Value("${app.admin-inicial.usuario:admin}") String usuario,
                                      @Value("${app.admin-inicial.password:}") String passwordConfigurada) {
        this.cuentaAccesoRepository = cuentaAccesoRepository;
        this.usuarioService = usuarioService;
        this.usuario = usuario;
        this.passwordConfigurada = passwordConfigurada;
    }

    @Override
    public void run(String... args) {
        if (cuentaAccesoRepository.count() > 0) {
            return;
        }
        boolean generada = passwordConfigurada == null || passwordConfigurada.isBlank();
        String password = generada ? generarPassword() : passwordConfigurada;
        usuarioService.crear(Rol.ADMINISTRADOR, usuario, password,
                new DatosPersona("Administrador", "Inicial", usuario + "@sistematicket.local", null, "administración", null));
        if (generada) {
            log.warn("Se creó el administrador inicial '{}' con la contraseña temporal: {} "
                    + "(se pide cambiarla en el primer ingreso)", usuario, password);
        } else {
            log.info("Se creó el administrador inicial '{}' con la contraseña configurada "
                    + "(se pide cambiarla en el primer ingreso)", usuario);
        }
    }

    private static String generarPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(CARACTERES.charAt(random.nextInt(CARACTERES.length())));
        }
        for (int i = 0; i < 4; i++) {
            password.append(DIGITOS.charAt(random.nextInt(DIGITOS.length())));
        }
        return password.toString();
    }
}
