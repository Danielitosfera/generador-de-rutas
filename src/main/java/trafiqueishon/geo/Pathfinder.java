<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: rutas
 * Archivo: Pathfinder.java
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
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
package trafiqueishon.geo;

import java.util.*;
import java.util.function.Predicate;

public class Pathfinder {

    public enum Modo { DIJKSTRA, A_ESTRELLA }

    private final GrafoVial grafo;
    private Modo modo = Modo.A_ESTRELLA;
    private double penalizacionGiro = 0;

    public Pathfinder(GrafoVial grafo) {
        this.grafo = Objects.requireNonNull(grafo);
    }

    public void setModo(Modo m) { this.modo = Objects.requireNonNull(m); }
    public Modo getModo() { return modo; }

    public void setPenalizacionGiro(double segundos) {
        if (!Double.isFinite(segundos) || segundos < 0) {
            throw new IllegalArgumentException("La penalizacion debe ser finita y no negativa.");
        }
        this.penalizacionGiro = segundos;
    }

    public RutaResult ruta(Nodo inicio, Nodo fin) {
        return ruta(inicio, fin, edge -> !edge.cerrado);
    }

    public RutaResult ruta(Nodo inicio, Nodo fin, Predicate<Edge> filtroVial) {
        if (!nodoValido(inicio) || !nodoValido(fin) || filtroVial == null) return null;
        List<Edge> r = buscar(inicio, fin, Collections.emptyMap(), filtroVial);
        return r == null ? null : new RutaResult(r, inicio, fin);
    }

    public Map<Nodo, Double> costosMinimosDesde(Nodo inicio) {
        if (!nodoValido(inicio)) return Map.of();
        Map<Nodo, Double> costos = new IdentityHashMap<>();
        Set<Nodo> visitados = Collections.newSetFromMap(new IdentityHashMap<>());
        PriorityQueue<EntradaNodo> cola = new PriorityQueue<>(
                Comparator.comparingDouble(EntradaNodo::costo));
        costos.put(inicio, 0.0);
        cola.add(new EntradaNodo(inicio, 0));
        while (!cola.isEmpty()) {
            EntradaNodo actual = cola.poll();
            if (!visitados.add(actual.nodo())) continue;
            for (Edge edge : actual.nodo().salientes) {
                if (edge.cerrado) continue;
                double peso = edge.costoActual();
                if (!Double.isFinite(peso) || peso < 0) continue;
                double nuevoCosto = actual.costo() + peso;
                if (nuevoCosto < costos.getOrDefault(edge.destino, Double.POSITIVE_INFINITY)) {
                    costos.put(edge.destino, nuevoCosto);
                    cola.add(new EntradaNodo(edge.destino, nuevoCosto));
                }
            }
        }
        return Collections.unmodifiableMap(costos);
    }

    public RutaResult ruta(List<Nodo> waypoints) {
        return ruta(waypoints, edge -> true);
    }

    public RutaResult ruta(List<Nodo> waypoints, Predicate<Edge> filtroVial) {
        if (waypoints == null || waypoints.size() < 2 || filtroVial == null
                || waypoints.stream().anyMatch(nodo -> !nodoValido(nodo))) {
            return null;
        }
        List<Edge> recorrido = new ArrayList<>();
        for (int i = 1; i < waypoints.size(); i++) {
            Nodo inicio = waypoints.get(i - 1);
            Nodo fin = waypoints.get(i);
            List<Edge> tramo = buscar(inicio, fin, Collections.emptyMap(), filtroVial);
            if (tramo == null) return null;
            recorrido.addAll(tramo);
        }
        return new RutaResult(recorrido, waypoints.getFirst(), waypoints.getLast());
    }

    public List<RutaResult> rutasAlternativas(Nodo inicio, Nodo fin, int k) {
        if (k < 0) throw new IllegalArgumentException("La cantidad de rutas no puede ser negativa.");
        if (!nodoValido(inicio) || !nodoValido(fin)) return List.of();
        List<RutaResult> resultado = new ArrayList<>();
        Set<List<Integer>> firmas = new HashSet<>();
        Map<String, Double> penalizaciones = new HashMap<>();

        for (int i = 0; i < k; i++) {
            List<Edge> r = buscar(inicio, fin, penalizaciones, edge -> !edge.cerrado);
            if (r == null || r.isEmpty()) break;
            List<Integer> firma = r.stream().map(edge -> edge.id).toList();
            if (!firmas.add(firma)) break;
            resultado.add(new RutaResult(r, inicio, fin));

            for (Edge e : r) {
                String ida = e.origen.id + ":" + e.destino.id;
                String vuelta = e.destino.id + ":" + e.origen.id;
                penalizaciones.merge(ida, e.costoActual() * 0.6, Double::sum);
                penalizaciones.merge(vuelta, e.costoActual() * 0.6, Double::sum);
            }
        }
        return resultado;
    }

    private List<Edge> buscar(Nodo inicio, Nodo fin, Map<String, Double> penalizaciones,
                              Predicate<Edge> filtroVial) {
        if (!nodoValido(inicio) || !nodoValido(fin) || filtroVial == null) return null;
        if (inicio == fin) return Collections.emptyList();

        List<Edge> edges = grafo.getEdges();
        int estadoInicial = edges.size();
        int numeroEstados = edges.size() + 1;
        double[] g = new double[numeroEstados];
        Arrays.fill(g, Double.POSITIVE_INFINITY);
        double[] f = new double[numeroEstados];
        Arrays.fill(f, Double.POSITIVE_INFINITY);
        Edge[] cameFrom = new Edge[numeroEstados];
        int[] estadoAnterior = new int[numeroEstados];
        Arrays.fill(estadoAnterior, -1);
        boolean[] cerrado = new boolean[numeroEstados];

        g[estadoInicial] = 0;
        f[estadoInicial] = (modo == Modo.A_ESTRELLA) ? heuristica(inicio, fin) : 0;

        PriorityQueue<EntradaCola> abierta = new PriorityQueue<>(
                Comparator.comparingDouble(EntradaCola::prioridad));
        abierta.add(new EntradaCola(estadoInicial, f[estadoInicial]));

        while (!abierta.isEmpty()) {
            int estado = abierta.poll().estado();
            if (cerrado[estado]) continue;
            Nodo actual = estado == estadoInicial ? inicio : edges.get(estado).destino;

            if (actual == fin) return reconstruir(cameFrom, estadoAnterior, estado, estadoInicial);
            cerrado[estado] = true;
            Edge previa = estado == estadoInicial ? null : edges.get(estado);
            String nombrePrevio = (previa != null) ? previa.name : null;

            for (Edge e : actual.salientes) {
                if (!filtroVial.test(e)) continue;
                int siguienteEstado = e.id;
                if (cerrado[siguienteEstado]) continue;

                double costo = e.costoActual()
                        + penalizaciones.getOrDefault(
                        e.origen.id + ":" + e.destino.id, 0.0);

                if (penalizacionGiro > 0 && nombrePrevio != null && e.name != null
                        && !e.name.equals(nombrePrevio)) {
                    costo += penalizacionGiro;
                }

                double tentativa = g[estado] + costo;
                if (tentativa < g[siguienteEstado]) {
                    cameFrom[siguienteEstado] = e;
                    estadoAnterior[siguienteEstado] = estado;
                    g[siguienteEstado] = tentativa;
                    f[siguienteEstado] = tentativa
                            + ((modo == Modo.A_ESTRELLA) ? heuristica(e.destino, fin) : 0);
                    abierta.add(new EntradaCola(siguienteEstado, f[siguienteEstado]));
                }
            }
        }
        return null;
    }

    private boolean nodoValido(Nodo nodo) {
        return nodo != null && nodo.id >= 0 && nodo.id < grafo.numNodos()
                && grafo.getNodos().get(nodo.id) == nodo;
    }

    private List<Edge> reconstruir(Edge[] cameFrom, int[] estadoAnterior,
                                   int estadoFinal, int estadoInicial) {
        LinkedList<Edge> ruta = new LinkedList<>();
        int estado = estadoFinal;
        while (estado != estadoInicial) {
            Edge e = cameFrom[estado];
            if (e == null) return Collections.emptyList();
            ruta.addFirst(e);
            estado = estadoAnterior[estado];
        }
        return ruta;
    }

    private double heuristica(Nodo a, Nodo b) {
        double lat1 = Math.toRadians(a.lat);
        double lat2 = Math.toRadians(b.lat);
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.lon - a.lon);
        double sinLat = Math.sin(dLat / 2);
        double sinLon = Math.sin(dLon / 2);
        double h = sinLat * sinLat
                + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
        double distancia = 2 * 6_371_000 * Math.asin(Math.sqrt(h));
        return distancia / grafo.getVelocidadMaximaMps();
    }

    private record EntradaCola(int estado, double prioridad) {}
    private record EntradaNodo(Nodo nodo, double costo) {}
}