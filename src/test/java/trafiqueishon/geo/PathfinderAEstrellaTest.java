/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: rutas
 * Archivo: PathfinderAEstrellaTest.java
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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PathfinderAEstrellaTest {

    private static final double PASO = 0.001;

    @Test
    void encuentraLaRutaSimpleEnLinea() {
        GrafoVial grafo = grafo(via("a-b-c", punto(0, 0), punto(1, 0), punto(2, 0)));
        Nodo a = nodo(grafo, 0, 0);
        Nodo c = nodo(grafo, 2, 0);

        RutaResult ruta = new Pathfinder(grafo).ruta(a, c);

        assertNotNull(ruta);
        assertEquals(2, ruta.getEdges().size());
        assertEquals(List.of(a, nodo(grafo, 1, 0), c), ruta.getNodos());
    }

    @Test
    void rodeaUnTramoCerradoYNoLoUsa() {
        GrafoVial grafo = grafoConDesvio();
        Nodo a = nodo(grafo, 0, 0);
        Nodo c = nodo(grafo, 2, 0);
        Pathfinder pathfinder = new Pathfinder(grafo);
        RutaResult directa = pathfinder.ruta(a, c);
        assertEquals(2, directa.getEdges().size());
        double tiempoDirecta = directa.tiempoSegundos();

        entre(grafo, nodo(grafo, 1, 0), c).cerrado = true;
        RutaResult desvio = pathfinder.ruta(a, c);

        assertNotNull(desvio);
        assertTrue(desvio.getEdges().stream().noneMatch(e -> e.cerrado));
        assertTrue(desvio.getEdges().size() > directa.getEdges().size());
        assertTrue(desvio.tiempoSegundos() > tiempoDirecta);
    }

    @Test
    void devuelveNullCuandoNoHayCaminoPosible() {
        GrafoVial grafo = grafo(
                via("izquierda", punto(0, 0), punto(1, 0)),
                via("derecha", punto(5, 5), punto(6, 5)));

        RutaResult ruta = new Pathfinder(grafo).ruta(nodo(grafo, 0, 0), nodo(grafo, 6, 5));

        assertNull(ruta);
    }

    @Test
    void devuelveNullSiElUnicoCaminoEstaCerrado() {
        GrafoVial grafo = grafo(via("a-b", punto(0, 0), punto(1, 0)));
        Nodo a = nodo(grafo, 0, 0);
        Nodo b = nodo(grafo, 1, 0);
        entre(grafo, a, b).cerrado = true;

        assertNull(new Pathfinder(grafo).ruta(a, b));
    }

    @Test
    void aEstrellaYDijkstraDanElMismoCostoOptimo() {
        GrafoVial grafo = grafoConDesvio();
        Nodo a = nodo(grafo, 0, 0);
        Nodo c = nodo(grafo, 2, 0);
        entre(grafo, nodo(grafo, 1, 0), c).cerrado = true;

        Pathfinder aEstrella = new Pathfinder(grafo);
        aEstrella.setModo(Pathfinder.Modo.A_ESTRELLA);
        Pathfinder dijkstra = new Pathfinder(grafo);
        dijkstra.setModo(Pathfinder.Modo.DIJKSTRA);

        assertEquals(dijkstra.ruta(a, c).tiempoSegundos(),
                aEstrella.ruta(a, c).tiempoSegundos(), 1e-6);
    }

    @Test
    void origenIgualADestinoDevuelveRutaVacia() {
        GrafoVial grafo = grafo(via("a-b", punto(0, 0), punto(1, 0)));
        Nodo a = nodo(grafo, 0, 0);

        RutaResult ruta = new Pathfinder(grafo).ruta(a, a);

        assertNotNull(ruta);
        assertTrue(ruta.esVacia());
    }

    // ------------------------------------------------------------------ ayudas

    /** A-B-C en línea (directa) y A-D-E-F-C por arriba (desvío más largo). */
    private GrafoVial grafoConDesvio() {
        return grafo(
                via("directa", punto(0, 0), punto(1, 0), punto(2, 0)),
                via("desvio", punto(0, 0), punto(0, 1), punto(1, 1), punto(2, 1), punto(2, 0)));
    }

    private static double[] punto(int x, int y) {
        return new double[]{-70.0 + x * PASO, -15.0 + y * PASO};
    }

    private static Via via(String nombre, double[]... coordenadas) {
        Via via = new Via();
        via.id = nombre;
        via.name = nombre;
        via.highway = "residential";
        via.coords = new ArrayList<>(Arrays.asList(coordenadas));
        return via;
    }

    private static GrafoVial grafo(Via... vias) {
        List<Via> lista = List.of(vias);
        Proyector proyector = new Proyector();
        proyector.preProyectar(lista);
        GrafoVial grafo = new GrafoVial();
        grafo.construir(lista, proyector);
        return grafo;
    }

    private static Nodo nodo(GrafoVial grafo, int x, int y) {
        double[] p = punto(x, y);
        return grafo.nodoMasCercanoLonLat(p[0], p[1]);
    }

    private static Edge entre(GrafoVial grafo, Nodo origen, Nodo destino) {
        return grafo.getEdges().stream()
                .filter(e -> e.origen == origen && e.destino == destino)
                .findFirst().orElseThrow();
    }
}
