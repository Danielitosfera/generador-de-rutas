/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: datos
 * Archivo: ExportadorGpx.java
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
import trafiqueishon.ruta.PaletaColores;
import trafiqueishon.ruta.Trayecto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ExportadorGpx {

    public void exportar(Trayecto trayecto, Path destino) throws IOException {
        exportar(List.of(new RutaExportable(trayecto.nombre(), PaletaColores.COLOR_RUTA_IDA, trayecto)), destino);
    }

    public void exportar(List<RutaExportable> rutas, Path destino) throws IOException {
        if (rutas.isEmpty()) throw new IllegalArgumentException("Selecciona al menos una ruta.");
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<gpx version=\"1.1\" creator=\"Trafiqueishon\" ");
        sb.append("xmlns=\"http://www.topografix.com/GPX/1/1\">\n");
        for (RutaExportable ruta : rutas) {
            sb.append("  <trk>\n");
            sb.append("    <name>").append(escape(ruta.nombre())).append("</name>\n");
            sb.append("    <trkseg>\n");
            for (Edge e : ruta.trayecto().edges()) {
                Nodo n = e.origen();
                sb.append("      <trkpt lat=\"").append(n.lat)
                        .append("\" lon=\"").append(n.lon).append("\"/>\n");
            }
            if (!ruta.trayecto().edges().isEmpty()) {
                Nodo ultimo = ruta.trayecto().edges().getLast().destino();
                sb.append("      <trkpt lat=\"").append(ultimo.lat)
                        .append("\" lon=\"").append(ultimo.lon).append("\"/>\n");
            }
            sb.append("    </trkseg>\n");
            sb.append("  </trk>\n");
        }
        sb.append("</gpx>\n");
        Files.writeString(destino, sb.toString());
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}