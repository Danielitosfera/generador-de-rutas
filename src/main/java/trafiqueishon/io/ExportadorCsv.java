package trafiqueishon.io;

import trafiqueishon.geo.Edge;
import trafiqueishon.geo.Nodo;
import trafiqueishon.ruta.Trayecto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ExportadorCsv {

    public void exportar(Trayecto trayecto, Path destino) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("orden,lon,lat,calle,highway,longitud_m\n");
        int i = 0;
        for (Edge e : trayecto.edges()) {
            Nodo n = e.origen();
            sb.append(++i).append(',')
                    .append(n.lon).append(',')
                    .append(n.lat).append(',')
                    .append(escape(e.name)).append(',')
                    .append(escape(e.highway)).append(',')
                    .append(String.format("%.2f", e.longitudMetros())).append('\n');
        }
        Files.writeString(destino, sb.toString());
    }

    private String escape(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}