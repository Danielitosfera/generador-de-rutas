/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: rutas
 * Archivo: Via.java
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

import java.util.List;

public class Via {
    public String id;
    public String highway;
    public String name;
    public int lanes;
    public boolean oneway;
    public boolean reverseOneway;
    public int maxspeed;
    public String surface;
    public boolean bridge;
    public String ref;

    public float[] projX;
    public float[] projY;

    public double minX, minY, maxX, maxY;

    public transient List<double[]> coords;

    public Via() {}

    public void liberarCoords() {
        if (coords != null) {
            coords.clear();
            coords = null;
        }
    }

    @Override
    public String toString() {
        return "Via{" + id + ", " + highway + ", " + name + "}";
    }
}