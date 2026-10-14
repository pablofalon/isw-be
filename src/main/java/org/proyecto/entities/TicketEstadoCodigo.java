package org.proyecto.entities;

import java.util.EnumSet;
import java.util.Set;

/**
 * Códigos de estado del ticket según el diagrama de transición de estados del documento
 * (Recepción de reclamos + Cierre de tickets). TicketEstado es la entidad persistida
 * (catálogo), este enum identifica cada fila de forma estable en el código.
 */
public enum TicketEstadoCodigo {
    NUEVO,
    PENDIENTE_CORRECCION,
    VALIDADO,
    EN_ANALISIS,
    RESUELTO,
    CERRADO,
    REABIERTO;

    /**
     * Máquina de estados del ticket (diagrama unificado del documento de estados): a qué estados se
     * puede pasar desde este. Es el permiteTransicion del diagrama de secuencia de CU07.
     */
    public Set<TicketEstadoCodigo> siguientes() {
        return switch (this) {
            case NUEVO -> EnumSet.of(PENDIENTE_CORRECCION, VALIDADO);
            case PENDIENTE_CORRECCION -> EnumSet.of(NUEVO, VALIDADO);
            case VALIDADO -> EnumSet.of(EN_ANALISIS);
            case EN_ANALISIS, REABIERTO -> EnumSet.of(RESUELTO);
            case RESUELTO -> EnumSet.of(CERRADO);
            case CERRADO -> EnumSet.of(REABIERTO);
        };
    }

    public boolean permiteTransicionA(TicketEstadoCodigo destino) {
        return siguientes().contains(destino);
    }
}
