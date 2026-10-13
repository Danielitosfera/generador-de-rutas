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
        int[] extremos = extremosMasLejanos(unicos);
        List<Integer> intermedios = new ArrayList<>();
        for (int i = 0; i < unicos.size(); i++) {
            if (i != extremos[0] && i != extremos[1]) intermedios.add(i);
        }
        int[] orden = intermedios.size() <= 15
                ? ordenExacto(unicos, extremos, intermedios)
                : ordenAproximado(unicos, extremos, intermedios);
        List<PuntoImportante> ordenados = new ArrayList<>(unicos.size());
        ordenados.add(unicos.get(extremos[0]));
        for (int indice : orden) ordenados.add(unicos.get(indice));
        ordenados.add(unicos.get(extremos[1]));
        return calcular(id, nombre, empresa, sentido, ordenados);
    }

    private int[] extremosMasLejanos(List<PuntoImportante> puntos) {
        int primero = 0, segundo = 1;
        double mayor = -1;
        for (int i = 0; i < puntos.size(); i++) {
            for (int j = i + 1; j < puntos.size(); j++) {
                double distancia = distanciaMetros(puntos.get(i), puntos.get(j));
                if (distancia > mayor) {
                    mayor = distancia;
                    primero = i;
                    segundo = j;
                }
            }
        }
        PuntoImportante a = puntos.get(primero);
        PuntoImportante b = puntos.get(segundo);
        if (a.tipo() == PuntoImportante.TipoPunto.DESTINO
                || a.tipo() == PuntoImportante.TipoPunto.TERMINAL) {
            int temporal = primero;
            primero = segundo;
            segundo = temporal;
        } else if (b.tipo() == PuntoImportante.TipoPunto.ORIGEN) {
            int temporal = primero;
            primero = segundo;
            segundo = temporal;
        }
        return new int[]{primero, segundo};
    }

    private int[] ordenExacto(List<PuntoImportante> puntos, int[] extremos,
                              List<Integer> intermedios) {
        int m = intermedios.size();
        if (m == 0) return new int[0];
        double[][] costo = matrizCostos(puntos);
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

    private int[] ordenAproximado(List<PuntoImportante> puntos, int[] extremos,
                                  List<Integer> intermedios) {
        double[][] costo = matrizCostos(puntos);
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
        boolean mejoro;
        do {
            mejoro = false;
            for (int i = 0; i < recorrido.size() - 2; i++) {
                for (int j = i + 1; j < recorrido.size() - 1; j++) {
                    int antes = i == 0 ? extremos[0] : recorrido.get(i - 1);
                    int desde = recorrido.get(i);
                    int hasta = recorrido.get(j);
                    int despues = recorrido.get(j + 1);
                    double anterior = costo[antes][desde] + costo[hasta][despues];
                    double invertido = costo[antes][hasta] + costo[desde][despues];
                    if (invertido + 1e-9 < anterior) {
                        java.util.Collections.reverse(recorrido.subList(i, j + 1));
                        mejoro = true;
                    }
                }
            }
        } while (mejoro);
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

    private static double distanciaMetros(PuntoImportante a, PuntoImportante b) {
        double lat1 = Math.toRadians(a.lat());
        double lat2 = Math.toRadians(b.lat());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.lon() - a.lon());
        double h = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * 6_371_000 * Math.asin(Math.sqrt(h));
    }
}
