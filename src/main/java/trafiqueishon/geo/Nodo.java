/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: rutas
 * Archivo: Nodo.java
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
 * @author Beto Hanco (BH)
 */
package trafiqueishon.geo;

import java.util.ArrayList;
import java.util.List;

public class Nodo {
    public final int id;
    public final double x;
    public final double y;
    public final double lon;
    public final double lat;
    public final List<Edge> salientes = new ArrayList<>();
    public final List<Edge> entrantes = new ArrayList<>();

    public Nodo(int id, double x, double y, double lon, double lat) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.lon = lon;
        this.lat = lat;
    }

    @Override
    public String toString() {
        return "Nodo#" + id + "(" + String.format("%.5f,%.5f", lon, lat) + ")";
    }
}