<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: dominio
 * Archivo: Trayecto.java
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
 * @author Piero Gonzales (PG)
 */
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
package trafiqueishon.ruta;

import trafiqueishon.geo.Edge;

import java.util.List;

public record Trayecto(
        String id,
        String nombre,
        String empresa,
        List<PuntoImportante> puntos,
        List<Edge> edges,
        double distanciaMetros,
        double tiempoSegundos,
        String sentido) {

    public Trayecto {
        puntos = List.copyOf(puntos);
        edges = List.copyOf(edges);
        sentido = sentido == null ? "PARTIDA" : sentido;
    }

    public String resumen() {
        double km = distanciaMetros / 1000.0;
        int min = (int) (tiempoSegundos / 60);
        int seg = (int) Math.round(tiempoSegundos % 60);
        return String.format("%.2f km  |  %d min %02d s  |  %d tramos",
                km, min, seg, edges.size());
    }

    public boolean esVacia() { return edges.isEmpty(); }

    public boolean esVacio() { return esVacia(); }
}