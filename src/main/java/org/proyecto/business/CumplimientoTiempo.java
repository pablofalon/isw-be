package org.proyecto.business;

/**
 * Clasificación del tiempo de respuesta de un ticket frente al objetivo de su prioridad (CU11 paso 4).
 * Se declaran de la más a la menos urgente: es el orden en que se muestran.
 */
public enum CumplimientoTiempo {
    VENCIDO,
    PROXIMO_A_VENCER,
    EN_PLAZO,
    /** Sin prioridad (CU11 FA2) o con una prioridad sin objetivo definido: no entra en el cálculo. */
    SIN_PRIORIDAD
}
