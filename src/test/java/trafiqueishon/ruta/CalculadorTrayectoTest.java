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
}