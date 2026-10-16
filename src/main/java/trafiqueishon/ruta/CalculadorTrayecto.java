/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: dominio
 * Archivo: CalculadorTrayecto.java
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

import trafiqueishon.geo.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class CalculadorTrayecto {

    private final GrafoVial grafo;
    private final Pathfinder pathfinder;

    public CalculadorTrayecto(GrafoVial grafo) {
        this.grafo = grafo;
        this.pathfinder = new Pathfinder(grafo);
        this.pathfinder.setModo(Pathfinder.Modo.DIJKSTRA);
        this.pathfinder.setPenalizacionGiro(0);
    }

    public Trayecto calcular(String id, String nombre, String empresa,
                             String sentido, List<PuntoImportante> puntos) {
        if (puntos == null || puntos.size() < 2) {
            throw new IllegalArgumentException("Se requieren al menos 2 puntos.");
        }
        List<Edge> edges = new ArrayList<>();
        for (int i = 1; i < puntos.size(); i++) {
            PuntoImportante a = puntos.get(i - 1);
            PuntoImportante b = puntos.get(i);
            Nodo inicio = grafo.nodoMasCercanoLonLat(a.lon(), a.lat());
            Nodo fin = grafo.nodoMasCercanoLonLat(b.lon(), b.lat());
            if (inicio == null || fin == null) {
                throw new IllegalStateException(
                        "No hay nodo vial cerca de " + a.nombre() + " o " + b.nombre());
            }
            RutaResult tramo = pathfinder.ruta(inicio, fin);
            if (tramo == null) {
                throw new IllegalStateException(
                        "Sin ruta vial entre " + a.nombre() + " y " + b.nombre());
            }
            edges.addAll(tramo.getEdges());
        }
        if (edges.isEmpty()) {
            throw new IllegalStateException("Los paraderos se ajustan al mismo nodo vial.");
        }
        double dist = edges.stream().mapToDouble(Edge::longitudMetros).sum();
        double tiempo = edges.stream().mapToDouble(Edge::costoActual).sum();
        return new Trayecto(id, nombre, empresa, puntos, edges, dist, tiempo, sentido);
    }

    public Trayecto calcularOrdenOptimo(String id, String nombre, String empresa,
                                        String sentido, List<PuntoImportante> paraderos) {
        if (paraderos == null || paraderos.size() < 2) {
            throw new IllegalArgumentException("Se requieren al menos 2 paraderos.");
        }
        List<PuntoImportante> unicos = new ArrayList<>();
        for (PuntoImportante punto : paraderos) {
            if (unicos.stream().noneMatch(p -> p.id().equals(punto.id()))) unicos.add(punto);
        }
        if (unicos.size() < 2) {
            throw new IllegalArgumentException("Los paraderos deben ser distintos.");
        }
        double[][] costo = matrizCostos(unicos);
        int[] extremos = extremosMasLejanos(unicos, costo);
        List<Integer> intermedios = new ArrayList<>();
        for (int i = 0; i < unicos.size(); i++) {
            if (i != extremos[0] && i != extremos[1]) intermedios.add(i);
        }
        int[] orden = intermedios.size() <= 15
                ? ordenExacto(extremos, intermedios, costo)
                : ordenAproximado(extremos, intermedios, costo);
        List<PuntoImportante> ordenados = new ArrayList<>(unicos.size());
        ordenados.add(unicos.get(extremos[0]));
        for (int indice : orden) ordenados.add(unicos.get(indice));
        ordenados.add(unicos.get(extremos[1]));
        return calcular(id, nombre, empresa, sentido, ordenados);
    }

    private int[] extremosMasLejanos(List<PuntoImportante> puntos, double[][] costo) {
        int origenMarcado = -1;
        int destinoMarcado = -1;
        for (int i = 0; i < puntos.size(); i++) {
            if (origenMarcado < 0
                    && puntos.get(i).tipo() == PuntoImportante.TipoPunto.ORIGEN) {
                origenMarcado = i;
            }
            if (destinoMarcado < 0
                    && (puntos.get(i).tipo() == PuntoImportante.TipoPunto.DESTINO
                    || puntos.get(i).tipo() == PuntoImportante.TipoPunto.TERMINAL)) {
                destinoMarcado = i;
            }
        }
        if (origenMarcado >= 0 && destinoMarcado >= 0 && origenMarcado != destinoMarcado) {
            return new int[]{origenMarcado, destinoMarcado};
        }
        if (origenMarcado >= 0) {
            return new int[]{origenMarcado,
                    extremoMasLejanoConectado(origenMarcado, costo, true)};
        }
        if (destinoMarcado >= 0) {
            return new int[]{extremoMasLejanoConectado(destinoMarcado, costo, false),
                    destinoMarcado};
        }

        int primero = -1, segundo = -1;
        double mayor = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < puntos.size(); i++) {
            for (int j = i + 1; j < puntos.size(); j++) {
                double ida = costo[i][j];
                double vuelta = costo[j][i];
                if (!Double.isFinite(ida) && !Double.isFinite(vuelta)) continue;
                double separacion = Double.isFinite(ida) && Double.isFinite(vuelta)
                        ? ida + vuelta : Double.isFinite(ida) ? ida : vuelta;
                if (separacion > mayor) {
                    mayor = separacion;
                    primero = i;
                    segundo = j;
                }
            }
        }
        if (primero < 0) {
            throw new IllegalStateException("No existe conexión vial entre los paraderos.");
        }
        if (Double.isFinite(costo[segundo][primero])
                && (!Double.isFinite(costo[primero][segundo])
                || costo[segundo][primero] < costo[primero][segundo])) {
            return new int[]{segundo, primero};
        }
        return new int[]{primero, segundo};
    }

    private int extremoMasLejanoConectado(int fijo, double[][] costo, boolean esOrigen) {
        int mejor = -1;
        double mayor = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < costo.length; i++) {
            if (i == fijo) continue;
            double distancia = esOrigen ? costo[fijo][i] : costo[i][fijo];
            if (Double.isFinite(distancia) && distancia > mayor) {
                mayor = distancia;
                mejor = i;
            }
        }
        if (mejor < 0) {
            throw new IllegalStateException("No existe conexión vial desde/hacia el terminal marcado.");
        }
        return mejor;
    }

    private int[] ordenExacto(int[] extremos, List<Integer> intermedios, double[][] costo) {
        int m = intermedios.size();
        if (m == 0) return new int[0];
        int estados = 1 << m;
        double[][] dp = new double[estados][m];
        int[][] previo = new int[estados][m];
        for (double[] fila : dp) Arrays.fill(fila, Double.POSITIVE_INFINITY);
        for (int[] fila : previo) Arrays.fill(fila, -1);

        for (int i = 0; i < m; i++) {
            dp[1 << i][i] = costo[extremos[0]][intermedios.get(i)];
        }
        for (int mascara = 1; mascara < estados; mascara++) {
            for (int ultimo = 0; ultimo < m; ultimo++) {
                if ((mascara & (1 << ultimo)) == 0) continue;
                double actual = dp[mascara][ultimo];
                if (!Double.isFinite(actual)) continue;
                for (int siguiente = 0; siguiente < m; siguiente++) {
                    if ((mascara & (1 << siguiente)) != 0) continue;
                    int nuevaMascara = mascara | (1 << siguiente);
                    double nuevoCosto = actual + costo[intermedios.get(ultimo)]
                            [intermedios.get(siguiente)];
                    if (nuevoCosto < dp[nuevaMascara][siguiente]) {
                        dp[nuevaMascara][siguiente] = nuevoCosto;
                        previo[nuevaMascara][siguiente] = ultimo;
                    }
                }
            }
        }

        int mascaraCompleta = estados - 1;
        int ultimoMejor = -1;
        double mejorCosto = Double.POSITIVE_INFINITY;
        for (int ultimo = 0; ultimo < m; ultimo++) {
            double costoTotal = dp[mascaraCompleta][ultimo]
                    + costo[intermedios.get(ultimo)][extremos[1]];
            if (costoTotal < mejorCosto) {
                mejorCosto = costoTotal;
                ultimoMejor = ultimo;
            }
        }
        if (ultimoMejor < 0 || !Double.isFinite(mejorCosto)) {
            throw new IllegalStateException("No existe una ruta que conecte todos los paraderos.");
        }
        int[] resultado = new int[m];
        int mascara = mascaraCompleta;
        for (int posicion = m - 1; posicion >= 0; posicion--) {
            resultado[posicion] = intermedios.get(ultimoMejor);
            int siguiente = previo[mascara][ultimoMejor];
            mascara ^= 1 << ultimoMejor;
            ultimoMejor = siguiente;
        }
        return resultado;
    }

    private int[] ordenAproximado(int[] extremos, List<Integer> intermedios,
                                  double[][] costo) {
        List<Integer> pendientes = new ArrayList<>(intermedios);
        List<Integer> recorrido = new ArrayList<>();
        int actual = extremos[0];
        while (!pendientes.isEmpty()) {
            int desde = actual;
            int proximo = pendientes.stream().min(Comparator.comparingDouble(
                    indice -> costo[desde][indice])).orElseThrow();
            recorrido.add(proximo);
            pendientes.remove(Integer.valueOf(proximo));
            actual = proximo;
        }
        recorrido.add(extremos[1]);
        for (int pasada = 0; pasada < 32; pasada++) {
            double[] diferencias = new double[recorrido.size()];
            for (int i = 0; i < recorrido.size() - 1; i++) {
                int desde = recorrido.get(i);
                int hasta = recorrido.get(i + 1);
                diferencias[i + 1] = diferencias[i] + costo[hasta][desde] - costo[desde][hasta];
            }
            boolean mejoro = false;
            busqueda:
            for (int i = 0; i < recorrido.size() - 2; i++) {
                for (int j = i + 1; j < recorrido.size() - 1; j++) {
                    int antes = i == 0 ? extremos[0] : recorrido.get(i - 1);
                    int desde = recorrido.get(i);
                    int hasta = recorrido.get(j);
                    int despues = recorrido.get(j + 1);
                    double anterior = costo[antes][desde] + costo[hasta][despues];
                    double invertido = costo[antes][hasta] + costo[desde][despues]
                            + diferencias[j] - diferencias[i];
                    if (invertido + 1e-9 < anterior) {
                        java.util.Collections.reverse(recorrido.subList(i, j + 1));
                        mejoro = true;
                        break busqueda;
                    }
                }
            }
            if (!mejoro) break;
        }
        recorrido.removeLast();
        return recorrido.stream().mapToInt(Integer::intValue).toArray();
    }

    private double[][] matrizCostos(List<PuntoImportante> puntos) {
        List<Nodo> nodos = puntos.stream()
                .map(p -> grafo.nodoMasCercanoLonLat(p.lon(), p.lat())).toList();
        if (nodos.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalStateException("No hay nodos viales cerca de todos los paraderos.");
        }
        double[][] costo = new double[puntos.size()][puntos.size()];
        for (int i = 0; i < puntos.size(); i++) {
            var costosDesde = pathfinder.costosMinimosDesde(nodos.get(i));
            for (int j = 0; j < puntos.size(); j++) {
                if (nodos.get(i) == nodos.get(j)) continue;
                costo[i][j] = costosDesde.getOrDefault(nodos.get(j), Double.POSITIVE_INFINITY);
            }
        }
        return costo;
    }

}
