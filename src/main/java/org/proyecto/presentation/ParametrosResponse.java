package org.proyecto.presentation;

import org.proyecto.entities.AuditoriaParametro;
import org.proyecto.entities.Categoria;
import org.proyecto.entities.ParametroSistema;

import java.time.LocalDateTime;
import java.util.List;

/** Vista completa de CU25: los tres grupos de parámetros. */
public record ParametrosResponse(List<CategoriaAdminResponse> categorias, List<PrioridadResponse> prioridades,
                                 List<ParametroGeneralResponse> generales) {

    public record CategoriaAdminResponse(Long id, String nombre, String descripcion, String area, boolean activa) {
        public static CategoriaAdminResponse from(Categoria c) {
            return new CategoriaAdminResponse(c.getId(), c.getNombre(), c.getDescripcion(), c.getArea(), c.isActiva());
        }
    }

    public record ParametroGeneralResponse(String clave, String descripcion, String unidad, int valor,
                                           int valorDefecto, int minimo, int maximo) {
        public static ParametroGeneralResponse from(ParametroSistema p) {
            return new ParametroGeneralResponse(p.getClave(), p.getDescripcion(), p.getUnidad(), p.getValor(),
                    p.getValorDefecto(), p.getMinimo(), p.getMaximo());
        }
    }

    public record AuditoriaResponse(Long id, LocalDateTime fecha, String administrador, String grupo,
                                    String elemento, String accion, String valorAnterior, String valorNuevo) {
        public static AuditoriaResponse from(AuditoriaParametro a) {
            return new AuditoriaResponse(a.getId(), a.getFecha(), a.getAdministrador(), a.getGrupo(),
                    a.getElemento(), a.getAccion(), a.getValorAnterior(), a.getValorNuevo());
        }
    }
}
