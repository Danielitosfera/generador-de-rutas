/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: simulación
 * Archivo: PathfinderDinamico.java
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

import trafiqueishon.geo.Edge;
import trafiqueishon.geo.GrafoVial;
import trafiqueishon.geo.Nodo;
import trafiqueishon.geo.RutaResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Búsqueda incremental de rutas (D* Lite) para tráfico dinámico.
 *
 * <p>Complementa a {@code Pathfinder}: éste calcula una ruta desde cero; esta clase
 * conserva el estado de la búsqueda y, cuando un tramo se cierra, se reabre o cambia
 * de costo, corrige solo los nodos afectados en lugar de reiniciar. La búsqueda
 * parte de la meta hacia el inicio, de modo que el vehículo puede avanzar con
 * {@link #mover(Nodo)} sin perder lo ya calculado.</p>
 *
 * <p>Como {@link EventoListener}, se registra en {@code GestorEventos}: tras cada
 * publicación detecta qué tramos cambiaron de costo (p. ej. por un evento que
 * bloquea una vía) y actualiza la ruta. Debe registrarse después del listener que
 * aplica los eventos al grafo.</p>
 *
 * <p>Costos y heurística: usa {@link Edge#costoActual()} (infinito si el tramo está
 * cerrado) y la distancia de círculo máximo dividida por la velocidad máxima de la red,
 * igual que {@code Pathfinder}. Los métodos públicos están sincronizados.</p>
 */
public final class PathfinderDinamico implements EventoListener {

    private static final double INF = Double.POSITIVE_INFINITY;

    private final GrafoVial grafo;
    private final List<Edge> edges;
    private final double[] g;
    private final double[] rhs;
    private final double[] clave1;
    private final double[] clave2;
    private final int[] version;
    private final boolean[] enCola;
    private final double[] costoConocido;
    private final PriorityQueue<Entrada> cola = new PriorityQueue<>(
            Comparator.<Entrada>comparingDouble(e -> e.k1).thenComparingDouble(e -> e.k2));

    private Nodo inicio;
    private Nodo ultimoInicio;
    private Nodo meta;
    private double km;
    private int expansionesUltimoCalculo;
    private int expansionesTotales;

    public PathfinderDinamico(GrafoVial grafo) {
        this.grafo = Objects.requireNonNull(grafo);
        this.edges = grafo.getEdges();
        int n = grafo.numNodos();
        g = new double[n];
        rhs = new double[n];
        clave1 = new double[n];
        clave2 = new double[n];
        version = new int[n];
        enCola = new boolean[n];
        costoConocido = new double[edges.size()];
    }

    /**
     * Calcula la ruta inicial.
     *
     * @return {@code true} si existe un camino de {@code inicio} a {@code meta}
     */
    public synchronized boolean iniciar(Nodo inicio, Nodo meta) {
        if (!nodoValido(inicio) || !nodoValido(meta)) {
            throw new IllegalArgumentException("Los nodos no pertenecen al grafo.");
        }
        if (edges.size() != costoConocido.length || grafo.numNodos() != g.length) {
            throw new IllegalStateException("El grafo cambió; crea un nuevo PathfinderDinamico.");
        }
        this.inicio = inicio;
        this.ultimoInicio = inicio;
        this.meta = meta;
        km = 0;
        Arrays.fill(g, INF);
        Arrays.fill(rhs, INF);
        Arrays.fill(enCola, false);
        Arrays.fill(version, 0);
        cola.clear();
        for (Edge e : edges) costoConocido[e.id] = costo(e);
        rhs[meta.id] = 0;
        insertar(meta);
        calcularCaminoMasCorto();
        return hayCamino();
    }

    /** Indica que el vehículo avanzó hasta {@code nuevoInicio}; no recalcula desde cero. */
    public synchronized void mover(Nodo nuevoInicio) {
        exigirIniciado();
        if (!nodoValido(nuevoInicio)) {
            throw new IllegalArgumentException("El nodo no pertenece al grafo.");
        }
        km += heuristica(ultimoInicio, nuevoInicio);
        ultimoInicio = nuevoInicio;
        inicio = nuevoInicio;
        calcularCaminoMasCorto();
    }

    /** Avisa que el costo de un tramo cambió (cierre, reapertura, tráfico). */
    public synchronized void notificarCambio(Edge edge) {
        notificarCambios(List.of(Objects.requireNonNull(edge)));
    }

    /** Avisa que el costo de varios tramos cambió; actualiza solo sus nodos de origen. */
    public synchronized void notificarCambios(Collection<Edge> cambiados) {
        exigirIniciado();
        for (Edge e : cambiados) {
            costoConocido[e.id] = costo(e);
            actualizarVertice(e.origen);
        }
        calcularCaminoMasCorto();
    }

    /**
     * Detecta los tramos cuyo costo difiere del último conocido y los procesa.
     *
     * @return cantidad de tramos que cambiaron
     */
    public synchronized int sincronizarCambios() {
        exigirIniciado();
        int cambios = 0;
        for (Edge e : edges) {
            double actual = costo(e);
            if (Double.compare(actual, costoConocido[e.id]) == 0) continue;
            costoConocido[e.id] = actual;
            actualizarVertice(e.origen);
            cambios++;
        }
        if (cambios > 0) calcularCaminoMasCorto();
        else expansionesUltimoCalculo = 0;
        return cambios;
    }

    /** Reacciona a un cambio de eventos: recalcula solo lo afectado. */
    @Override
    public synchronized void eventosActualizados(List<EventoTrafico> eventos) {
        if (meta == null) return;
        sincronizarCambios();
    }

    public synchronized boolean hayCamino() {
        return inicio != null && g[inicio.id] < INF;
    }

    /** Costo (segundos) del camino actual, o infinito si no hay. */
    public synchronized double costoCamino() {
        return inicio == null ? INF : g[inicio.id];
    }

    /** Nodos del camino actual desde el inicio hasta la meta; vacío si no hay camino. */
    public synchronized List<Nodo> camino() {
        List<Edge> tramos = tramosOVacio();
        if (tramos == null) return List.of();
        List<Nodo> nodos = new ArrayList<>(tramos.size() + 1);
        nodos.add(inicio);
        for (Edge e : tramos) nodos.add(e.destino);
        return Collections.unmodifiableList(nodos);
    }

    /** Camino actual como {@link RutaResult}, o {@code null} si no hay camino. */
    public synchronized RutaResult ruta() {
        List<Edge> tramos = tramosOVacio();
        return tramos == null ? null : new RutaResult(tramos, inicio, meta);
    }

    /** Nodos procesados en el último cálculo; sirve para comprobar que fue incremental. */
    public synchronized int expansionesUltimoCalculo() { return expansionesUltimoCalculo; }

    public synchronized int expansionesTotales() { return expansionesTotales; }

    // ------------------------------------------------------------------ D* Lite

    private void calcularCaminoMasCorto() {
        expansionesUltimoCalculo = 0;
        while (true) {
            Entrada tope = topeVigente();
            if (tope == null) break;
            double[] claveInicio = calcularClave(inicio);
            boolean inicioConsistente = g[inicio.id] == rhs[inicio.id];
            if (comparar(tope.k1, tope.k2, claveInicio[0], claveInicio[1]) >= 0
                    && inicioConsistente) {
                break;
            }
            cola.poll();
            int u = tope.id;
            enCola[u] = false;
            expansionesUltimoCalculo++;
            expansionesTotales++;
            double[] nueva = calcularClave(grafo.getNodos().get(u));
            if (comparar(tope.k1, tope.k2, nueva[0], nueva[1]) < 0) {
                insertar(grafo.getNodos().get(u));
            } else if (g[u] > rhs[u]) {
                g[u] = rhs[u];
                for (Edge e : grafo.getNodos().get(u).entrantes) actualizarVertice(e.origen);
            } else {
                g[u] = INF;
                actualizarVertice(grafo.getNodos().get(u));
                for (Edge e : grafo.getNodos().get(u).entrantes) actualizarVertice(e.origen);
            }
        }
    }

    private void actualizarVertice(Nodo u) {
        if (u != meta) {
            double mejor = INF;
            for (Edge e : u.salientes) {
                double c = costo(e);
                if (c == INF) continue;
                mejor = Math.min(mejor, c + g[e.destino.id]);
            }
            rhs[u.id] = mejor;
        }
        if (enCola[u.id]) {
            enCola[u.id] = false;
            version[u.id]++;
        }
        if (g[u.id] != rhs[u.id]) insertar(u);
    }

    private void insertar(Nodo u) {
        double[] k = calcularClave(u);
        clave1[u.id] = k[0];
        clave2[u.id] = k[1];
        version[u.id]++;
        enCola[u.id] = true;
        cola.add(new Entrada(u.id, k[0], k[1], version[u.id]));
    }

    private double[] calcularClave(Nodo s) {
        double m = Math.min(g[s.id], rhs[s.id]);
        if (m == INF) return new double[]{INF, INF};
        return new double[]{m + heuristica(inicio, s) + km, m};
    }

    private Entrada topeVigente() {
        while (!cola.isEmpty()) {
            Entrada e = cola.peek();
            if (enCola[e.id] && version[e.id] == e.version) return e;
            cola.poll();
        }
        return null;
    }

    private static int comparar(double a1, double a2, double b1, double b2) {
        int c = Double.compare(a1, b1);
        return c != 0 ? c : Double.compare(a2, b2);
    }

    // ------------------------------------------------------------------ utilidades

    private List<Edge> tramosOVacio() {
        if (!hayCamino()) return null;
        List<Edge> tramos = new ArrayList<>();
        Nodo actual = inicio;
        int limite = grafo.numNodos() + 1;
        while (actual != meta) {
            if (limite-- <= 0) return null;
            Edge mejor = null;
            double mejorCosto = INF;
            for (Edge e : actual.salientes) {
                double c = costo(e);
                if (c == INF || g[e.destino.id] == INF) continue;
                double total = c + g[e.destino.id];
                if (total < mejorCosto) {
                    mejorCosto = total;
                    mejor = e;
                }
            }
            if (mejor == null) return null;
            tramos.add(mejor);
            actual = mejor.destino;
        }
        return tramos;
    }

    private static double costo(Edge e) {
        double c = e.costoActual();
        return Double.isFinite(c) && c >= 0 ? c : INF;
    }

    private double heuristica(Nodo a, Nodo b) {
        double lat1 = Math.toRadians(a.lat);
        double lat2 = Math.toRadians(b.lat);
        double sinLat = Math.sin((lat2 - lat1) / 2);
        double sinLon = Math.sin(Math.toRadians(b.lon - a.lon) / 2);
        double h = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
        double metros = 2 * 6_371_000 * Math.asin(Math.sqrt(Math.min(1, h)));
        return metros / grafo.getVelocidadMaximaMps();
    }

    private boolean nodoValido(Nodo nodo) {
        return nodo != null && nodo.id >= 0 && nodo.id < grafo.numNodos()
                && grafo.getNodos().get(nodo.id) == nodo;
    }

    private void exigirIniciado() {
        if (meta == null) throw new IllegalStateException("Llama primero a iniciar().");
    }

    private static final class Entrada {
        private final int id;
        private final double k1;
        private final double k2;
        private final int version;

        private Entrada(int id, double k1, double k2, int version) {
            this.id = id;
            this.k1 = k1;
            this.k2 = k2;
            this.version = version;
        }
    }
}
