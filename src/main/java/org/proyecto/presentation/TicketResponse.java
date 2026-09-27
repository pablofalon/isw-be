package org.proyecto.presentation;

import org.proyecto.entities.Ticket;
import org.proyecto.entities.TicketEstadoCodigo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TicketResponse {

    private Long id;
    private String titulo;
    private String descripcion;
    private TicketEstadoCodigo estado;
    private String estadoNombre;
    private UsuarioResumen usuario;
    private OperadorResumen operador;
    private CategoriaResponse categoria;
    private PrioridadResponse prioridadSugerida;
    private PrioridadResponse prioridad;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaUltimaActualizacion;
    private LocalDateTime fechaCierre;
    private String motivoCierre;
    private String motivoReapertura;
    private boolean validado;
    private List<AdjuntoResponse> adjuntos = new ArrayList<>();
    private List<HistorialEstadoResponse> historialEstados = new ArrayList<>();
    private List<HistorialAsignacionResponse> historialAsignaciones = new ArrayList<>();

    public TicketResponse() {
    }

    public static TicketResponse from(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        response.id = ticket.getId();
        response.titulo = ticket.getTitulo();
        response.descripcion = ticket.getDescripcion();
        response.estado = ticket.getEstadoActual().getCodigo();
        response.estadoNombre = ticket.getEstadoActual().getNombre();
        response.usuario = UsuarioResumen.from(ticket.getUsuario());
        response.operador = OperadorResumen.from(ticket.getOperador());
        response.categoria = CategoriaResponse.from(ticket.getCategoria());
        response.prioridadSugerida = PrioridadResponse.from(ticket.getPrioridadSugerida());
        response.prioridad = PrioridadResponse.from(ticket.getPrioridad());
        response.fechaCreacion = ticket.getFechaCreacion();
        response.fechaUltimaActualizacion = ticket.getFechaUltimaActualizacion();
        response.fechaCierre = ticket.getFechaCierre();
        response.motivoCierre = ticket.getMotivoCierre();
        response.motivoReapertura = ticket.getMotivoReapertura();
        response.validado = ticket.isValidado();
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public TicketEstadoCodigo getEstado() {
        return estado;
    }

    public void setEstado(TicketEstadoCodigo estado) {
        this.estado = estado;
    }

    public String getEstadoNombre() {
        return estadoNombre;
    }

    public void setEstadoNombre(String estadoNombre) {
        this.estadoNombre = estadoNombre;
    }

    public UsuarioResumen getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioResumen usuario) {
        this.usuario = usuario;
    }

    public OperadorResumen getOperador() {
        return operador;
    }

    public void setOperador(OperadorResumen operador) {
        this.operador = operador;
    }

    public CategoriaResponse getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaResponse categoria) {
        this.categoria = categoria;
    }

    public PrioridadResponse getPrioridadSugerida() {
        return prioridadSugerida;
    }

    public void setPrioridadSugerida(PrioridadResponse prioridadSugerida) {
        this.prioridadSugerida = prioridadSugerida;
    }

    public PrioridadResponse getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(PrioridadResponse prioridad) {
        this.prioridad = prioridad;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaUltimaActualizacion() {
        return fechaUltimaActualizacion;
    }

    public void setFechaUltimaActualizacion(LocalDateTime fechaUltimaActualizacion) {
        this.fechaUltimaActualizacion = fechaUltimaActualizacion;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public String getMotivoCierre() {
        return motivoCierre;
    }

    public void setMotivoCierre(String motivoCierre) {
        this.motivoCierre = motivoCierre;
    }

    public String getMotivoReapertura() {
        return motivoReapertura;
    }

    public void setMotivoReapertura(String motivoReapertura) {
        this.motivoReapertura = motivoReapertura;
    }

    public boolean isValidado() {
        return validado;
    }

    public void setValidado(boolean validado) {
        this.validado = validado;
    }

    public List<AdjuntoResponse> getAdjuntos() {
        return adjuntos;
    }

    public void setAdjuntos(List<AdjuntoResponse> adjuntos) {
        this.adjuntos = adjuntos;
    }

    public List<HistorialEstadoResponse> getHistorialEstados() {
        return historialEstados;
    }

    public void setHistorialEstados(List<HistorialEstadoResponse> historialEstados) {
        this.historialEstados = historialEstados;
    }

    public List<HistorialAsignacionResponse> getHistorialAsignaciones() {
        return historialAsignaciones;
    }

    public void setHistorialAsignaciones(List<HistorialAsignacionResponse> historialAsignaciones) {
        this.historialAsignaciones = historialAsignaciones;
    }
}
