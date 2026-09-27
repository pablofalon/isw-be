package org.proyecto.presentation;

import org.proyecto.config.AuthenticatedSession;
import org.proyecto.data.CategoriaRepository;
import org.proyecto.entities.CuentaAcceso;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;

    public CategoriaController(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping
    public List<CategoriaResponse> listar(@AuthenticatedSession CuentaAcceso cuenta) {
        return categoriaRepository.findByActivaTrue().stream().map(CategoriaResponse::from).toList();
    }
}
