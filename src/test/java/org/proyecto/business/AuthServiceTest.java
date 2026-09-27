package org.proyecto.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.proyecto.data.CuentaAccesoRepository;
import org.proyecto.entities.CuentaAcceso;
import org.proyecto.entities.Rol;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
        authService = new AuthService(cuentaAccesoRepository, cryptoManager);
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
    void loginBloqueaLaCuentaTrasCincoIntentosFallidos() {
        when(cuentaAccesoRepository.findByUsuarioLogin("juan.perez")).thenReturn(Optional.of(cuenta));

        for (int i = 0; i < 5; i++) {
            authService.login("juan.perez", "incorrecta");
        }

        assertFalse(cuenta.isActiva());
    }

    @Test
    void loginConUsuarioInexistenteNoRevelaLaDiferencia() {
        when(cuentaAccesoRepository.findByUsuarioLogin("no.existe")).thenReturn(Optional.empty());

        Optional<CuentaAcceso> resultado = authService.login("no.existe", "cualquiera");

        assertFalse(resultado.isPresent());
    }

    @Test
    void loginConCuentaInactivaFalla() {
        cuenta.setActiva(false);
        when(cuentaAccesoRepository.findByUsuarioLogin("juan.perez")).thenReturn(Optional.of(cuenta));

        Optional<CuentaAcceso> resultado = authService.login("juan.perez", "usuario123");

        assertFalse(resultado.isPresent());
    }
}
