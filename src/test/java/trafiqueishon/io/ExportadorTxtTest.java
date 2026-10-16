/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: datos
 * Archivo: ExportadorTxtTest.java
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
 * @author Daniel Taza (DT)
 */
package trafiqueishon.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import trafiqueishon.geo.Edge;
import trafiqueishon.geo.GrafoVial;
import trafiqueishon.geo.Proyector;
import trafiqueishon.geo.Via;
import trafiqueishon.ruta.PuntoImportante;
import trafiqueishon.ruta.Trayecto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExportadorTxtTest {

    @TempDir
    Path directorio;

    @Test
    void listaTodosLosPuntosDePartidaYRetornoEnOrdenConCoordenadas() throws IOException {
        Trayecto partida = trayecto("PARTIDA", List.of(
                punto("p1", "Jr. Chirihuano", -70.1330, -15.4990),
                punto("p2", "Av. Cannica", -70.1320, -15.4980),
                punto("p3", "Plaza Bolognesi", -70.1310, -15.4970)));
        Trayecto retorno = trayecto("RETORNO", List.of(
                punto("p4", "Sede de Colegio Trinomo", -70.1200, -15.4900),
                punto("p5", "Jr. Chirihuano", -70.1330, -15.4990)));
        Path destino = directorio.resolve("ruta.txt");

        new ExportadorTxt().exportar(partida, retorno, destino);
        String texto = Files.readString(destino);

        assertTrue(texto.contains("TRAFIQUEISHON - RECORRIDO DE RUTA"));
        assertTrue(texto.contains("RECORRIDO DE PARTIDA"));
        assertTrue(texto.contains("RECORRIDO DE RETORNO"));
        assertTrue(texto.contains("1. JR. CHIRIHUANO"));
        assertTrue(texto.contains("(x=-70.13300, y=-15.49900)"));
        assertTrue(texto.contains("3. PLAZA BOLOGNESI"));
        assertTrue(texto.contains("Total: 3 puntos"));
        assertTrue(texto.contains("1. SEDE DE COLEGIO TRINOMO"));
        assertTrue(texto.contains("Total: 2 puntos"));
        assertTrue(texto.indexOf("RECORRIDO DE PARTIDA") < texto.indexOf("RECORRIDO DE RETORNO"));
        assertTrue(texto.indexOf("AV. CANNICA") < texto.indexOf("PLAZA BOLOGNESI"));
    }

    @Test
    void sinPuntosNombradosRecorreLosNodosDeLosTramos() {
        Trayecto sinPuntos = trayectoConTramos();

        String texto = new ExportadorTxt().generar(List.of(
                new RutaExportable("Micro Prueba", "#000000", sinPuntos)));

        assertTrue(texto.contains("Total: 3 puntos"), texto);
        assertTrue(texto.contains("1. CALLE UNO"), texto);
    }

    @Test
    void exportaVariasRutasSeparadasPorLinea() {
        Trayecto ida = trayecto("PARTIDA", List.of(punto("a", "A", -70.1, -15.1)));
        Trayecto vuelta = trayecto("RETORNO", List.of(punto("b", "B", -70.2, -15.2)));

        String texto = new ExportadorTxt().generar(List.of(
                new RutaExportable("Micro · Ida", "#000000", ida),
                new RutaExportable("Micro · Vuelta", "#000000", vuelta)));

        assertTrue(texto.contains("-".repeat(44)));
        assertTrue(texto.contains("RUTA: MICRO · IDA"));
        assertTrue(texto.contains("RUTA: MICRO · VUELTA"));
        assertTrue(texto.endsWith("=".repeat(44) + "\n"));
    }

    @Test
    void rechazaUnaListaVaciaDeRutas() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExportadorTxt().exportar(List.of(), directorio.resolve("x.txt")));
    }

    private static PuntoImportante punto(String id, String nombre, double lon, double lat) {
        return new PuntoImportante(id, nombre, lon, lat);
    }

    private static Trayecto trayecto(String sentido, List<PuntoImportante> puntos) {
        return new Trayecto("t-" + sentido, "Ruta " + sentido, "Empresa", puntos,
                List.of(), 0, 0, sentido);
    }

    private static Trayecto trayectoConTramos() {
        Via via = new Via();
        via.id = "v";
        via.name = "Calle Uno";
        via.highway = "residential";
        via.oneway = true;
        via.coords = new ArrayList<>(List.of(new double[]{-70.0, -15.0},
                new double[]{-69.999, -15.0}, new double[]{-69.998, -15.0}));
        List<Via> vias = List.of(via);
        Proyector proyector = new Proyector();
        proyector.preProyectar(vias);
        GrafoVial grafo = new GrafoVial();
        grafo.construir(vias, proyector);
        List<Edge> edges = new ArrayList<>(grafo.getEdges());
        return new Trayecto("t", "Sin puntos", "Empresa", List.of(), edges, 0, 0, "PARTIDA");
    }
}
