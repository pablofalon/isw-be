package org.proyecto.business;

import org.proyecto.data.CuentaAccesoRepository;
import org.proyecto.entities.CuentaAcceso;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Implementa CU23 - Iniciar sesión. Ante credenciales inválidas no distingue entre "usuario
 * inexistente" y "contraseña incorrecta", para no filtrar existencia de cuentas. Solo con la
 * contraseña correcta informa que la cuenta está inactiva (FA2). Al superar el máximo de intentos
 * fallidos la cuenta queda bloqueada por un tiempo configurable (E1) y no se desactiva.
 */
@Service
public class AuthService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final CuentaAccesoRepository cuentaAccesoRepository;
    private final CryptoManager cryptoManager;
    private final int maximoIntentosFallidos;
    private final long minutosBloqueo;

    public AuthService(CuentaAccesoRepository cuentaAccesoRepository, CryptoManager cryptoManager,
                       @Value("${app.login.maximo-intentos:5}") int maximoIntentosFallidos,
                       @Value("${app.login.minutos-bloqueo:15}") long minutosBloqueo) {
        this.cuentaAccesoRepository = cuentaAccesoRepository;
        this.cryptoManager = cryptoManager;
        this.maximoIntentosFallidos = maximoIntentosFallidos;
        this.minutosBloqueo = minutosBloqueo;
    }

    /**
     * Devuelve la cuenta si las credenciales son válidas, vacío si no lo son, y lanza
     * CuentaNoDisponibleException si la cuenta está bloqueada o inactiva.
     */
    public Optional<CuentaAcceso> login(String usuarioLogin, String password) {
        Optional<CuentaAcceso> cuentaOpt = cuentaAccesoRepository.findByUsuarioLogin(usuarioLogin);
        if (cuentaOpt.isEmpty()) {
            return Optional.empty();
        }

        CuentaAcceso cuenta = cuentaOpt.get();
        LocalDateTime ahora = LocalDateTime.now();
        if (cuenta.estaBloqueada(ahora)) {
            throw bloqueada(cuenta);
        }

        if (!cryptoManager.compare(password, cuenta.getPasswordHash())) {
            registrarIntentoFallido(cuenta, ahora);
            return Optional.empty();
        }

        if (!cuenta.isActiva()) {
            throw new CuentaNoDisponibleException("La cuenta está inactiva. Contactá al administrador.");
        }

        cuenta.setIntentosFallidos(0);
        cuenta.setBloqueadaHasta(null);
        cuenta.setFechaUltimoAcceso(ahora);
        cuentaAccesoRepository.save(cuenta);
        return Optional.of(cuenta);
    }

    private void registrarIntentoFallido(CuentaAcceso cuenta, LocalDateTime ahora) {
        cuenta.setIntentosFallidos(cuenta.getIntentosFallidos() + 1);
        boolean alcanzoElMaximo = cuenta.getIntentosFallidos() >= maximoIntentosFallidos;
        if (alcanzoElMaximo) {
            cuenta.setIntentosFallidos(0);
            cuenta.setBloqueadaHasta(ahora.plusMinutes(minutosBloqueo));
        }
        cuentaAccesoRepository.save(cuenta);
        if (alcanzoElMaximo) {
            throw bloqueada(cuenta);
        }
    }

    private CuentaNoDisponibleException bloqueada(CuentaAcceso cuenta) {
        return new CuentaNoDisponibleException("La cuenta está bloqueada por superar los intentos permitidos. "
                + "Podés volver a intentar a partir de las " + cuenta.getBloqueadaHasta().format(HORA) + ".");
    }
}
