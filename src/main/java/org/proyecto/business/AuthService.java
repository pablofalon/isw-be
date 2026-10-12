package org.proyecto.business;

import org.proyecto.data.CuentaAccesoRepository;
import org.proyecto.entities.CuentaAcceso;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementa CU23 - Iniciar sesión: valida credenciales, cuenta activa/bloqueo por intentos
 * fallidos, y no distingue en la respuesta entre "usuario inexistente" y "contraseña incorrecta"
 * (mismo criterio que UserService.login original, para no filtrar existencia de cuentas).
 */
@Service
public class AuthService {

    public static final int MAXIMO_INTENTOS_FALLIDOS = 5;

    private final CuentaAccesoRepository cuentaAccesoRepository;
    private final CryptoManager cryptoManager;

    public AuthService(CuentaAccesoRepository cuentaAccesoRepository, CryptoManager cryptoManager) {
        this.cuentaAccesoRepository = cuentaAccesoRepository;
        this.cryptoManager = cryptoManager;
    }

    public Optional<CuentaAcceso> login(String usuarioLogin, String password) {
        Optional<CuentaAcceso> cuentaOpt = cuentaAccesoRepository.findByUsuarioLogin(usuarioLogin);
        if (cuentaOpt.isEmpty()) {
            return Optional.empty();
        }

        CuentaAcceso cuenta = cuentaOpt.get();
        if (!cuenta.isActiva()) {
            return Optional.empty();
        }

        if (!cryptoManager.compare(password, cuenta.getPasswordHash())) {
            registrarIntentoFallido(cuenta);
            return Optional.empty();
        }

        cuenta.setIntentosFallidos(0);
        cuenta.setFechaUltimoAcceso(LocalDateTime.now());
        cuentaAccesoRepository.save(cuenta);
        return Optional.of(cuenta);
    }

    private void registrarIntentoFallido(CuentaAcceso cuenta) {
        cuenta.setIntentosFallidos(cuenta.getIntentosFallidos() + 1);
        if (cuenta.getIntentosFallidos() >= MAXIMO_INTENTOS_FALLIDOS) {
            cuenta.setActiva(false);
        }
        cuentaAccesoRepository.save(cuenta);
    }
}
