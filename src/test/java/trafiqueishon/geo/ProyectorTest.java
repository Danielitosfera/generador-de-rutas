<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: rutas
 * Archivo: ProyectorTest.java
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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProyectorTest {
    @Test
    void usaCoordenadasDeRenderFloatSinPerderExtremos() {
        Via via = new Via();
        via.coords = List.of(new double[]{-70.0, -15.0},
                new double[]{-69.9, -14.9});

        new Proyector().preProyectar(List.of(via));

        assertEquals(0f, via.projX[0]);
        assertEquals(1f, via.projX[1]);
        assertEquals(1f, via.projY[0]);
        assertEquals(0f, via.projY[1]);
    }
}
