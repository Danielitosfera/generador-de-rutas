/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: simulación
 * Archivo: DemandaPasajerosTest.java
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DemandaPasajerosTest {

    private static final int MEDIODIA_NORMAL = 10 * 60;
    private static final int HORA_PUNTA_TARDE = 18 * 60;
    private static final int MADRUGADA = 2 * 60;

    @Test
    void repartePorUnidadYLimitaElAbordajeADieciocho() {
        DemandaPasajeros demanda = new DemandaPasajeros(100, 4);

        assertEquals(25, demanda.demandaPorSalida());
        assertEquals(18, demanda.abordarEnSalida(MEDIODIA_NORMAL));
        assertEquals(7, demanda.pendientes());
    }

    @Test
    void acumulaLoQueNoAborda() {
        DemandaPasajeros demanda = new DemandaPasajeros(100, 4);

        demanda.abordarEnSalida(MEDIODIA_NORMAL);
        demanda.abordarEnSalida(MEDIODIA_NORMAL);

        assertEquals(14, demanda.pendientes());
    }

    @Test
    void aplicaElFactorHorarioDeDemanda() {
        int punta = new DemandaPasajeros(10, 1).abordarEnSalida(HORA_PUNTA_TARDE);
        int madrugada = new DemandaPasajeros(10, 1).abordarEnSalida(MADRUGADA);

        assertEquals(18, punta);
        assertEquals(2, madrugada);
    }

    @Test
    void sinDemandaNoAbordaNadie() {
        assertEquals(0, new DemandaPasajeros(0, 3).abordarEnSalida(HORA_PUNTA_TARDE));
    }

    @Test
    void rechazaParametrosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new DemandaPasajeros(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new DemandaPasajeros(10, 0));
    }

    @Test
    void avisaCuandoLosPendientesDesbordan() {
        DemandaPasajeros demanda = new DemandaPasajeros(Integer.MAX_VALUE, 1);

        demanda.abordarEnSalida(MEDIODIA_NORMAL);

        assertThrows(ArithmeticException.class, () -> demanda.abordarEnSalida(MEDIODIA_NORMAL));
    }
}
