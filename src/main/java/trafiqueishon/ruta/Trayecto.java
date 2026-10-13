package trafiqueishon.ruta;

import trafiqueishon.geo.Edge;

import java.util.List;

public record Trayecto(
        String id,
        String nombre,
        String empresa,
        List<PuntoImportante> puntos,
        List<Edge> edges,
        double distanciaMetros,
        double tiempoSegundos,
        String sentido) {

    public Trayecto {
        puntos = List.copyOf(puntos);
        edges = List.copyOf(edges);
        sentido = sentido == null ? "PARTIDA" : sentido;
    }

    public String resumen() {
        double km = distanciaMetros / 1000.0;
        int min = (int) (tiempoSegundos / 60);
        int seg = (int) Math.round(tiempoSegundos % 60);
        return String.format("%.2f km  |  %d min %02d s  |  %d tramos",
                km, min, seg, edges.size());
    }

    public boolean esVacia() { return edges.isEmpty(); }

    public boolean esVacio() { return esVacia(); }
}