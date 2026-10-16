/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: simulación
 * Archivo: DespachadorTest.java
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
import trafiqueishon.geo.GrafoVial;
import trafiqueishon.geo.Proyector;
import trafiqueishon.geo.Via;
import trafiqueishon.ruta.PuntoImportante;
import trafiqueishon.ruta.Trayecto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DespachadorTest {

    private static final int MINUTO = 10 * 60;

    @Test
    void lasDosPrimerasUnidadesSalenJuntasUnaDeCadaTerminal() {
        Despachador despachador = new Despachador(servicio(10, 4, 100));

        Despachador.Salida primera = despachador.siguienteSalida(0, MINUTO).orElseThrow();
        Despachador.Salida segunda = despachador.siguienteSalida(0, MINUTO).orElseThrow();

        assertFalse(primera.desdeTerminalFinal());
        assertTrue(segunda.desdeTerminalFinal());
        assertEquals(0, primera.numeroUnidad());
        assertEquals(1, segunda.numeroUnidad());
        assertTrue(despachador.siguienteSalida(0, MINUTO).isEmpty());
    }

    @Test
    void respetaLaFrecuenciaEntreSalidasYAlternaTerminal() {
        Despachador despachador = new Despachador(servicio(10, 4, 100));
        despachador.siguienteSalida(0, MINUTO);
        despachador.siguienteSalida(0, MINUTO);

        assertTrue(despachador.siguienteSalida(599, MINUTO).isEmpty());
        Optional<Despachador.Salida> tercera = despachador.siguienteSalida(600, MINUTO);
        Optional<Despachador.Salida> cuarta = despachador.siguienteSalida(1200, MINUTO);

        assertFalse(tercera.orElseThrow().desdeTerminalFinal());
        assertTrue(cuarta.orElseThrow().desdeTerminalFinal());
        assertTrue(despachador.terminado());
        assertEquals(4, despachador.unidadesDespachadas());
    }

    @Test
    void noSaleNingunaUnidadMasDeLasDisponibles() {
        Despachador despachador = new Despachador(servicio(1, 2, 0));

        int salidas = 0;
        while (despachador.siguienteSalida(100_000, MINUTO).isPresent()) salidas++;

        assertEquals(2, salidas);
        assertTrue(despachador.terminado());
        assertEquals(0, despachador.minutosHastaProximoDespacho(100_000));
    }

    @Test
    void unaSolaUnidadAvanzaElProximoDespachoDeInmediato() {
        Despachador despachador = new Despachador(servicio(5, 1, 10));

        despachador.siguienteSalida(0, MINUTO);

        assertEquals(300.0, despachador.proximoDespacho(), 1e-9);
        assertTrue(despachador.terminado());
    }

    @Test
    void asignaPasajerosSegunLaDemandaDelServicio() {
        Despachador despachador = new Despachador(servicio(10, 4, 100));

        Despachador.Salida primera = despachador.siguienteSalida(0, MINUTO).orElseThrow();

        assertEquals(18, primera.pasajeros());
    }

    @Test
    void finalizarDetieneLasSalidasPendientes() {
        Despachador despachador = new Despachador(servicio(10, 4, 100));

        despachador.finalizar();

        assertTrue(despachador.terminado());
        assertTrue(despachador.siguienteSalida(1_000_000, MINUTO).isEmpty());
    }

    @Test
    void calculaLosMinutosHastaElProximoDespacho() {
        Despachador despachador = new Despachador(servicio(10, 4, 100));
        despachador.siguienteSalida(0, MINUTO);
        despachador.siguienteSalida(0, MINUTO);

        assertEquals(10, despachador.minutosHastaProximoDespacho(0));
        assertEquals(5, despachador.minutosHastaProximoDespacho(300));
    }

    // ------------------------------------------------------------------ ayudas

    private static ServicioMicro servicio(int frecuenciaMinutos, int unidades, int pasajeros) {
        Via via = new Via();
        via.id = "v";
        via.name = "Calle";
        via.highway = "residential";
        via.coords = new ArrayList<>(List.of(new double[]{-70.0, -15.0},
                new double[]{-69.999, -15.0}));
        List<Via> vias = List.of(via);
        Proyector proyector = new Proyector();
        proyector.preProyectar(vias);
        GrafoVial grafo = new GrafoVial();
        grafo.construir(vias, proyector);
        Trayecto trayecto = new Trayecto("t", "Ruta", "Empresa",
                List.of(new PuntoImportante("p", "P", -70.0, -15.0)),
                new ArrayList<>(grafo.getEdges()), 0, 0, "PARTIDA");
        return new ServicioMicro("s1", "Servicio", trayecto, trayecto, frecuenciaMinutos,
                unidades, pasajeros, "#2E9E44");
    }
}
