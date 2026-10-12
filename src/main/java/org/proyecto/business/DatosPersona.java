package org.proyecto.business;

/**
 * Datos personales que se cargan en el alta o la modificación de un usuario. teléfono aplica a
 * usuarios de tickets; área y cargo, a operadores y administradores.
 */
public record DatosPersona(String nombre, String apellido, String correoElectronico,
                           String telefono, String area, String cargo) {
}
