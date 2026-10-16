<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: datos
 * Archivo: ExportadorPdf.java
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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
<<<<<<< HEAD
import org.apache.pdfbox.util.Matrix;
import trafiqueishon.geo.Edge;
import trafiqueishon.geo.Nodo;
import trafiqueishon.ruta.PuntoImportante;
=======
import trafiqueishon.geo.Edge;
import trafiqueishon.geo.Nodo;
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
<<<<<<< HEAD
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Exporta cada micro como una lámina de recorrido en PDF (horizontal, tamaño carta):
 * croquis blanco con calles grises y zonas punteadas, recorrido de partida en rojo y
 * de retorno en azul, nombre de zona en vertical, leyenda, panel derecho con las
 * paradas de ida y de vuelta, y marco verde con línea amarilla.
 *
 * <p>Se genera una página por micro: el par "· Ida" / "· Vuelta" de un mismo servicio
 * comparte página. Los colores del PDF son fijos (partida rojo, retorno azul) y no
 * dependen del color propio de cada ruta.</p>
 */
public final class ExportadorPdf {

    /** Zona rotulada en el croquis (posición del centro del nombre). */
    public record Zona(String nombre, double lon, double lat) {}

    private static final Color ROJO = new Color(222, 38, 38);
    private static final Color AZUL = new Color(28, 62, 190);
    private static final Color AZUL_SEPARADOR = new Color(0, 140, 255);
    private static final Color VERDE = new Color(46, 158, 68);
    private static final Color AMARILLO = new Color(255, 214, 0);
    private static final Color CALLE = new Color(150, 150, 150);
    private static final Color PUNTEADO = new Color(185, 185, 185);
    private static final Color NEGRO = Color.BLACK;

    private static final float MARGEN_PAGINA = 12;
    private static final float GROSOR_VERDE = 9;
    private static final float GROSOR_AMARILLO = 1.4f;
    private static final float PROPORCION_PANEL = 0.25f;
    private static final float DESFASE_RUTAS = 1.5f;
    private static final float CELDA_URBANA = 9;

    private List<Zona> zonas;

    /** Define los nombres de zona; si no se llama, solo se rotula JULIACA. */
    public void setZonas(List<Zona> zonas) {
        this.zonas = zonas == null ? null : List.copyOf(zonas);
    }

    public void exportar(List<RutaExportable> rutas, Path destino) throws IOException {
        if (rutas.isEmpty()) throw new IllegalArgumentException("Selecciona al menos una ruta.");
        List<Lamina> laminas = agrupar(rutas);
        try (PDDocument documento = new PDDocument()) {
            for (Lamina lamina : laminas) dibujarLamina(documento, lamina);
=======
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
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
            documento.save(destino.toFile());
        }
    }

<<<<<<< HEAD
    // ------------------------------------------------------------------ agrupación

    private record Lamina(String titulo, RutaExportable partida, RutaExportable retorno) {}

    private static final class Acumulador {
        private final String titulo;
        private RutaExportable partida;
        private RutaExportable retorno;

        private Acumulador(String titulo) { this.titulo = titulo; }
    }

    private List<Lamina> agrupar(List<RutaExportable> rutas) {
        Map<String, Acumulador> porClave = new LinkedHashMap<>();
        for (RutaExportable ruta : rutas) {
            boolean vuelta = esRetorno(ruta);
            String titulo = nombreBase(ruta.nombre());
            String clave = titulo.toLowerCase(Locale.ROOT);
            int repeticion = 0;
            while (true) {
                Acumulador acumulador = porClave.computeIfAbsent(clave + "#" + repeticion,
                        k -> new Acumulador(titulo));
                RutaExportable ocupada = vuelta ? acumulador.retorno : acumulador.partida;
                if (ocupada == null) {
                    if (vuelta) acumulador.retorno = ruta;
                    else acumulador.partida = ruta;
                    break;
                }
                repeticion++;
            }
        }
        List<Lamina> laminas = new ArrayList<>();
        for (Acumulador a : porClave.values()) {
            laminas.add(new Lamina(a.titulo, a.partida, a.retorno));
        }
        return laminas;
    }

    private static boolean esRetorno(RutaExportable ruta) {
        String sentido = ruta.trayecto().sentido();
        if ("RETORNO".equalsIgnoreCase(sentido)) return true;
        return ruta.nombre().matches("(?i).*\\b(vuelta|retorno)\\s*$");
    }

    private static String nombreBase(String nombre) {
        String base = nombre.replaceAll("(?i)\\s*[·\\-–:|]?\\s*(ida|vuelta|partida|retorno)\\s*$", "")
                .trim();
        return base.isBlank() ? nombre : base;
    }

    // ------------------------------------------------------------------ lámina

    private void dibujarLamina(PDDocument documento, Lamina lamina) throws IOException {
        PDPage hoja = new PDPage(new PDRectangle(PDRectangle.LETTER.getHeight(),
                PDRectangle.LETTER.getWidth()));
        documento.addPage(hoja);
        float ancho = hoja.getMediaBox().getWidth();
        float alto = hoja.getMediaBox().getHeight();
        float interior = MARGEN_PAGINA + GROSOR_VERDE + 6;
        float x0 = interior, y0 = interior, x1 = ancho - interior, y1 = alto - interior;
        float anchoPanel = (x1 - x0) * PROPORCION_PANEL;
        float panelX = x1 - anchoPanel;
        float mapaX1 = panelX - 9;

        try (PDPageContentStream cs = new PDPageContentStream(documento, hoja)) {
            dibujarMapa(cs, lamina, x0, y0, mapaX1, y1);
            dibujarSeparador(cs, panelX - 7, y0, y1);
            dibujarPanel(cs, lamina, panelX, y0, x1, y1);
            dibujarMarco(cs, ancho, alto);
        }
    }

    private void dibujarMarco(PDPageContentStream cs, float ancho, float alto) throws IOException {
        float m = MARGEN_PAGINA + GROSOR_VERDE / 2;
        cs.setStrokingColor(VERDE);
        cs.setLineWidth(GROSOR_VERDE);
        cs.addRect(m, m, ancho - 2 * m, alto - 2 * m);
        cs.stroke();
        float a = MARGEN_PAGINA + GROSOR_VERDE + 1.5f;
        cs.setStrokingColor(AMARILLO);
        cs.setLineWidth(GROSOR_AMARILLO);
        cs.addRect(a, a, ancho - 2 * a, alto - 2 * a);
        cs.stroke();
    }

    private void dibujarSeparador(PDPageContentStream cs, float x, float y0, float y1)
            throws IOException {
        cs.setStrokingColor(AZUL_SEPARADOR);
        cs.setLineWidth(5);
        cs.moveTo(x, y0);
        cs.lineTo(x, y1);
        cs.stroke();
    }

    // ------------------------------------------------------------------ panel de paradas

    private void dibujarPanel(PDPageContentStream cs, Lamina lamina, float x0, float y0, float x1,
                              float y1) throws IOException {
        List<String> partida = paradas(lamina.partida());
        List<String> retorno = paradas(lamina.retorno());
        float margen = 10;
        float anchoTexto = x1 - x0 - margen * 2 - 12;
        float alturaUtil = y1 - y0 - margen * 2;
        int lineas = partida.size() + retorno.size();
        float tamano = Math.max(5.5f, Math.min(9f, (alturaUtil - 56) / (Math.max(1, lineas) * 1.45f)));
        float paso = tamano * 1.45f;

        float y = y1 - margen - 11;
        y = titulo(cs, "RECORRIDO DE PARTIDA", x0 + margen, y);
        y = lista(cs, partida, x0 + margen, y, anchoTexto, tamano, paso, y0 + margen);
        y -= 14;
        y = titulo(cs, "RECORRIDO DE RETORNO", x0 + margen, y);
        lista(cs, retorno, x0 + margen, y, anchoTexto, tamano, paso, y0 + margen);
    }

    private float titulo(PDPageContentStream cs, String texto, float x, float y) throws IOException {
        cs.setNonStrokingColor(ROJO);
        texto(cs, PDType1Font.HELVETICA_BOLD, 11, texto, x, y);
        return y - 17;
    }

    private float lista(PDPageContentStream cs, List<String> nombres, float x, float y, float ancho,
                        float tamano, float paso, float limiteInferior) throws IOException {
        for (String nombre : nombres) {
            if (y < limiteInferior) {
                cs.setNonStrokingColor(NEGRO);
                texto(cs, PDType1Font.HELVETICA_BOLD, tamano, "...", x + 10, y);
                return y - paso;
            }
            cs.setNonStrokingColor(NEGRO);
            circulo(cs, x + 3, y + tamano * 0.33f, 2.1f);
            cs.fill();
            texto(cs, PDType1Font.HELVETICA_BOLD, tamano,
                    ajustar(PDType1Font.HELVETICA_BOLD, tamano, nombre, ancho), x + 11, y);
            y -= paso;
        }
        return y;
    }

    private List<String> paradas(RutaExportable ruta) {
        List<String> nombres = new ArrayList<>();
        if (ruta == null) return nombres;
        for (PuntoImportante punto : ruta.trayecto().puntos()) nombres.add(ascii(punto.nombre()));
        if (nombres.isEmpty()) {
            String anterior = null;
            for (Edge e : ruta.trayecto().edges()) {
                if (e.name == null || e.name.isBlank() || e.name.equals(anterior)) continue;
                nombres.add(ascii(e.name));
                anterior = e.name;
            }
        }
        return nombres;
    }

    // ------------------------------------------------------------------ mapa

    private void dibujarMapa(PDPageContentStream cs, Lamina lamina, float mx0, float my0,
                             float mx1, float my1) throws IOException {
        List<Nodo[]> ida = trazo(lamina.partida());
        List<Nodo[]> vuelta = trazo(lamina.retorno());
        double minLon = Double.POSITIVE_INFINITY, minLat = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
        double sumaLat = 0;
        long n = 0;
        List<Nodo> todos = new ArrayList<>();
        for (List<Nodo[]> tramos : List.of(ida, vuelta)) {
            for (Nodo[] par : tramos) {
                for (Nodo nodo : par) {
                    todos.add(nodo);
=======
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
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
                    minLon = Math.min(minLon, nodo.lon);
                    maxLon = Math.max(maxLon, nodo.lon);
                    minLat = Math.min(minLat, nodo.lat);
                    maxLat = Math.max(maxLat, nodo.lat);
<<<<<<< HEAD
                    sumaLat += nodo.lat;
                    n++;
                }
            }
        }
        cs.setNonStrokingColor(Color.WHITE);
        cs.addRect(mx0, my0, mx1 - mx0, my1 - my0);
        cs.fill();
        encabezado(cs, lamina.titulo(), mx0 + 10, my1 - 18);
        if (n == 0) {
            cs.setNonStrokingColor(NEGRO);
            texto(cs, PDType1Font.HELVETICA, 10, "No hay tramos viales para dibujar.", mx0 + 10, my1 - 44);
            dibujarLeyenda(cs, posicionLeyenda(List.of(), mx0, my0, mx1, my1));
            return;
        }

        double factorLon = Math.max(0.01, Math.cos(Math.toRadians(sumaLat / n)));
        double anchoGeo = Math.max(1e-9, (maxLon - minLon) * factorLon);
        double altoGeo = Math.max(1e-9, maxLat - minLat);
        double pad = 26;
        double espacioX = (mx1 - mx0) - 2 * pad;
        double espacioY = (my1 - my0) - 2 * pad - 14;
        double escala = Math.min(espacioX / anchoGeo, espacioY / altoGeo);
        double inicioX = mx0 + pad + (espacioX - anchoGeo * escala) / 2;
        double inicioY = my0 + pad + (espacioY - altoGeo * escala) / 2;
        Proy proy = new Proy(minLon, minLat, factorLon, escala, inicioX, inicioY);

        cs.saveGraphicsState();
        cs.addRect(mx0, my0, mx1 - mx0, my1 - my0);
        cs.clip();
        List<Edge> calles = callesVisibles(todos, proy, mx0, my0, mx1, my1);
        double[] centro = dibujarZonasUrbanas(cs, calles, proy, mx0, my0, mx1, my1);
        dibujarCalles(cs, calles, proy);
        dibujarNombresZona(cs, proy, centro, mx0, my0, mx1, my1);
        boolean ambas = !ida.isEmpty() && !vuelta.isEmpty();
        dibujarRecorrido(cs, vuelta, proy, AZUL, ambas ? DESFASE_RUTAS : 0);
        dibujarRecorrido(cs, ida, proy, ROJO, ambas ? DESFASE_RUTAS : 0);
        dibujarExtremos(cs, ida.isEmpty() ? vuelta : ida, proy);
        cs.restoreGraphicsState();

        List<float[]> puntosRuta = new ArrayList<>();
        for (List<Nodo[]> tramos : List.of(ida, vuelta)) {
            for (Nodo[] par : tramos) {
                float ax = proy.x(par[0]), ay = proy.y(par[0]), bx = proy.x(par[1]), by = proy.y(par[1]);
                puntosRuta.add(new float[]{ax, ay});
                puntosRuta.add(new float[]{(ax + bx) / 2, (ay + by) / 2});
                puntosRuta.add(new float[]{bx, by});
            }
        }
        dibujarLeyenda(cs, posicionLeyenda(puntosRuta, mx0, my0, mx1, my1));
    }

    /** Pares origen-destino de los tramos de una ruta, en orden. */
    private List<Nodo[]> trazo(RutaExportable ruta) {
        List<Nodo[]> pares = new ArrayList<>();
        if (ruta == null) return pares;
        for (Edge e : ruta.trayecto().edges()) pares.add(new Nodo[]{e.origen(), e.destino()});
        return pares;
    }

    private record Proy(double minLon, double minLat, double factorLon, double escala,
                        double inicioX, double inicioY) {
        float x(Nodo n) { return (float) (inicioX + (n.lon - minLon) * factorLon * escala); }

        float y(Nodo n) { return (float) (inicioY + (n.lat - minLat) * escala); }
    }

    /** Calles de la red alcanzables desde la ruta que caen dentro de la lámina. */
    private List<Edge> callesVisibles(List<Nodo> semillas, Proy p, float mx0, float my0, float mx1,
                                      float my1) {
        float holgura = 30;
        Set<Nodo> visitados = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Edge> aristas = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Nodo> cola = new ArrayDeque<>();
        for (Nodo s : semillas) if (visitados.add(s)) cola.add(s);
        while (!cola.isEmpty()) {
            Nodo nodo = cola.poll();
            for (Edge e : nodo.salientes) {
                aristas.add(e);
                encolar(e.destino(), p, mx0 - holgura, my0 - holgura, mx1 + holgura, my1 + holgura,
                        visitados, cola);
            }
            for (Edge e : nodo.entrantes) {
                aristas.add(e);
                encolar(e.origen(), p, mx0 - holgura, my0 - holgura, mx1 + holgura, my1 + holgura,
                        visitados, cola);
            }
        }
        List<Edge> unicas = new ArrayList<>();
        for (Edge e : aristas) {
            if (e.origen().id > e.destino().id && tieneInversa(e)) continue;
            unicas.add(e);
        }
        return unicas;
    }

    private void encolar(Nodo vecino, Proy p, float x0, float y0, float x1, float y1,
                         Set<Nodo> visitados, Deque<Nodo> cola) {
        float vx = p.x(vecino), vy = p.y(vecino);
        if (vx < x0 || vx > x1 || vy < y0 || vy > y1) return;
        if (visitados.add(vecino)) cola.add(vecino);
    }

    private boolean tieneInversa(Edge e) {
        for (Edge otra : e.destino().salientes) if (otra.destino() == e.origen()) return true;
        return false;
    }

    private void dibujarCalles(PDPageContentStream cs, List<Edge> calles, Proy p) throws IOException {
        cs.setStrokingColor(CALLE);
        cs.setLineWidth(0.35f);
        for (Edge e : calles) {
            cs.moveTo(p.x(e.origen()), p.y(e.origen()));
            cs.lineTo(p.x(e.destino()), p.y(e.destino()));
        }
        cs.stroke();
    }

    /** Puntea las celdas con mucha calle; devuelve el centro de masa (x, y) o null. */
    private double[] dibujarZonasUrbanas(PDPageContentStream cs, List<Edge> calles, Proy p,
                                         float mx0, float my0, float mx1, float my1)
            throws IOException {
        int cols = Math.max(1, (int) Math.ceil((mx1 - mx0) / CELDA_URBANA));
        int filas = Math.max(1, (int) Math.ceil((my1 - my0) / CELDA_URBANA));
        double[] largo = new double[cols * filas];
        for (Edge e : calles) {
            double ax = p.x(e.origen()), ay = p.y(e.origen());
            double bx = p.x(e.destino()), by = p.y(e.destino());
            int c = (int) Math.floor(((ax + bx) / 2 - mx0) / CELDA_URBANA);
            int f = (int) Math.floor(((ay + by) / 2 - my0) / CELDA_URBANA);
            if (c < 0 || c >= cols || f < 0 || f >= filas) continue;
            largo[f * cols + c] += Math.hypot(ax - bx, ay - by);
        }
        double[] suma = new double[cols * filas];
        double maximo = 0;
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < cols; c++) {
                double s = 0;
                for (int df = -1; df <= 1; df++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int ff = f + df, cc = c + dc;
                        if (ff >= 0 && ff < filas && cc >= 0 && cc < cols) s += largo[ff * cols + cc];
                    }
                }
                suma[f * cols + c] = s;
                maximo = Math.max(maximo, s);
            }
        }
        if (maximo <= 0) return null;
        double umbral = maximo * 0.12;
        double sx = 0, sy = 0, peso = 0;
        cs.setNonStrokingColor(PUNTEADO);
        float paso = 3.6f;
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < cols; c++) {
                if (suma[f * cols + c] < umbral) continue;
                float cx0 = mx0 + c * CELDA_URBANA, cy0 = my0 + f * CELDA_URBANA;
                for (float dy = paso / 2; dy < CELDA_URBANA; dy += paso) {
                    for (float dx = paso / 2; dx < CELDA_URBANA; dx += paso) {
                        cs.addRect(cx0 + dx - 0.4f, cy0 + dy - 0.4f, 0.8f, 0.8f);
                    }
                }
                double w = largo[f * cols + c];
                sx += (cx0 + CELDA_URBANA / 2) * w;
                sy += (cy0 + CELDA_URBANA / 2) * w;
                peso += w;
            }
        }
        cs.fill();
        return peso > 0 ? new double[]{sx / peso, sy / peso} : null;
    }

    private void dibujarNombresZona(PDPageContentStream cs, Proy p, double[] centro, float mx0,
                                    float my0, float mx1, float my1) throws IOException {
        List<double[]> posiciones = new ArrayList<>();
        List<String> nombres = new ArrayList<>();
        if (zonas != null) {
            for (Zona z : zonas) {
                double x = p.inicioX() + (z.lon() - p.minLon()) * p.factorLon() * p.escala();
                double y = p.inicioY() + (z.lat() - p.minLat()) * p.escala();
                posiciones.add(new double[]{x, y});
                nombres.add(z.nombre());
            }
        } else if (centro != null) {
            posiciones.add(centro);
            nombres.add("JULIACA");
        }
        for (int i = 0; i < nombres.size(); i++) {
            double[] pos = posiciones.get(i);
            if (pos[0] < mx0 || pos[0] > mx1 || pos[1] < my0 || pos[1] > my1) continue;
            String texto = ascii(nombres.get(i)).toUpperCase(Locale.ROOT);
            float tam = 15;
            float ancho = PDType1Font.HELVETICA_BOLD.getStringWidth(texto) / 1000 * tam;
            cs.setNonStrokingColor(ROJO);
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, tam);
            cs.setTextMatrix(Matrix.getRotateInstance(Math.PI / 2, (float) pos[0] + tam * 0.35f,
                    (float) pos[1] - ancho / 2));
            cs.showText(texto);
            cs.endText();
        }
    }

    private void dibujarRecorrido(PDPageContentStream cs, List<Nodo[]> tramos, Proy p, Color color,
                                  float desfase) throws IOException {
        if (tramos.isEmpty()) return;
        List<float[]> puntos = new ArrayList<>();
        Nodo ultimo = null;
        for (Nodo[] par : tramos) {
            if (ultimo != par[0]) {
                dibujarPolilinea(cs, puntos, color, desfase);
                puntos.clear();
                puntos.add(new float[]{p.x(par[0]), p.y(par[0])});
            }
            puntos.add(new float[]{p.x(par[1]), p.y(par[1])});
            ultimo = par[1];
        }
        dibujarPolilinea(cs, puntos, color, desfase);
    }

    private void dibujarPolilinea(PDPageContentStream cs, List<float[]> pts, Color color,
                                  float desfase) throws IOException {
        if (pts.size() < 2) return;
        cs.setStrokingColor(color);
        cs.setLineWidth(2.2f);
        cs.setLineCapStyle(1);
        cs.setLineJoinStyle(1);
        for (int i = 0; i < pts.size(); i++) {
            float[] a = pts.get(Math.max(0, i - 1));
            float[] b = pts.get(Math.min(pts.size() - 1, i + 1));
            float dx = b[0] - a[0], dy = b[1] - a[1];
            float longitud = (float) Math.hypot(dx, dy);
            float nx = longitud == 0 ? 0 : -dy / longitud * desfase;
            float ny = longitud == 0 ? 0 : dx / longitud * desfase;
            float x = pts.get(i)[0] + nx, y = pts.get(i)[1] + ny;
            if (i == 0) cs.moveTo(x, y);
            else cs.lineTo(x, y);
        }
        cs.stroke();
    }

    private void dibujarExtremos(PDPageContentStream cs, List<Nodo[]> tramos, Proy p)
            throws IOException {
        if (tramos.isEmpty()) return;
        Nodo inicio = tramos.getFirst()[0];
        Nodo fin = tramos.getLast()[1];
        extremo(cs, p.x(inicio), p.y(inicio), "INICIO");
        extremo(cs, p.x(fin), p.y(fin), "FINAL");
    }

    private void extremo(PDPageContentStream cs, float x, float y, String etiqueta)
            throws IOException {
        cs.setNonStrokingColor(Color.WHITE);
        circulo(cs, x, y, 4);
        cs.fill();
        cs.setStrokingColor(ROJO);
        cs.setLineWidth(1.6f);
        circulo(cs, x, y, 4);
        cs.stroke();
        float tam = 6.5f;
        float ancho = PDType1Font.HELVETICA_BOLD.getStringWidth(etiqueta) / 1000 * tam;
        cs.setNonStrokingColor(Color.WHITE);
        cs.addRect(x + 6, y + 4, ancho + 3, tam + 2);
        cs.fill();
        cs.setNonStrokingColor(ROJO);
        texto(cs, PDType1Font.HELVETICA_BOLD, tam, etiqueta, x + 7.5f, y + 5.5f);
    }

    private void encabezado(PDPageContentStream cs, String titulo, float x, float y)
            throws IOException {
        String texto = ascii(titulo).toUpperCase(Locale.ROOT);
        float ancho = PDType1Font.HELVETICA_BOLD.getStringWidth(texto) / 1000 * 13;
        cs.setNonStrokingColor(Color.WHITE);
        cs.addRect(x - 4, y - 5, ancho + 8, 20);
        cs.fill();
        cs.setNonStrokingColor(AZUL);
        texto(cs, PDType1Font.HELVETICA_BOLD, 13, texto, x, y);
    }

    // ------------------------------------------------------------------ leyenda

    /** Coloca la leyenda en la primera esquina (abajo-derecha, abajo-izquierda, arriba...) sin ruta. */
    private float[] posicionLeyenda(List<float[]> ruta, float mx0, float my0, float mx1, float my1) {
        float ancho = 188, alto = 64, m = 8;
        float[][] candidatas = {
                {mx1 - ancho - m, my0 + m}, {mx0 + m, my0 + m},
                {mx1 - ancho - m, my1 - alto - m - 22}, {mx0 + m, my1 - alto - m - 22}};
        for (float[] c : candidatas) {
            boolean libre = true;
            for (float[] p : ruta) {
                if (p[0] >= c[0] - 6 && p[0] <= c[0] + ancho + 6
                        && p[1] >= c[1] - 6 && p[1] <= c[1] + alto + 6) {
                    libre = false;
                    break;
                }
            }
            if (libre) return c;
        }
        return candidatas[0];
    }

    private void dibujarLeyenda(PDPageContentStream cs, float[] pos) throws IOException {
        float ancho = 188, alto = 64;
        float x = pos[0], y = pos[1];
        cs.setNonStrokingColor(Color.WHITE);
        cs.addRect(x, y, ancho, alto);
        cs.fill();
        cs.setStrokingColor(NEGRO);
        cs.setLineWidth(0.8f);
        cs.addRect(x, y, ancho, alto);
        cs.stroke();

        globo(cs, x + 22, y + alto / 2, 17);
        cs.setNonStrokingColor(NEGRO);
        texto(cs, PDType1Font.HELVETICA_BOLD, 7.5f, "LEYENDA", x + 46, y + alto - 14);
        entradaLeyenda(cs, ROJO, "RECORRIDO DE PARTIDA", x + 46, y + alto - 29);
        entradaLeyenda(cs, AZUL, "RECORRIDO DE RETORNO", x + 46, y + alto - 43);
        cs.setNonStrokingColor(NEGRO);
        circulo(cs, x + 49, y + alto - 55.7f, 1.8f);
        cs.fill();
        cs.setStrokingColor(ROJO);
        cs.setLineWidth(1.2f);
        circulo(cs, x + 65.5f, y + alto - 55.7f, 3);
        cs.stroke();
        texto(cs, PDType1Font.HELVETICA_BOLD, 6.5f, "INICIO / FINAL", x + 79, y + alto - 58);
    }

    private void entradaLeyenda(PDPageContentStream cs, Color color, String texto, float x, float y)
            throws IOException {
        cs.setNonStrokingColor(NEGRO);
        circulo(cs, x + 3, y + 2.3f, 1.8f);
        cs.fill();
        cs.setStrokingColor(color);
        cs.setLineWidth(2.4f);
        cs.moveTo(x + 9, y + 2.3f);
        cs.lineTo(x + 28, y + 2.3f);
        cs.stroke();
        cs.setNonStrokingColor(NEGRO);
        texto(cs, PDType1Font.HELVETICA_BOLD, 6.5f, texto, x + 33, y);
    }

    /** Globo terráqueo en blanco y negro. */
    private void globo(PDPageContentStream cs, float cx, float cy, float r) throws IOException {
        cs.setNonStrokingColor(Color.WHITE);
        circulo(cs, cx, cy, r);
        cs.fill();
        cs.setStrokingColor(NEGRO);
        cs.setLineWidth(0.9f);
        circulo(cs, cx, cy, r);
        cs.stroke();
        elipse(cs, cx, cy, r * 0.45f, r);
        cs.stroke();
        cs.moveTo(cx, cy - r);
        cs.lineTo(cx, cy + r);
        cs.moveTo(cx - r, cy);
        cs.lineTo(cx + r, cy);
        float m = (float) Math.sqrt(r * r - (r * 0.5f) * (r * 0.5f));
        cs.moveTo(cx - m, cy + r * 0.5f);
        cs.lineTo(cx + m, cy + r * 0.5f);
        cs.moveTo(cx - m, cy - r * 0.5f);
        cs.lineTo(cx + m, cy - r * 0.5f);
        cs.stroke();
        cs.setNonStrokingColor(NEGRO);
        cs.moveTo(cx - r * 0.6f, cy + r * 0.5f);
        cs.lineTo(cx - r * 0.2f, cy + r * 0.6f);
        cs.lineTo(cx - r * 0.1f, cy + r * 0.1f);
        cs.lineTo(cx - r * 0.5f, cy - r * 0.1f);
        cs.closePath();
        cs.fill();
        cs.moveTo(cx + r * 0.15f, cy - r * 0.1f);
        cs.lineTo(cx + r * 0.55f, cy - r * 0.2f);
        cs.lineTo(cx + r * 0.4f, cy - r * 0.6f);
        cs.lineTo(cx + r * 0.2f, cy - r * 0.5f);
        cs.closePath();
        cs.fill();
    }

    // ------------------------------------------------------------------ primitivas

    private void circulo(PDPageContentStream cs, float cx, float cy, float r) throws IOException {
        elipse(cs, cx, cy, r, r);
    }

    private void elipse(PDPageContentStream cs, float cx, float cy, float rx, float ry)
            throws IOException {
        float k = 0.5522848f;
        cs.moveTo(cx + rx, cy);
        cs.curveTo(cx + rx, cy + ry * k, cx + rx * k, cy + ry, cx, cy + ry);
        cs.curveTo(cx - rx * k, cy + ry, cx - rx, cy + ry * k, cx - rx, cy);
        cs.curveTo(cx - rx, cy - ry * k, cx - rx * k, cy - ry, cx, cy - ry);
        cs.curveTo(cx + rx * k, cy - ry, cx + rx, cy - ry * k, cx + rx, cy);
        cs.closePath();
    }

    private void texto(PDPageContentStream cs, PDType1Font fuente, float tamano, String texto,
                       float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(fuente, tamano);
        cs.newLineAtOffset(x, y);
        cs.showText(ascii(texto));
        cs.endText();
    }

    private String ajustar(PDType1Font fuente, float tamano, String texto, float maximo)
            throws IOException {
        String actual = ascii(texto).toUpperCase(Locale.ROOT);
        while (actual.length() > 4 && fuente.getStringWidth(actual) / 1000 * tamano > maximo) {
            actual = actual.substring(0, actual.length() - 4) + "...";
        }
        return actual;
=======
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
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
    }

    private String ascii(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }
<<<<<<< HEAD
=======

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
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
}
