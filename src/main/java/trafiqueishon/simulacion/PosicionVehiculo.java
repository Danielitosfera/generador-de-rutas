<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: simulación
 * Archivo: PosicionVehiculo.java
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
package trafiqueishon.simulacion;

import trafiqueishon.ruta.PaletaColores;

=======
package trafiqueishon.simulacion;

>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
public record PosicionVehiculo(double x, double y, TipoVehiculo tipo,
                               int pasajeros, int capacidad, String idRuta, String nombreRuta,
                               String color, Estado estado) {
    public PosicionVehiculo(double x, double y, TipoVehiculo tipo, int pasajeros,
                            int capacidad, String idRuta, String nombreRuta, String color) {
        this(x, y, tipo, pasajeros, capacidad, idRuta, nombreRuta, color, Estado.EN_RUTA);
    }

    public PosicionVehiculo(double x, double y, TipoVehiculo tipo) {
<<<<<<< HEAD
        this(x, y, tipo, 0, tipo == TipoVehiculo.BUS ? 50 : 18, "", "", PaletaColores.COLOR_RUTA_ACTIVA,
=======
        this(x, y, tipo, 0, tipo == TipoVehiculo.BUS ? 50 : 18, "", "", "#00A8FF",
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
                Estado.EN_RUTA);
    }

    public PosicionVehiculo(double x, double y, TipoVehiculo tipo, int pasajeros, int capacidad) {
<<<<<<< HEAD
        this(x, y, tipo, pasajeros, capacidad, "", "", PaletaColores.COLOR_RUTA_ACTIVA, Estado.EN_RUTA);
=======
        this(x, y, tipo, pasajeros, capacidad, "", "", "#00A8FF", Estado.EN_RUTA);
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
    }

    public enum Estado { EN_RUTA, EN_PARADA, DETENIDO, AVERIADO, SIN_RUTA }

    public enum TipoVehiculo {
        BUS, MINIBUS
    }
}
