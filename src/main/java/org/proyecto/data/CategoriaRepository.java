package org.proyecto.data;

import org.proyecto.entities.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByActivaTrue();

    List<Categoria> findAllByOrderByNombre();

    Optional<Categoria> findByNombreIgnoreCase(String nombre);
}
