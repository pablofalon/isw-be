package org.proyecto.presentation;

import org.proyecto.entities.Comentario;

import java.time.LocalDateTime;

public class ComentarioResponse {

    private Long id;
    private String contenido;
    private LocalDateTime fechaCreacion;
    private UsuarioResumen usuarioAutor;
    private OperadorResumen operadorAutor;

    public ComentarioResponse() {
    }

    public static ComentarioResponse from(Comentario comentario) {
        ComentarioResponse response = new ComentarioResponse();
        response.id = comentario.getId();
        response.contenido = comentario.getContenido();
        response.fechaCreacion = comentario.getFechaCreacion();
        response.usuarioAutor = UsuarioResumen.from(comentario.getUsuarioAutor());
        response.operadorAutor = OperadorResumen.from(comentario.getOperadorAutor());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public UsuarioResumen getUsuarioAutor() {
        return usuarioAutor;
    }

    public void setUsuarioAutor(UsuarioResumen usuarioAutor) {
        this.usuarioAutor = usuarioAutor;
    }

    public OperadorResumen getOperadorAutor() {
        return operadorAutor;
    }

    public void setOperadorAutor(OperadorResumen operadorAutor) {
        this.operadorAutor = operadorAutor;
    }
}
