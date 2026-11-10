package org.proyecto.business;

/**
 * Catálogo de códigos de permiso que chequea el sistema. Los controllers piden un permiso, nunca
 * un rol; qué cuentas lo tienen lo decide el árbol de grupos (ver DataSeeder y el ABM de
 * seguridad).
 */
public final class Permisos {

    public static final String TICKET_CREAR = "TICKET_CREAR";
    public static final String TICKET_VER_PROPIOS = "TICKET_VER_PROPIOS";
    public static final String TICKET_VER_TODOS = "TICKET_VER_TODOS";
    public static final String TICKET_REENVIAR = "TICKET_REENVIAR";
    public static final String TICKET_VALIDAR_RESOLUCION = "TICKET_VALIDAR_RESOLUCION";
    public static final String TICKET_REABRIR_PROPIO = "TICKET_REABRIR_PROPIO";
    public static final String TICKET_CLASIFICAR = "TICKET_CLASIFICAR";
    public static final String TICKET_ASIGNAR = "TICKET_ASIGNAR";
    public static final String TICKET_RESOLVER = "TICKET_RESOLVER";
    public static final String TICKET_REABRIR_OPERATIVO = "TICKET_REABRIR_OPERATIVO";
    /** Operar sobre tickets asignados a otro operador ("o tener permisos de administración", CU17/CU07). */
    public static final String TICKET_GESTIONAR_TODOS = "TICKET_GESTIONAR_TODOS";
    public static final String SEGURIDAD_ADMINISTRAR = "SEGURIDAD_ADMINISTRAR";
    public static final String USUARIOS_ADMINISTRAR = "USUARIOS_ADMINISTRAR";
    /** Generar reportes, consultar indicadores y exportar (CU20, CU21, CU22). */
    public static final String REPORTES_CONSULTAR = "REPORTES_CONSULTAR";

    private Permisos() {
    }
}
