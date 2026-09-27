package org.proyecto.presentation;

public class CrearTicketRequest {

    private String titulo;
    private String descripcion;
    private Long idCategoria;
    private Long idPrioridadSugerida;

    public CrearTicketRequest() {
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Long idCategoria) {
        this.idCategoria = idCategoria;
    }

    public Long getIdPrioridadSugerida() {
        return idPrioridadSugerida;
    }

    public void setIdPrioridadSugerida(Long idPrioridadSugerida) {
        this.idPrioridadSugerida = idPrioridadSugerida;
    }
}
