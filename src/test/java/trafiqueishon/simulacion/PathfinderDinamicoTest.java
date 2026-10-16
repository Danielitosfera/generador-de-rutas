/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: simulación
 * Archivo: PathfinderDinamicoTest.java
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
import trafiqueishon.geo.Edge;
import trafiqueishon.geo.GrafoVial;
import trafiqueishon.geo.Nodo;
import trafiqueishon.geo.Pathfinder;
import trafiqueishon.geo.Proyector;
import trafiqueishon.geo.RutaResult;
import trafiqueishon.geo.Via;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PathfinderDinamicoTest {

    private static final double PASO = 0.001;

    @Test
    void laRutaInicialCoincideConElCostoDeAEstrella() {
        GrafoVial grafo = cuadricula(8);
        Nodo inicio = nodo(grafo, 0, 0);
        Nodo meta = nodo(grafo, 7, 7);

        PathfinderDinamico dinamico = new PathfinderDinamico(grafo);

        assertTrue(dinamico.iniciar(inicio, meta));
        assertEquals(new Pathfinder(grafo).ruta(inicio, meta).tiempoSegundos(),
                dinamico.costoCamino(), 1e-6);
        assertEquals(inicio, dinamico.camino().getFirst());
        assertEquals(meta, dinamico.camino().getLast());
    }

    @Test
    void bloqueaUnaViaYReencuentraUnaRutaQueLaEvita() {
        GrafoVial grafo = cuadricula(8);
        Nodo inicio = nodo(grafo, 0, 0);
        Nodo meta = nodo(grafo, 7, 7);
        PathfinderDinamico dinamico = new PathfinderDinamico(grafo);
        dinamico.iniciar(inicio, meta);
        double costoAntes = dinamico.costoCamino();
        Edge bloqueada = dinamico.ruta().getEdges().get(3);

        bloqueada.cerrado = true;
        dinamico.notificarCambio(bloqueada);

        assertTrue(dinamico.hayCamino());
        RutaResult nueva = dinamico.ruta();
        assertFalse(nueva.getEdges().contains(bloqueada));
        assertTrue(nueva.getEdges().stream().noneMatch(e -> e.cerrado));
        assertEquals(new Pathfinder(grafo).ruta(inicio, meta).tiempoSegundos(),
                dinamico.costoCamino(), 1e-6);
        assertTrue(dinamico.costoCamino() >= costoAntes);
    }

    @Test
    void recalculaSinReiniciarExpandiendoMenosNodosQueElCalculoInicial() {
        GrafoVial grafo = cuadricula(20);
        Nodo inicio = nodo(grafo, 0, 0);
        Nodo meta = nodo(grafo, 19, 19);
        PathfinderDinamico dinamico = new PathfinderDinamico(grafo);
        dinamico.iniciar(inicio, meta);
        int expansionesIniciales = dinamico.expansionesUltimoCalculo();
        Edge bloqueada = dinamico.ruta().getEdges().getFirst();

        bloqueada.cerrado = true;
        dinamico.notificarCambio(bloqueada);

        assertTrue(dinamico.hayCamino());
        assertTrue(dinamico.expansionesUltimoCalculo() < expansionesIniciales,
                "recálculo=" + dinamico.expansionesUltimoCalculo()
                        + " inicial=" + expansionesIniciales);
    }

    @Test
    void sinCambiosNoExpandeNingunNodo() {
        GrafoVial grafo = cuadricula(6);
        PathfinderDinamico dinamico = new PathfinderDinamico(grafo);
        dinamico.iniciar(nodo(grafo, 0, 0), nodo(grafo, 5, 5));

        assertEquals(0, dinamico.sincronizarCambios());
        assertEquals(0, dinamico.expansionesUltimoCalculo());
    }

    @Test
    void sinCaminoPosibleLoReportaYSeRecuperaAlReabrir() {
        GrafoVial grafo = cuadricula(2);
        Nodo inicio = nodo(grafo, 0, 0);
        Nodo meta = nodo(grafo, 1, 1);
        PathfinderDinamico dinamico = new PathfinderDinamico(grafo);
        dinamico.iniciar(inicio, meta);
        double costoOriginal = dinamico.costoCamino();
        List<Edge> salidas = new ArrayList<>(inicio.salientes);

        salidas.forEach(e -> e.cerrado = true);
        dinamico.notificarCambios(salidas);

        assertFalse(dinamico.hayCamino());
        assertNull(dinamico.ruta());
        assertTrue(dinamico.camino().isEmpty());

        salidas.forEach(e -> e.cerrado = false);
        dinamico.notificarCambios(salidas);

        assertTrue(dinamico.hayCamino());
        assertEquals(costoOriginal, dinamico.costoCamino(), 1e-6);
    }

    @Test
    void siguePrimerosPasosYRecalculaDesdeLaNuevaPosicion() {
        GrafoVial grafo = cuadricula(10);
        Nodo meta = nodo(grafo, 9, 9);
        PathfinderDinamico dinamico = new PathfinderDinamico(grafo);
        dinamico.iniciar(nodo(grafo, 0, 0), meta);
        List<Nodo> camino = dinamico.camino();

        dinamico.mover(camino.get(4));
        Edge adelante = dinamico.ruta().getEdges().get(1);
        adelante.cerrado = true;
        dinamico.notificarCambio(adelante);

        assertEquals(camino.get(4), dinamico.camino().getFirst());
        assertFalse(dinamico.ruta().getEdges().contains(adelante));
        assertEquals(new Pathfinder(grafo).ruta(camino.get(4), meta).tiempoSegundos(),
                dinamico.costoCamino(), 1e-6);
    }

    @Test
    void reaccionaCuandoGestorEventosPublicaUnEventoQueBloqueaUnaVia() {
        GrafoVial grafo = cuadricula(8);
        Nodo inicio = nodo(grafo, 0, 0);
        Nodo meta = nodo(grafo, 7, 7);
        PathfinderDinamico dinamico = new PathfinderDinamico(grafo);
        dinamico.iniciar(inicio, meta);
        GestorEventos gestor = new GestorEventos();
        gestor.agregarListener(dinamico);
        Edge bloqueada = dinamico.ruta().getEdges().get(2);

        // Quien aplica los eventos al grafo cierra el tramo; luego se publica el evento.
        bloqueada.cerrado = true;
        gestor.publicar(List.of(new EventoTrafico("Cierre", TipoEvento.VIA_CERRADA,
                bloqueada.origen.lon, bloqueada.origen.lat)));

        assertFalse(dinamico.ruta().getEdges().contains(bloqueada));
        assertEquals(new Pathfinder(grafo).ruta(inicio, meta).tiempoSegundos(),
                dinamico.costoCamino(), 1e-6);
    }

    @Test
    void absorbeCambiosDeTraficoQueNoCierranLaVia() {
        GrafoVial grafo = cuadricula(8);
        Nodo inicio = nodo(grafo, 0, 0);
        Nodo meta = nodo(grafo, 7, 7);
        PathfinderDinamico dinamico = new PathfinderDinamico(grafo);
        dinamico.iniciar(inicio, meta);
        Edge lenta = dinamico.ruta().getEdges().get(1);

        lenta.factorTrafico = 8.0;
        assertEquals(1, dinamico.sincronizarCambios());

        assertEquals(new Pathfinder(grafo).ruta(inicio, meta).tiempoSegundos(),
                dinamico.costoCamino(), 1e-6);
    }

    @Test
    void coincideConAEstrellaTrasCierresYReaperturasAleatorios() {
        GrafoVial grafo = cuadricula(12);
        Nodo inicio = nodo(grafo, 0, 0);
        Nodo meta = nodo(grafo, 11, 11);
        Pathfinder referencia = new Pathfinder(grafo);
        PathfinderDinamico dinamico = new PathfinderDinamico(grafo);
        dinamico.iniciar(inicio, meta);
        Random azar = new Random(42);
        List<Edge> edges = grafo.getEdges();

        for (int paso = 0; paso < 150; paso++) {
            Edge e = edges.get(azar.nextInt(edges.size()));
            e.cerrado = !e.cerrado;
            dinamico.notificarCambio(e);

            RutaResult esperada = referencia.ruta(inicio, meta);
            if (esperada == null) {
                assertFalse(dinamico.hayCamino(), "paso " + paso);
            } else {
                assertTrue(dinamico.hayCamino(), "paso " + paso);
                assertEquals(esperada.tiempoSegundos(), dinamico.costoCamino(), 1e-6,
                        "paso " + paso);
                assertTrue(dinamico.ruta().getEdges().stream().noneMatch(x -> x.cerrado));
            }
        }
    }

    @Test
    void rechazaNodosAjenosAlGrafoYUsoSinIniciar() {
        GrafoVial grafo = cuadricula(3);
        GrafoVial otro = cuadricula(3);
        PathfinderDinamico dinamico = new PathfinderDinamico(grafo);

        assertThrows(IllegalStateException.class, () -> dinamico.sincronizarCambios());
        assertThrows(IllegalArgumentException.class,
                () -> dinamico.iniciar(nodo(otro, 0, 0), nodo(grafo, 2, 2)));
    }

    // ------------------------------------------------------------------ ayudas

    /** Cuadrícula n×n de calles bidireccionales. */
    private static GrafoVial cuadricula(int n) {
        List<Via> vias = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<double[]> horizontal = new ArrayList<>();
            List<double[]> vertical = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                horizontal.add(punto(j, i));
                vertical.add(punto(i, j));
            }
            vias.add(via("h" + i, horizontal));
            vias.add(via("v" + i, vertical));
        }
        Proyector proyector = new Proyector();
        proyector.preProyectar(vias);
        GrafoVial grafo = new GrafoVial();
        grafo.construir(vias, proyector);
        return grafo;
    }

    private static double[] punto(int x, int y) {
        return new double[]{-70.0 + x * PASO, -15.0 + y * PASO};
    }

    private static Via via(String nombre, List<double[]> coordenadas) {
        Via via = new Via();
        via.id = nombre;
        via.name = nombre;
        via.highway = "residential";
        via.coords = coordenadas;
        return via;
    }

    private static Nodo nodo(GrafoVial grafo, int x, int y) {
        double[] p = punto(x, y);
        return grafo.nodoMasCercanoLonLat(p[0], p[1]);
    }
}
