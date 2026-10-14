package org.proyecto.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.proyecto.data.CuentaAccesoRepository;
import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.Rol;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CuentaAccesoRepository cuentaAccesoRepository;

    private final CryptoManager cryptoManager = new CryptoManager();

    private AuthService authService;
    private CuentaAcceso cuenta;

    @BeforeEach
    void setUp() {
        authService = new AuthService(cuentaAccesoRepository, cryptoManager, 5, 15);
        cuenta = new CuentaAcceso();
        cuenta.setUsuarioLogin("juan.perez");
        cuenta.setPasswordHash(cryptoManager.hash("usuario123"));
        cuenta.setRol(Rol.USUARIO);
        cuenta.setActiva(true);
        lenient().when(cuentaAccesoRepository.save(any(CuentaAcceso.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void loginConCredencialesCorrectasDevuelveLaCuenta() {
        when(cuentaAccesoRepository.findByUsuarioLogin("juan.perez")).thenReturn(Optional.of(cuenta));

        Optional<CuentaAcceso> resultado = authService.login("juan.perez", "usuario123");

        assertTrue(resultado.isPresent());
        assertTrue(cuenta.getIntentosFallidos() == 0);
    }

    @Test
    void loginConContraseñaIncorrectaIncrementaIntentosFallidos() {
        when(cuentaAccesoRepository.findByUsuarioLogin("juan.perez")).thenReturn(Optional.of(cuenta));

        Optional<CuentaAcceso> resultado = authService.login("juan.perez", "incorrecta");

        assertFalse(resultado.isPresent());
        assertTrue(cuenta.getIntentosFallidos() == 1);
    }

    @Test
    void loginBloqueaTemporalmenteLaCuentaTrasCincoIntentosFallidos() {
        when(cuentaAccesoRepository.findByUsuarioLogin("juan.perez")).thenReturn(Optional.of(cuenta));

        for (int i = 0; i < 4; i++) {
            authService.login("juan.perez", "incorrecta");
        }
        assertThrows(CuentaNoDisponibleException.class, () -> authService.login("juan.perez", "incorrecta"));

        assertTrue(cuenta.isActiva());
        assertTrue(cuenta.estaBloqueada(LocalDateTime.now()));
        assertTrue(cuenta.getBloqueadaHasta().isAfter(LocalDateTime.now().plusMinutes(14)));
    }

    @Test
    void loginConCuentaBloqueadaFallaAunqueLaContraseñaSeaCorrecta() {
        cuenta.setBloqueadaHasta(LocalDateTime.now().plusMinutes(10));
        when(cuentaAccesoRepository.findByUsuarioLogin("juan.perez")).thenReturn(Optional.of(cuenta));

        assertThrows(CuentaNoDisponibleException.class, () -> authService.login("juan.perez", "usuario123"));
    }

    @Test
    void loginConBloqueoVencidoIngresaYLimpiaElBloqueo() {
        cuenta.setBloqueadaHasta(LocalDateTime.now().minusMinutes(1));
        when(cuentaAccesoRepository.findByUsuarioLogin("juan.perez")).thenReturn(Optional.of(cuenta));

        assertTrue(authService.login("juan.perez", "usuario123").isPresent());
        assertNull(cuenta.getBloqueadaHasta());
    }

    @Test
    void loginConUsuarioInexistenteNoRevelaLaDiferencia() {
        when(cuentaAccesoRepository.findByUsuarioLogin("no.existe")).thenReturn(Optional.empty());

        Optional<CuentaAcceso> resultado = authService.login("no.existe", "cualquiera");

        assertFalse(resultado.isPresent());
    }

    @Test
    void loginConCuentaInactivaInformaQueEstaInactiva() {
        cuenta.setActiva(false);
        when(cuentaAccesoRepository.findByUsuarioLogin("juan.perez")).thenReturn(Optional.of(cuenta));

        CuentaNoDisponibleException error = assertThrows(CuentaNoDisponibleException.class,
                () -> authService.login("juan.perez", "usuario123"));
        assertTrue(error.getMessage().contains("inactiva"));
    }

    @Test
    void loginConCuentaInactivaYContraseñaIncorrectaNoRevelaElEstado() {
        cuenta.setActiva(false);
        when(cuentaAccesoRepository.findByUsuarioLogin("juan.perez")).thenReturn(Optional.of(cuenta));

        assertFalse(authService.login("juan.perez", "incorrecta").isPresent());
    }
}
