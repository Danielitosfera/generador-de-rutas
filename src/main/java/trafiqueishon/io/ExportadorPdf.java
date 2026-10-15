package trafiqueishon.io;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import trafiqueishon.geo.Edge;
import trafiqueishon.geo.Nodo;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;

public final class ExportadorPdf {
    private static final int RUTAS_POR_PAGINA = 8;
    private static final float MARGEN = 38;

    public void exportar(List<RutaExportable> rutas, Path destino) throws IOException {
        if (rutas.isEmpty()) throw new IllegalArgumentException("Selecciona al menos una ruta.");
        try (PDDocument documento = new PDDocument()) {
            int paginas = (rutas.size() + RUTAS_POR_PAGINA - 1) / RUTAS_POR_PAGINA;
            for (int pagina = 0; pagina < paginas; pagina++) {
                int desde = pagina * RUTAS_POR_PAGINA;
                List<RutaExportable> lote = rutas.subList(desde,
                        Math.min(rutas.size(), desde + RUTAS_POR_PAGINA));
                dibujarPagina(documento, lote, desde, pagina + 1, paginas);
            }
            documento.save(destino.toFile());
        }
    }

    private void dibujarPagina(PDDocument documento, List<RutaExportable> rutas, int offset,
                               int pagina, int paginas) throws IOException {
        PDPage hoja = new PDPage(new PDRectangle(PDRectangle.LETTER.getHeight(),
                PDRectangle.LETTER.getWidth()));
        documento.addPage(hoja);
        try (PDPageContentStream contenido = new PDPageContentStream(documento, hoja)) {
            float ancho = hoja.getMediaBox().getWidth();
            float alto = hoja.getMediaBox().getHeight();
            texto(contenido, "Rutas de micros - Trafiqueishon", MARGEN, alto - 34, 16);
            texto(contenido, "Pagina " + pagina + " de " + paginas, ancho - 115,
                    alto - 32, 9);

            float leyendaY = alto - 59;
            for (int i = 0; i < rutas.size(); i++) {
                RutaExportable ruta = rutas.get(i);
                float x = MARGEN + (i % 2) * ((ancho - 2 * MARGEN) / 2);
                float y = leyendaY - (i / 2) * 30;
                contenido.setStrokingColor(color(ruta.color()));
                contenido.setLineWidth(3);
                contenido.moveTo(x, y + 3);
                contenido.lineTo(x + 20, y + 3);
                contenido.stroke();
                texto(contenido, truncar(ruta.nombre(), 44), x + 27, y, 9);
                texto(contenido, String.format(java.util.Locale.ROOT, "%.2f km | %.1f min",
                        ruta.trayecto().distanciaMetros() / 1000.0,
                        ruta.trayecto().tiempoSegundos() / 60.0), x + 27, y - 11, 7);
            }
            float markersY = leyendaY - ((rutas.size() + 1) / 2) * 30 - 2;
            contenido.setNonStrokingColor(new Color(57, 255, 20));
            contenido.addRect(MARGEN, markersY, 7, 7);
            contenido.fill();
            texto(contenido, "Inicio", MARGEN + 11, markersY + 1, 8);
            contenido.setNonStrokingColor(new Color(255, 59, 92));
            contenido.addRect(MARGEN + 65, markersY, 7, 7);
            contenido.fill();
            texto(contenido, "Final", MARGEN + 76, markersY + 1, 8);

            float mapaMinY = MARGEN + 18;
            float mapaMaxY = markersY - 15;
            dibujarMapa(contenido, rutas, ancho, mapaMinY, mapaMaxY);
            texto(contenido, "Rutas seleccionadas: " + (offset + 1) + "-"
                    + (offset + rutas.size()), MARGEN, 19, 8);
        }
    }

    private void dibujarMapa(PDPageContentStream contenido, List<RutaExportable> rutas,
                             float anchoPagina, float minY, float maxY) throws IOException {
        double minLon = Double.POSITIVE_INFINITY, minLat = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
        double latitudMedia = 0;
        long coordenadas = 0;
        for (RutaExportable ruta : rutas) {
            for (Edge edge : ruta.trayecto().edges()) {
                Nodo[] nodos = {edge.origen(), edge.destino()};
                for (Nodo nodo : nodos) {
                    minLon = Math.min(minLon, nodo.lon);
                    maxLon = Math.max(maxLon, nodo.lon);
                    minLat = Math.min(minLat, nodo.lat);
                    maxLat = Math.max(maxLat, nodo.lat);
                    latitudMedia += nodo.lat;
                    coordenadas++;
                }
            }
        }
        if (coordenadas == 0) {
            texto(contenido, "No hay tramos viales para dibujar.", MARGEN, maxY - 20, 10);
            return;
        }
        double factorLon = Math.max(0.01, Math.cos(Math.toRadians(latitudMedia / coordenadas)));
        double anchoGeo = Math.max(1e-9, (maxLon - minLon) * factorLon);
        double altoGeo = Math.max(1e-9, maxLat - minLat);
        double espacioX = anchoPagina - 2 * MARGEN;
        double espacioY = Math.max(20, maxY - minY);
        double escala = Math.min(espacioX / anchoGeo, espacioY / altoGeo);
        double dibujoAncho = anchoGeo * escala;
        double dibujoAlto = altoGeo * escala;
        double inicioX = (anchoPagina - dibujoAncho) / 2;
        double inicioY = minY + (espacioY - dibujoAlto) / 2;

        for (RutaExportable ruta : rutas) {
            List<Edge> edges = ruta.trayecto().edges();
            if (edges.isEmpty()) continue;
            contenido.setStrokingColor(color(ruta.color()));
            contenido.setLineWidth(2.4f);
            boolean segmentoAbierto = false;
            Nodo anterior = null;
            for (Edge edge : edges) {
                Nodo origen = edge.origen();
                Nodo destino = edge.destino();
                if (!segmentoAbierto || anterior != origen) {
                    if (segmentoAbierto) contenido.stroke();
                    contenido.moveTo(x(origen, minLon, factorLon, escala, inicioX),
                            y(origen, minLat, escala, inicioY));
                    segmentoAbierto = true;
                }
                contenido.lineTo(x(destino, minLon, factorLon, escala, inicioX),
                        y(destino, minLat, escala, inicioY));
                anterior = destino;
            }
            if (segmentoAbierto) contenido.stroke();
            dibujarMarcador(contenido, edges.getFirst().origen(), minLon, minLat,
                    factorLon, escala, inicioX, inicioY, new Color(57, 255, 20));
            dibujarMarcador(contenido, edges.getLast().destino(), minLon, minLat,
                    factorLon, escala, inicioX, inicioY, new Color(255, 59, 92));
        }
    }

    private void dibujarMarcador(PDPageContentStream contenido, Nodo nodo, double minLon,
                                 double minLat, double factorLon, double escala, double inicioX,
                                 double inicioY, Color color) throws IOException {
        contenido.setNonStrokingColor(color);
        contenido.addRect(x(nodo, minLon, factorLon, escala, inicioX) - 2,
                y(nodo, minLat, escala, inicioY) - 2, 4, 4);
        contenido.fill();
    }

    private float x(Nodo nodo, double minLon, double factorLon, double escala, double inicioX) {
        return (float) (inicioX + (nodo.lon - minLon) * factorLon * escala);
    }

    private float y(Nodo nodo, double minLat, double escala, double inicioY) {
        return (float) (inicioY + (nodo.lat - minLat) * escala);
    }

    private void texto(PDPageContentStream contenido, String texto, float x, float y, int tamano)
            throws IOException {
        contenido.beginText();
        contenido.setFont(PDType1Font.HELVETICA, tamano);
        contenido.newLineAtOffset(x, y);
        contenido.showText(ascii(texto));
        contenido.endText();
    }

    private String ascii(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    private String truncar(String texto, int maximo) {
        String seguro = ascii(texto);
        return seguro.length() <= maximo ? seguro : seguro.substring(0, maximo - 3) + "...";
    }

    private Color color(String hex) {
        try {
            return Color.decode(hex);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Color de ruta inválido: " + hex, ex);
        }
    }
}
