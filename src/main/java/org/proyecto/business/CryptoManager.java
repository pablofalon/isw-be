package org.proyecto.business;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class CryptoManager {

    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean compare(String plainPassword, String hash) {
        return BCrypt.checkpw(plainPassword, hash);
    }
}
