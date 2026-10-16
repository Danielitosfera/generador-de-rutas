<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: simulación
 * Archivo: EventoTrafico.java
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

public record EventoTrafico(String nombre, TipoEvento tipo, double lon, double lat) {
    public EventoTrafico {
        if (nombre == null || nombre.isBlank() || tipo == null
                || !Double.isFinite(lon) || !Double.isFinite(lat)
                || lon < -180 || lon > 180 || lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Evento de tráfico inválido.");
        }
    }
}
