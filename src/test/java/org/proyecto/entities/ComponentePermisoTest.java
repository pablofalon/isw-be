package org.proyecto.entities;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentePermisoTest {

    @Test
    void unPermisoNoAdmiteHijosYSeDevuelveASiMismo() {
        Permiso permiso = new Permiso("TICKET_CREAR", null);

        assertThrows(UnsupportedOperationException.class, () -> permiso.agregar(new Permiso("OTRA", null)));
        assertThrows(UnsupportedOperationException.class, () -> permiso.quitar(new Permiso("OTRA", null)));
        assertTrue(permiso.getHijos().isEmpty());
        assertEquals(Set.of("TICKET_CREAR"), permiso.obtenerPermisos());
    }

    @Test
    void unGrupoAnidadoDevuelveLosPermisosDeTodosLosNiveles() {
        GrupoPermisos base = new GrupoPermisos("BASE", null);
        base.agregar(new Permiso("TICKET_CLASIFICAR", null));
        GrupoPermisos admin = new GrupoPermisos("ADMIN", null);
        admin.agregar(base);
        admin.agregar(new Permiso("SEGURIDAD_ADMINISTRAR", null));

        assertEquals(Set.of("TICKET_CLASIFICAR", "SEGURIDAD_ADMINISTRAR"), admin.obtenerPermisos());
    }

    @Test
    void unPermisoQueLlegaPorDosCaminosNoSeDuplica() {
        Permiso compartido = new Permiso("TICKET_VER_TODOS", null);
        GrupoPermisos a = new GrupoPermisos("A", null);
        a.agregar(compartido);
        GrupoPermisos b = new GrupoPermisos("B", null);
        b.agregar(compartido);
        GrupoPermisos raiz = new GrupoPermisos("RAIZ", null);
        raiz.agregar(a);
        raiz.agregar(b);

        assertEquals(Set.of("TICKET_VER_TODOS"), raiz.obtenerPermisos());
    }

    @Test
    void agregarseASiMismoOFormarUnCicloIndirectoSeRechaza() {
        GrupoPermisos a = new GrupoPermisos("A", null);
        GrupoPermisos b = new GrupoPermisos("B", null);
        a.agregar(b);

        assertThrows(ComposicionInvalidaException.class, () -> a.agregar(a));
        assertThrows(ComposicionInvalidaException.class, () -> b.agregar(a));
        assertTrue(b.getHijos().isEmpty());
    }

    @Test
    void agregarDosVecesElMismoHijoSeRechaza() {
        GrupoPermisos grupo = new GrupoPermisos("F", null);
        grupo.agregar(new Permiso("P", null));

        assertThrows(ComposicionInvalidaException.class, () -> grupo.agregar(new Permiso("P", null)));
    }

    @Test
    void quitarUnHijoLoSacaYQuitarUnoInexistenteSeRechaza() {
        Permiso permiso = new Permiso("P", null);
        GrupoPermisos grupo = new GrupoPermisos("F", null);
        grupo.agregar(permiso);

        grupo.quitar(permiso);

        assertFalse(grupo.esOContiene(permiso));
        assertThrows(ComposicionInvalidaException.class, () -> grupo.quitar(permiso));
    }

    @Test
    void laCuentaUneLosPermisosDeTodosSusComponentes() {
        GrupoPermisos operador = new GrupoPermisos("OPERADOR", null);
        operador.agregar(new Permiso("TICKET_RESOLVER", null));
        CuentaAcceso cuenta = new CuentaAcceso();
        cuenta.setComponentes(List.of(operador, new Permiso("SEGURIDAD_ADMINISTRAR", null)));

        assertEquals(Set.of("TICKET_RESOLVER", "SEGURIDAD_ADMINISTRAR"), cuenta.obtenerPermisos());
    }
}
