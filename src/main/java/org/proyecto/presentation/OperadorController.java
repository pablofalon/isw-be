package org.proyecto.presentation;

import org.proyecto.business.Autorizador;
import org.proyecto.business.Permisos;
import org.proyecto.business.SesionActiva;
import org.proyecto.config.AuthenticatedSession;
import org.proyecto.data.OperadorRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operadores")
public class OperadorController {

    private final OperadorRepository operadorRepository;
    private final Autorizador autorizador;

    public OperadorController(OperadorRepository operadorRepository, Autorizador autorizador) {
        this.operadorRepository = operadorRepository;
        this.autorizador = autorizador;
    }

    @GetMapping
    public List<OperadorResumen> listarActivos(@AuthenticatedSession SesionActiva sesion) {
        autorizador.requirePermiso(sesion, Permisos.TICKET_ASIGNAR);
        return operadorRepository.findByActivoTrue().stream().map(OperadorResumen::from).toList();
    }
}
