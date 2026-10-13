package trafiqueishon.geo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RutaResult {

    private final List<Edge> edges;
    private final Nodo inicio;
    private final Nodo fin;

    public RutaResult(List<Edge> edges, Nodo inicio, Nodo fin) {
        this.edges = edges == null ? Collections.emptyList() : List.copyOf(edges);
        this.inicio = inicio;
        this.fin = fin;
    }

    public List<Edge> getEdges() { return edges; }
    public Nodo getInicio() { return inicio; }
    public Nodo getFin() { return fin; }

    public List<Nodo> getNodos() {
        List<Nodo> nodos = new ArrayList<>(edges.size() + 1);
        if (inicio != null) nodos.add(inicio);
        for (Edge e : edges) nodos.add(e.destino);
        return nodos;
    }

    public double distanciaMetros() {
        double m = 0;
        for (Edge e : edges) m += e.longitudMetros();
        return m;
    }

    public double tiempoSegundos() {
        double s = 0;
        for (Edge e : edges) s += e.costoActual();
        return s;
    }

    public String resumen() {
        double km = distanciaMetros() / 1000.0;
        double seg = tiempoSegundos();
        int min = (int) (seg / 60);
        int s = (int) Math.round(seg % 60);
        return String.format("%.2f km  |  %d min %02d s  |  %d tramos",
                km, min, s, edges.size());
    }

    public boolean esVacia() { return edges.isEmpty(); }
}