<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: simulación
 * Archivo: ServicioMicro.java
 *
 * Autores de la obra:
 *   Piero Gonzales     (PG) - Módulo dominio
 *   Beto Hanco         (BH) - Módulo rutas
 *   Eberth Mamani      (EM) - Módulo simulación
 *   Daniel Taza        (DT) - Módulo datos
 *   Luis Yujra         (LY) - Módulo interfaz
 *
 * Copyright (c) 2026 Piero Gonzales, Beto Hanco, Eberth Mamani,
 * Daniel Taza, Luis Yujra. Todos los derechos reservados.
 *
 * Obra en colaboración. Versión 1.0.
 *
 * @author Eberth Mamani (EM)
 */
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
package trafiqueishon.simulacion;

import trafiqueishon.ruta.Trayecto;

import java.util.Objects;

public record ServicioMicro(
        String id,
        String nombre,
        Trayecto trayecto,
        Trayecto trayectoVuelta,
        int frecuenciaMinutos,
        int unidadesDisponibles,
        int pasajerosEsperando,
        String color) {

    public static final int MAX_UNIDADES = 6_000;

    public ServicioMicro {
        if (id == null || id.isBlank() || nombre == null || nombre.isBlank()
                || frecuenciaMinutos < 1 || frecuenciaMinutos > 180
                || unidadesDisponibles < 1 || unidadesDisponibles > MAX_UNIDADES
                || pasajerosEsperando < 0 || color == null || color.isBlank()) {
            throw new IllegalArgumentException("Configuración de ruta de micro inválida.");
        }
        Objects.requireNonNull(trayecto, "El trayecto de la micro es obligatorio.");
        if (trayecto.edges().isEmpty()) {
            throw new IllegalArgumentException("La ruta de micro debe tener tramos.");
        }
        Objects.requireNonNull(trayectoVuelta, "La ruta de retorno es obligatoria.");
        if (trayectoVuelta.edges().isEmpty()) {
            throw new IllegalArgumentException("La ruta de retorno debe tener tramos.");
        }
        if (pasajerosEsperando > unidadesDisponibles * 18 * 100) {
            throw new IllegalArgumentException("La demanda excede el límite operativo.");
        }
    }
}
