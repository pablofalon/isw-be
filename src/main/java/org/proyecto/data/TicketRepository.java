package org.proyecto.data;

import org.proyecto.entities.Ticket;
import org.proyecto.entities.TicketEstadoCodigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    List<Ticket> findByOperadorIdOrderByFechaCreacionDesc(Long operadorId);

    List<Ticket> findByEstadoActual_CodigoInOrderByFechaCreacionAsc(List<TicketEstadoCodigo> codigos);

    List<Ticket> findByEstadoActual_CodigoAndOperadorIsNullOrderByFechaCreacionAsc(TicketEstadoCodigo codigo);

    /**
     * Tickets operativos (CU10 / CU11): los de estado no final, salvo que se pida un estado puntual
     * (CU10 FA2, que permite incluir uno final). Todos los criterios son opcionales (null = sin filtrar);
     * operadorId acota a los asignados a ese operador y hasta es exclusivo.
     */
    @Query("""
            select t from Ticket t
            where (:operadorId is null or t.operador.id = :operadorId)
              and ((:codigo is null and t.estadoActual.estadoFinal = false) or t.estadoActual.codigo = :codigo)
              and (:idCategoria is null or t.categoria.id = :idCategoria)
              and (:idPrioridad is null or t.prioridad.id = :idPrioridad)
              and (:idOperador is null or t.operador.id = :idOperador)
              and (:numero is null or t.id = :numero)
              and (:desde is null or t.fechaCreacion >= :desde)
              and (:hasta is null or t.fechaCreacion < :hasta)
            """)
    List<Ticket> buscarOperativos(@Param("operadorId") Long operadorId, @Param("codigo") TicketEstadoCodigo codigo,
                                  @Param("idCategoria") Long idCategoria, @Param("idPrioridad") Long idPrioridad,
                                  @Param("idOperador") Long idOperador, @Param("numero") Long numero,
                                  @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Tickets creados en [desde, hasta) que cumplen los filtros opcionales (CU20 / CU21). */
    @Query("""
            select t from Ticket t
            where t.fechaCreacion >= :desde and t.fechaCreacion < :hasta
              and (:idCategoria is null or t.categoria.id = :idCategoria)
              and (:idPrioridad is null or t.prioridad.id = :idPrioridad)
              and (:codigo is null or t.estadoActual.codigo = :codigo)
              and (:idOperador is null or t.operador.id = :idOperador)
            order by t.fechaCreacion asc
            """)
    List<Ticket> buscarPorPeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                                  @Param("idCategoria") Long idCategoria, @Param("idPrioridad") Long idPrioridad,
                                  @Param("codigo") TicketEstadoCodigo codigo, @Param("idOperador") Long idOperador);
}
