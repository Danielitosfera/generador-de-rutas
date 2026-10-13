package trafiqueishon.geo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PathfinderTest {

    @Test
    void evitaTramosCerradosCuandoBuscaUnaRutaAlternativa() {
        Via directa = via("directa", "cerrada",
                new double[]{-70.0, -15.0}, new double[]{-69.999, -15.0});
        Via alternativa = via("alternativa", "abierta",
                new double[]{-70.0, -15.0}, new double[]{-69.9995, -15.0005},
                new double[]{-69.999, -15.0});
        List<Via> vias = List.of(directa, alternativa);
        Proyector proyector = new Proyector();
        proyector.preProyectar(vias);
        GrafoVial grafo = new GrafoVial();
        grafo.construir(vias, proyector);
        grafo.getEdges().stream().filter(edge -> "cerrada".equals(edge.name))
                .forEach(edge -> edge.cerrado = true);
        Nodo inicio = grafo.nodoMasCercanoLonLat(-70.0, -15.0);
        Nodo fin = grafo.nodoMasCercanoLonLat(-69.999, -15.0);

        RutaResult resultado = new Pathfinder(grafo).ruta(inicio, fin);

        assertNotNull(resultado);
        assertFalse(resultado.getEdges().isEmpty());
        assertTrue(resultado.getEdges().stream().allMatch(edge -> "abierta".equals(edge.name)));
    }

    private Via via(String id, String nombre, double[]... coordenadas) {
        Via via = new Via();
        via.id = id;
        via.name = nombre;
        via.highway = "residential";
        via.coords = java.util.Arrays.stream(coordenadas).toList();
        return via;
    }
}