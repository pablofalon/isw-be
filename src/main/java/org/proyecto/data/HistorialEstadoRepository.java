package org.proyecto.data;

import org.proyecto.entities.HistorialEstado;
import org.proyecto.entities.TicketEstadoCodigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {

    List<HistorialEstado> findByTicketIdOrderByFechaCambioAsc(Long ticketId);

    /** Historial del ticket, el más reciente primero (CU12); los filtros son opcionales y hasta es exclusivo. */
    @Query("""
            select h from HistorialEstado h
            where h.ticket.id = :ticketId
              and (:codigo is null or h.ticketEstado.codigo = :codigo)
              and (:desde is null or h.fechaCambio >= :desde)
              and (:hasta is null or h.fechaCambio < :hasta)
            order by h.fechaCambio desc, h.id desc
            """)
    List<HistorialEstado> buscarPorTicket(@Param("ticketId") Long ticketId,
                                          @Param("codigo") TicketEstadoCodigo codigo,
                                          @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Primera atención (CU11): la primera vez que el ticket pasó a EN_ANALISIS; vacío si todavía no se atendió. */
    @Query("""
            select min(h.fechaCambio) from HistorialEstado h
            where h.ticket.id = :ticketId
              and h.ticketEstado.codigo = org.proyecto.entities.TicketEstadoCodigo.EN_ANALISIS
            """)
    Optional<LocalDateTime> buscarFechaPrimeraAtencion(@Param("ticketId") Long ticketId);
}
