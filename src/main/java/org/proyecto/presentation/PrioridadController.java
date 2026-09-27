package org.proyecto.presentation;

import org.proyecto.config.AuthenticatedSession;
import org.proyecto.data.PrioridadRepository;
import org.proyecto.entities.CuentaAcceso;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prioridades")
public class PrioridadController {

    private final PrioridadRepository prioridadRepository;

    public PrioridadController(PrioridadRepository prioridadRepository) {
        this.prioridadRepository = prioridadRepository;
    }

    @GetMapping
    public List<PrioridadResponse> listar(@AuthenticatedSession CuentaAcceso cuenta) {
        return prioridadRepository.findAll().stream().map(PrioridadResponse::from).toList();
    }
}
