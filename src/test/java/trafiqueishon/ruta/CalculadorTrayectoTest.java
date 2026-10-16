/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: dominio
 * Archivo: CalculadorTrayectoTest.java
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
package trafiqueishon.ruta;

import org.junit.jupiter.api.Test;
import trafiqueishon.geo.GrafoVial;
import trafiqueishon.geo.Proyector;
import trafiqueishon.geo.Via;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CalculadorTrayectoTest {

    @Test
    void ordenaParaderosEntreLosDosMasAlejadosUsandoDijkstra() {
        Via via = new Via();
        via.id = "linea";
        via.highway = "residential";
        via.name = "Avenida";
        via.coords = List.of(new double[]{-70.0, -15.0},
                new double[]{-69.999, -15.0},
                new double[]{-69.998, -15.0},
                new double[]{-69.997, -15.0});
        Proyector proyector = new Proyector();
        proyector.preProyectar(List.of(via));
        GrafoVial grafo = new GrafoVial();
        grafo.construir(List.of(via), proyector);
        List<PuntoImportante> puntos = List.of(
                new PuntoImportante("medio2", "Medio 2", -69.998, -15),
                new PuntoImportante("fin", "Terminal", -69.997, -15,
                        PuntoImportante.TipoPunto.DESTINO),
                new PuntoImportante("medio1", "Medio 1", -69.999, -15),
                new PuntoImportante("inicio", "Inicio", -70.0, -15,
                        PuntoImportante.TipoPunto.ORIGEN));

        Trayecto trayecto = new CalculadorTrayecto(grafo).calcularOrdenOptimo(
                "linea-1", "Línea", "Empresa", "SERVICIO", puntos);

        assertEquals("inicio", trayecto.puntos().getFirst().id());
        assertEquals("fin", trayecto.puntos().getLast().id());
        assertEquals(4, trayecto.puntos().stream().map(PuntoImportante::id)
                .distinct().count());
        assertTrue(trayecto.edges().stream().allMatch(edge -> !edge.cerrado));
    }

    @Test
    void optimizaExactamenteLaRutaDeDiecisieteParaderos() {
        List<double[]> coordenadas = new ArrayList<>();
        List<PuntoImportante> puntos = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            double lon = -70.0 + i * 0.0001;
            coordenadas.add(new double[]{lon, -15});
            PuntoImportante.TipoPunto tipo = i == 0 ? PuntoImportante.TipoPunto.ORIGEN
                    : i == 16 ? PuntoImportante.TipoPunto.DESTINO
                    : PuntoImportante.TipoPunto.PARADA;
            puntos.add(new PuntoImportante("p" + i, "Paradero " + i, lon, -15, tipo));
        }
        Via via = new Via();
        via.id = "linea-larga";
        via.highway = "residential";
        via.coords = coordenadas;
        Proyector proyector = new Proyector();
        proyector.preProyectar(List.of(via));
        GrafoVial grafo = new GrafoVial();
        grafo.construir(List.of(via), proyector);
        java.util.Collections.reverse(puntos);

        Trayecto trayecto = new CalculadorTrayecto(grafo).calcularOrdenOptimo(
                "larga", "Línea larga", "Empresa", "SERVICIO", puntos);

        assertEquals("p0", trayecto.puntos().getFirst().id());
        assertEquals("p16", trayecto.puntos().getLast().id());
        for (int i = 0; i < 17; i++) {
            assertEquals("p" + i, trayecto.puntos().get(i).id());
        }
    }

    @Test
    void eligeTerminalesPorDistanciaRealDeLaRedYNoSoloPorProximidadGeografica() {
        double[] a = {-70.0, -15.0};
        double[] b = {-69.999, -15.0};
        double[] c = {-69.998, -15.0};
        Via atajo = via("atajo", "trunk", a, c);
        Via caminoAB = via("ab", "residential", a,
                new double[]{-70.0, -15.002}, new double[]{-69.999, -15.002}, b);
        Via caminoBC = via("bc", "residential", b,
                new double[]{-69.999, -15.008}, new double[]{-69.998, -15.008}, c);
        List<Via> vias = List.of(atajo, caminoAB, caminoBC);
        Proyector proyector = new Proyector();
        proyector.preProyectar(vias);
        GrafoVial grafo = new GrafoVial();
        grafo.construir(vias, proyector);
        List<PuntoImportante> paraderos = List.of(
                new PuntoImportante("a", "A", a[0], a[1]),
                new PuntoImportante("b", "B", b[0], b[1]),
                new PuntoImportante("c", "C", c[0], c[1]));

        Trayecto resultado = new CalculadorTrayecto(grafo).calcularOrdenOptimo(
                "red", "Ruta por red", "Empresa", "SERVICIO", paraderos);

        assertEquals("b", resultado.puntos().getFirst().id());
        assertEquals("c", resultado.puntos().getLast().id());
    }

    private Via via(String id, String highway, double[]... coordenadas) {
        Via via = new Via();
        via.id = id;
        via.name = id;
        via.highway = highway;
        via.coords = java.util.Arrays.stream(coordenadas).toList();
        return via;
    }
}