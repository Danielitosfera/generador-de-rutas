<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: simulación
 * Archivo: PerfilHoraPunta.java
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

import trafiqueishon.geo.Edge;
import trafiqueishon.geo.GrafoVial;

public final class PerfilHoraPunta {
    private PerfilHoraPunta() {}

    public static double factor(int minutosDelDia) {
        int minuto = Math.floorMod(minutosDelDia, 24 * 60);
        int hora = minuto / 60;
        if (hora >= 7 && hora < 9) return 2.2;
        if (hora >= 12 && hora < 14) return 1.5;
        if (hora >= 17 && hora < 20) return 2.0;
        return 1.0;
    }

    public static double factorDemanda(int minutosDelDia) {
        int hora = Math.floorMod(minutosDelDia, 24 * 60) / 60;
        if (hora >= 6 && hora < 9) return 2.5;
        if (hora >= 12 && hora < 14) return 1.8;
        if (hora >= 17 && hora < 20) return 2.8;
        if (hora >= 22 || hora < 5) return 0.2;
        return 1.0;
    }

    public static void aplicar(GrafoVial grafo, int minutosDelDia) {
        double factor = factor(minutosDelDia);
        for (Edge edge : grafo.getEdges()) edge.factorHoraPunta = factor;
    }
}
