package org.proyecto.business;

import org.proyecto.entities.Categoria;
import org.proyecto.entities.NivelPrioridad;

import java.util.List;
import java.util.Map;

/**
 * Configuración inicial de categorías y prioridades (CU25 FA4). La usan el DataSeeder para cargar
 * una base vacía y ParametroService para restaurar los valores por defecto; los parámetros
 * generales llevan su valor por defecto en la propia tabla.
 */
public final class ValoresPorDefecto {

    public record CategoriaInicial(String nombre, String descripcion, String area) {
        public Categoria crear() {
            return new Categoria(nombre, descripcion, area);
        }
    }

    public record PrioridadInicial(NivelPrioridad nivel, String descripcion, int tiempoObjetivoHoras) {
    }

    public static final List<CategoriaInicial> CATEGORIAS = List.of(
            new CategoriaInicial("Problemas de acceso", "Inconvenientes de login o permisos", "soporte técnico"),
            new CategoriaInicial("Reportes", "Errores o solicitudes sobre reportes", "reportes"),
            new CategoriaInicial("Rendimiento", "Lentitud o demoras del sistema", "soporte técnico"),
            new CategoriaInicial("Solicitudes", "Pedidos de nuevas funcionalidades", "desarrollo"),
            new CategoriaInicial("Notificaciones", "Problemas con el envío de notificaciones", "soporte técnico"));

    public static final List<PrioridadInicial> PRIORIDADES = List.of(
            new PrioridadInicial(NivelPrioridad.BAJA, "Impacto bajo, sin urgencia", 72),
            new PrioridadInicial(NivelPrioridad.MEDIA, "Impacto moderado", 48),
            new PrioridadInicial(NivelPrioridad.ALTA, "Impacto alto, requiere atención pronta", 24),
            new PrioridadInicial(NivelPrioridad.CRITICA, "Bloquea la operación del usuario", 4));

    public static final Map<NivelPrioridad, PrioridadInicial> PRIORIDAD_POR_NIVEL = Map.of(
            NivelPrioridad.BAJA, PRIORIDADES.get(0), NivelPrioridad.MEDIA, PRIORIDADES.get(1),
            NivelPrioridad.ALTA, PRIORIDADES.get(2), NivelPrioridad.CRITICA, PRIORIDADES.get(3));

    private ValoresPorDefecto() {
    }
}
