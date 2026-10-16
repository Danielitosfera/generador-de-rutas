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

public record PosicionVehiculo(double x, double y, TipoVehiculo tipo,
                               int pasajeros, int capacidad, String idRuta, String nombreRuta,
                               String color, Estado estado) {
    public PosicionVehiculo(double x, double y, TipoVehiculo tipo, int pasajeros,
                            int capacidad, String idRuta, String nombreRuta, String color) {
        this(x, y, tipo, pasajeros, capacidad, idRuta, nombreRuta, color, Estado.EN_RUTA);
    }

    public PosicionVehiculo(double x, double y, TipoVehiculo tipo) {
        this(x, y, tipo, 0, tipo == TipoVehiculo.BUS ? 50 : 18, "", "", PaletaColores.COLOR_RUTA_ACTIVA,
                Estado.EN_RUTA);
    }

    public PosicionVehiculo(double x, double y, TipoVehiculo tipo, int pasajeros, int capacidad) {
        this(x, y, tipo, pasajeros, capacidad, "", "", PaletaColores.COLOR_RUTA_ACTIVA, Estado.EN_RUTA);
    }

    public enum Estado { EN_RUTA, EN_PARADA, DETENIDO, AVERIADO, SIN_RUTA }

    public enum TipoVehiculo {
        BUS, MINIBUS
    }
}
