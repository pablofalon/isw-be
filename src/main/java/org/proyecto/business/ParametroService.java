package org.proyecto.business;

import org.proyecto.data.AuditoriaParametroRepository;
import org.proyecto.data.CategoriaRepository;
import org.proyecto.data.ParametroSistemaRepository;
import org.proyecto.data.PrioridadRepository;
import org.proyecto.entities.AuditoriaParametro;
import org.proyecto.entities.Categoria;
import org.proyecto.entities.ParametroSistema;
import org.proyecto.entities.Prioridad;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementa CU25 - Configurar parámetros: ABM de categorías, tiempos objetivo de las prioridades
 * y parámetros generales (umbral de alerta de tiempos, intentos y bloqueo de login). Cada cambio
 * queda auditado con fecha, administrador y valor anterior. Los demás servicios leen los valores
 * vigentes desde acá, de modo que un cambio rige sin reiniciar el sistema.
 */
@Service
public class ParametroService {

    private static final int MAXIMO_HORAS_OBJETIVO = 8760;
    private static final int MAXIMO_REGISTROS_AUDITORIA = 50;

    private final CategoriaRepository categoriaRepository;
    private final PrioridadRepository prioridadRepository;
    private final ParametroSistemaRepository parametroRepository;
    private final AuditoriaParametroRepository auditoriaRepository;

    public ParametroService(CategoriaRepository categoriaRepository, PrioridadRepository prioridadRepository,
                            ParametroSistemaRepository parametroRepository,
                            AuditoriaParametroRepository auditoriaRepository) {
        this.categoriaRepository = categoriaRepository;
        this.prioridadRepository = prioridadRepository;
        this.parametroRepository = parametroRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    // ---- Valores vigentes, consumidos por CU11 y CU23 ----

    /** Fracción (0,1] del tiempo objetivo desde la cual un ticket sin atender está próximo a vencer. */
    public double getUmbralAlerta() {
        return valor(ParametroSistema.UMBRAL_ALERTA_TIEMPOS) / 100.0;
    }

    public int getMaximoIntentosLogin() {
        return valor(ParametroSistema.LOGIN_MAXIMO_INTENTOS);
    }

    public long getMinutosBloqueoLogin() {
        return valor(ParametroSistema.LOGIN_MINUTOS_BLOQUEO);
    }

    private int valor(String clave) {
        return buscarParametro(clave).getValor();
    }

    // ---- Consulta ----

    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAllByOrderByNombre();
    }

    public List<Prioridad> listarPrioridades() {
        return prioridadRepository.findAll().stream()
                .sorted((a, b) -> a.getNivel().compareTo(b.getNivel()))
                .toList();
    }

    public List<ParametroSistema> listarGenerales() {
        return parametroRepository.findAllByOrderByClave();
    }

    public List<AuditoriaParametro> ultimosCambios() {
        return auditoriaRepository.findAllByOrderByFechaDescIdDesc(PageRequest.of(0, MAXIMO_REGISTROS_AUDITORIA));
    }

    // ---- Categorías ----

    @Transactional
    public Categoria crearCategoria(String administrador, String nombre, String descripcion, String area) {
        String nombreLimpio = nombreValido(nombre);
        validarNombreLibre(nombreLimpio, null);
        Categoria categoria = new Categoria(nombreLimpio, limpiar(descripcion), limpiar(area));
        categoria = categoriaRepository.save(categoria);
        auditar(administrador, GrupoParametros.CATEGORIAS, nombreLimpio, "ALTA", null, describir(categoria));
        return categoria;
    }

    @Transactional
    public Categoria modificarCategoria(String administrador, Long id, String nombre, String descripcion, String area) {
        Categoria categoria = buscarCategoria(id);
        String nombreLimpio = nombreValido(nombre);
        validarNombreLibre(nombreLimpio, id);
        String anterior = describir(categoria);
        categoria.setNombre(nombreLimpio);
        categoria.setDescripcion(limpiar(descripcion));
        categoria.setArea(limpiar(area));
        categoria = categoriaRepository.save(categoria);
        auditar(administrador, GrupoParametros.CATEGORIAS, nombreLimpio, "MODIFICACION", anterior, describir(categoria));
        return categoria;
    }

    /** Las categorías no se borran (los tickets las referencian): se activan o desactivan. */
    @Transactional
    public Categoria cambiarEstadoCategoria(String administrador, Long id, boolean activa) {
        Categoria categoria = buscarCategoria(id);
        if (categoria.isActiva() == activa) {
            return categoria;
        }
        String anterior = describir(categoria);
        categoria.setActiva(activa);
        categoria = categoriaRepository.save(categoria);
        auditar(administrador, GrupoParametros.CATEGORIAS, categoria.getNombre(),
                activa ? "ACTIVACION" : "DESACTIVACION", anterior, describir(categoria));
        return categoria;
    }

    // ---- Prioridades ----

    /** Los niveles son fijos (BAJA..CRITICA); solo se edita la descripción y el tiempo objetivo. */
    @Transactional
    public Prioridad modificarPrioridad(String administrador, Long id, String descripcion, Integer horasObjetivo) {
        Prioridad prioridad = prioridadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la prioridad " + id));
        if (horasObjetivo == null || horasObjetivo < 1 || horasObjetivo > MAXIMO_HORAS_OBJETIVO) {
            throw new ParametroInvalidoException(
                    "El tiempo máximo de respuesta debe estar entre 1 y " + MAXIMO_HORAS_OBJETIVO + " horas");
        }
        String anterior = describir(prioridad);
        prioridad.setDescripcion(limpiar(descripcion));
        prioridad.setTiempoObjetivoRespuesta(horasObjetivo);
        prioridad = prioridadRepository.save(prioridad);
        auditar(administrador, GrupoParametros.PRIORIDADES, prioridad.getNivel().name(), "MODIFICACION",
                anterior, describir(prioridad));
        return prioridad;
    }

    // ---- Parámetros generales ----

    @Transactional
    public ParametroSistema modificarGeneral(String administrador, String clave, Integer valor) {
        ParametroSistema parametro = buscarParametro(clave);
        if (valor == null || valor < parametro.getMinimo() || valor > parametro.getMaximo()) {
            throw new ParametroInvalidoException("El valor de \"" + parametro.getDescripcion() + "\" debe estar entre "
                    + parametro.getMinimo() + " y " + parametro.getMaximo() + " " + unidad(parametro));
        }
        int anterior = parametro.getValor();
        parametro.setValor(valor);
        parametro = parametroRepository.save(parametro);
        auditar(administrador, GrupoParametros.GENERALES, clave, "MODIFICACION",
                anterior + " " + unidad(parametro), valor + " " + unidad(parametro));
        return parametro;
    }

    // ---- Restaurar valores por defecto (FA4) ----

    /**
     * Restablece el grupo a su configuración inicial. En categorías reactiva y restaura las
     * iniciales (recreando las que falten) y desactiva las agregadas después, porque no se pueden borrar.
     */
    @Transactional
    public void restaurar(String administrador, GrupoParametros grupo) {
        switch (grupo) {
            case CATEGORIAS -> restaurarCategorias(administrador);
            case PRIORIDADES -> restaurarPrioridades(administrador);
            case GENERALES -> restaurarGenerales(administrador);
        }
    }

    private void restaurarCategorias(String administrador) {
        List<Categoria> existentes = categoriaRepository.findAllByOrderByNombre();
        for (ValoresPorDefecto.CategoriaInicial inicial : ValoresPorDefecto.CATEGORIAS) {
            Optional<Categoria> actual = existentes.stream()
                    .filter(c -> c.getNombre().equalsIgnoreCase(inicial.nombre())).findFirst();
            if (actual.isEmpty()) {
                categoriaRepository.save(inicial.crear());
                continue;
            }
            Categoria categoria = actual.get();
            categoria.setNombre(inicial.nombre());
            categoria.setDescripcion(inicial.descripcion());
            categoria.setArea(inicial.area());
            categoria.setActiva(true);
            categoriaRepository.save(categoria);
        }
        for (Categoria categoria : existentes) {
            boolean esInicial = ValoresPorDefecto.CATEGORIAS.stream()
                    .anyMatch(i -> i.nombre().equalsIgnoreCase(categoria.getNombre()));
            if (!esInicial && categoria.isActiva()) {
                categoria.setActiva(false);
                categoriaRepository.save(categoria);
            }
        }
        auditar(administrador, GrupoParametros.CATEGORIAS, "Todas las categorías", "RESTAURACION", null,
                "Valores por defecto");
    }

    private void restaurarPrioridades(String administrador) {
        for (Prioridad prioridad : prioridadRepository.findAll()) {
            ValoresPorDefecto.PrioridadInicial inicial = ValoresPorDefecto.PRIORIDAD_POR_NIVEL.get(prioridad.getNivel());
            if (inicial != null) {
                prioridad.setDescripcion(inicial.descripcion());
                prioridad.setTiempoObjetivoRespuesta(inicial.tiempoObjetivoHoras());
                prioridadRepository.save(prioridad);
            }
        }
        auditar(administrador, GrupoParametros.PRIORIDADES, "Todas las prioridades", "RESTAURACION", null,
                "Valores por defecto");
    }

    private void restaurarGenerales(String administrador) {
        for (ParametroSistema parametro : parametroRepository.findAll()) {
            parametro.setValor(parametro.getValorDefecto());
            parametroRepository.save(parametro);
        }
        auditar(administrador, GrupoParametros.GENERALES, "Todos los parámetros generales", "RESTAURACION", null,
                "Valores por defecto");
    }

    // ---- Internos ----

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría " + id));
    }

    private ParametroSistema buscarParametro(String clave) {
        return parametroRepository.findById(clave)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el parámetro " + clave));
    }

    private static String nombreValido(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ParametroInvalidoException("El nombre es obligatorio");
        }
        return nombre.trim();
    }

    /** FA6: no puede haber dos categorías con el mismo nombre (sin distinguir mayúsculas). */
    private void validarNombreLibre(String nombre, Long idPropio) {
        categoriaRepository.findByNombreIgnoreCase(nombre)
                .filter(existente -> !existente.getId().equals(idPropio))
                .ifPresent(existente -> {
                    throw new ParametroInvalidoException("Ya existe una categoría con el nombre \"" + nombre
                            + "\". Ingresá uno distinto");
                });
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static String unidad(ParametroSistema parametro) {
        return parametro.getUnidad() == null ? "" : parametro.getUnidad();
    }

    private static String describir(Categoria categoria) {
        return categoria.getNombre() + " | " + nullAGuion(categoria.getDescripcion()) + " | área: "
                + nullAGuion(categoria.getArea()) + " | " + (categoria.isActiva() ? "activa" : "inactiva");
    }

    private static String describir(Prioridad prioridad) {
        return nullAGuion(prioridad.getDescripcion()) + " | " + prioridad.getTiempoObjetivoRespuesta() + " h";
    }

    private static String nullAGuion(String texto) {
        return texto == null ? "-" : texto;
    }

    private void auditar(String administrador, GrupoParametros grupo, String elemento, String accion,
                         String valorAnterior, String valorNuevo) {
        auditoriaRepository.save(new AuditoriaParametro(LocalDateTime.now(), administrador, grupo.name(),
                elemento, accion, valorAnterior, valorNuevo));
    }
}
