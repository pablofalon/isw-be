package org.proyecto.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoManagerTest {

    private final CryptoManager cryptoManager = new CryptoManager();

    @Test
    void elHashEsDistintoDelTextoPlano() {
        String hash = cryptoManager.hash("miPassword123");
        assertNotEquals("miPassword123", hash);
    }

    @Test
    void compareDevuelveTrueConLaContraseñaCorrecta() {
        String hash = cryptoManager.hash("miPassword123");
        assertTrue(cryptoManager.compare("miPassword123", hash));
    }

    @Test
    void compareDevuelveFalseConLaContraseñaIncorrecta() {
        String hash = cryptoManager.hash("miPassword123");
        assertFalse(cryptoManager.compare("otraPassword", hash));
    }
}
