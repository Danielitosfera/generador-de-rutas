/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: datos
 * Archivo: ExportadorTxt.java
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

import trafiqueishon.geo.Edge;
import trafiqueishon.geo.Nodo;
import trafiqueishon.ruta.PuntoImportante;
import trafiqueishon.ruta.Trayecto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Exporta el recorrido de una ruta como texto plano: cada punto por el que
 * pasa, en orden y con sus coordenadas (x = longitud, y = latitud).
 */
public final class ExportadorTxt {

    private static final String DOBLE = "=".repeat(44);
    private static final String SIMPLE = "-".repeat(44);
    private static final int ANCHO_NOMBRE = 26;

    /** Exporta un recorrido de partida y, opcionalmente, uno de retorno. */
    public void exportar(Trayecto partida, Trayecto retorno, Path destino) throws IOException {
        Objects.requireNonNull(partida, "El recorrido de partida es obligatorio.");
        List<RutaExportable> rutas = new ArrayList<>();
        rutas.add(new RutaExportable(partida.nombre(), "#000000", partida));
        if (retorno != null) rutas.add(new RutaExportable(retorno.nombre(), "#000000", retorno));
        exportar(rutas, destino);
    }

    public void exportar(List<RutaExportable> rutas, Path destino) throws IOException {
        if (rutas == null || rutas.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos una ruta.");
        }
        Files.writeString(destino, generar(rutas), StandardCharsets.UTF_8);
    }

    /** Devuelve el texto del informe sin escribirlo a disco. */
    public String generar(List<RutaExportable> rutas) {
        StringBuilder sb = new StringBuilder();
        sb.append(DOBLE).append('\n');
        sb.append("TRAFIQUEISHON - RECORRIDO DE RUTA\n");
        sb.append(DOBLE).append('\n');
        for (int i = 0; i < rutas.size(); i++) {
            if (i > 0) sb.append(SIMPLE).append('\n');
            agregarRecorrido(sb, rutas.get(i));
        }
        sb.append(DOBLE).append('\n');
        return sb.toString();
    }

    private void agregarRecorrido(StringBuilder sb, RutaExportable ruta) {
        Trayecto trayecto = ruta.trayecto();
        boolean retorno = "RETORNO".equalsIgnoreCase(trayecto.sentido());
        sb.append(retorno ? "RECORRIDO DE RETORNO" : "RECORRIDO DE PARTIDA").append('\n');
        sb.append("RUTA: ").append(ruta.nombre().toUpperCase(Locale.ROOT)).append('\n');

        List<String[]> filas = puntos(trayecto);
        int n = 0;
        for (String[] fila : filas) {
            sb.append(String.format(Locale.ROOT, "%d. %-" + ANCHO_NOMBRE + "s (x=%s, y=%s)%n",
                    ++n, fila[0], fila[1], fila[2]));
        }
        sb.append("Total: ").append(n).append(" puntos\n");
    }

    /**
     * Puntos del recorrido en orden. Usa los puntos nombrados del trayecto; si
     * no los tiene, recorre los nodos de sus tramos con el nombre de la calle.
     */
    private List<String[]> puntos(Trayecto trayecto) {
        List<String[]> filas = new ArrayList<>();
        if (!trayecto.puntos().isEmpty()) {
            for (PuntoImportante p : trayecto.puntos()) {
                filas.add(new String[]{p.nombre().toUpperCase(Locale.ROOT),
                        coordenada(p.lon()), coordenada(p.lat())});
            }
            return filas;
        }
        List<Edge> edges = trayecto.edges();
        if (edges.isEmpty()) return filas;
        Nodo primero = edges.getFirst().origen();
        filas.add(fila(nombreCalle(edges.getFirst()), primero));
        for (Edge e : edges) filas.add(fila(nombreCalle(e), e.destino()));
        return filas;
    }

    private static String[] fila(String nombre, Nodo nodo) {
        return new String[]{nombre, coordenada(nodo.lon), coordenada(nodo.lat)};
    }

    private static String nombreCalle(Edge e) {
        return e.name == null || e.name.isBlank() ? "SIN NOMBRE" : e.name.toUpperCase(Locale.ROOT);
    }

    private static String coordenada(double valor) {
        return String.format(Locale.ROOT, "%.5f", valor);
    }
}
