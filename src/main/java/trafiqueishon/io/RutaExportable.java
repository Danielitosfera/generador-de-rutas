<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: datos
 * Archivo: RutaExportable.java
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
 * @author Daniel Taza (DT)
 */
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
package trafiqueishon.io;

import trafiqueishon.ruta.Trayecto;

import java.util.Objects;

public record RutaExportable(String nombre, String color, Trayecto trayecto) {
    public RutaExportable {
        if (nombre == null || nombre.isBlank() || color == null || color.isBlank()) {
            throw new IllegalArgumentException("La ruta exportable debe tener nombre y color.");
        }
        Objects.requireNonNull(trayecto, "El trayecto exportable es obligatorio.");
    }
}
