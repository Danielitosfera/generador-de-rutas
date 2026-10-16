<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: datos
 * Archivo: ExportadorCsv.java
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
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
package trafiqueishon.io;

import trafiqueishon.geo.Edge;
import trafiqueishon.geo.Nodo;
<<<<<<< HEAD
import trafiqueishon.ruta.PaletaColores;
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
import trafiqueishon.ruta.Trayecto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class ExportadorCsv {

    public void exportar(Trayecto trayecto, Path destino) throws IOException {
<<<<<<< HEAD
        exportar(List.of(new RutaExportable(trayecto.nombre(), PaletaColores.COLOR_RUTA_IDA, trayecto)), destino);
=======
        exportar(List.of(new RutaExportable(trayecto.nombre(), "#00A8FF", trayecto)), destino);
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
    }

    public void exportar(List<RutaExportable> rutas, Path destino) throws IOException {
        if (rutas.isEmpty()) throw new IllegalArgumentException("Selecciona al menos una ruta.");
        StringBuilder sb = new StringBuilder();
        sb.append("ruta,sentido,color,orden,lon,lat,calle,highway,longitud_m\n");
        for (RutaExportable ruta : rutas) {
            int i = 0;
            for (Edge e : ruta.trayecto().edges()) {
                Nodo n = e.origen();
                sb.append(escape(ruta.nombre())).append(',')
                        .append(escape(ruta.trayecto().sentido())).append(',')
                        .append(escape(ruta.color())).append(',')
                        .append(++i).append(',')
                        .append(n.lon).append(',')
                        .append(n.lat).append(',')
                        .append(escape(e.name)).append(',')
                        .append(escape(e.highway)).append(',')
                        .append(String.format(Locale.ROOT, "%.2f", e.longitudMetros()))
                        .append('\n');
            }
            if (!ruta.trayecto().edges().isEmpty()) {
                Nodo ultimo = ruta.trayecto().edges().getLast().destino();
                sb.append(escape(ruta.nombre())).append(',')
                        .append(escape(ruta.trayecto().sentido())).append(',')
                        .append(escape(ruta.color())).append(',')
                        .append(++i).append(',')
                        .append(ultimo.lon).append(',')
                        .append(ultimo.lat).append(",,,0\n");
            }
        }
        Files.writeString(destino, sb.toString());
    }

    private String escape(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}