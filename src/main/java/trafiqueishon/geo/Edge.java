<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: rutas
 * Archivo: Edge.java
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
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
package trafiqueishon.geo;

public class Edge {
    public final int id;
    public final Nodo origen;
    public final Nodo destino;
    public final double longitud;
    public final String highway;
    public final String name;
    public final int maxspeed;
    public final boolean oneway;
    public final int lanes;
    public final String ref;

    public double costoBase;
    public volatile double factorTrafico = 1.0;
    public volatile double factorHoraPunta = 1.0;
    public volatile boolean cerrado;

    public Edge(int id, Nodo origen, Nodo destino, double longitud,
                String highway, String name, int maxspeed, boolean oneway,
                int lanes, String ref) {
        this.id = id;
        this.origen = origen;
        this.destino = destino;
        this.longitud = longitud;
        this.highway = highway;
        this.name = name;
        this.maxspeed = maxspeed;
        this.oneway = oneway;
        this.lanes = lanes;
        this.ref = ref;
        recalcularCosto();
    }

    public static double velocidadPorTipo(String highway) {
        if (highway == null) return 30;
        return switch (highway) {
            case "motorway" -> 100;
            case "motorway_link" -> 60;
            case "trunk", "trunk_link" -> 80;
            case "primary", "primary_link" -> 60;
            case "secondary", "secondary_link" -> 50;
            case "tertiary", "tertiary_link" -> 40;
            case "residential" -> 30;
            case "unclassified" -> 40;
            case "service" -> 20;
            case "living_street" -> 15;
            default -> 30;
        };
    }

    public void recalcularCosto() {
        double vel = (maxspeed > 0) ? maxspeed : velocidadPorTipo(highway);
        if (vel <= 0) vel = 30;
        double mps = vel / 3.6;
        costoBase = longitud / mps;
    }

    public double costoActual() {
        return cerrado ? Double.POSITIVE_INFINITY
                : costoBase * factorTrafico * factorHoraPunta;
    }

    public Nodo origen() {
        return origen;
    }

    public Nodo destino() {
        return destino;
    }

    public Nodo getOrigen() {
        return origen;
    }

    public Nodo getDestino() {
        return destino;
    }

    public double longitudMetros() {
        return longitud;
    }

    public double velocidadLibreMps() {
        double velocidad = maxspeed > 0 ? maxspeed : velocidadPorTipo(highway);
        return Math.max(1.0, velocidad / 3.6);
    }

    @Override
    public String toString() {
        return "Edge#" + id + "[" + origen.id + "->" + destino.id
                + " " + highway + " " + String.format("%.0fs", costoBase) + "]";
    }
}