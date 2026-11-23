package org.proyecto.config;

import jakarta.annotation.PostConstruct;
import org.proyecto.business.SessionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/** Aplica al SessionManager (Singleton) el tiempo de inactividad configurado en application.properties. */
@Configuration
public class SesionConfig {

    private final long minutosInactividad;

    public SesionConfig(@Value("${app.sesion.minutos-inactividad:30}") long minutosInactividad) {
        this.minutosInactividad = minutosInactividad;
    }

    @PostConstruct
    void aplicar() {
        SessionManager.getInstance().setMinutosInactividad(minutosInactividad);
    }
}
