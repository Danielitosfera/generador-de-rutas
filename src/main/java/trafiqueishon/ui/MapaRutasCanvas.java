<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: interfaz
 * Archivo: MapaRutasCanvas.java
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
 * @author Luis Yujra (LY)
 */
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
package trafiqueishon.ui;

import javafx.application.Platform;
import javafx.animation.AnimationTimer;
<<<<<<< HEAD
import javafx.geometry.VPos;
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
<<<<<<< HEAD
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;
import trafiqueishon.geo.*;
import trafiqueishon.ruta.PaletaColores;
=======
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;
import trafiqueishon.geo.*;
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
import trafiqueishon.ruta.PuntoImportante;
import trafiqueishon.ruta.Trayecto;
import trafiqueishon.simulacion.EventoTrafico;
import trafiqueishon.simulacion.PosicionVehiculo;
import trafiqueishon.simulacion.ServicioMicro;
import trafiqueishon.simulacion.TipoEvento;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapaRutasCanvas extends Pane {
    private static final Logger LOG = Logger.getLogger(MapaRutasCanvas.class.getName());
    private static final long INTERVALO_RENDER_NANOS = 1_000_000_000L / 24;
<<<<<<< HEAD
    private Color fondo = Color.web("#181825");
    private Color colorTitulo = Color.web("#89B4FA");
    // Los colores de recorrido salen de PaletaColores (misma fuente que el simulador).
    private static final Color COLOR_RUTA_PRINCIPAL = Color.web(PaletaColores.COLOR_RUTA_IDA);
    private static final Color COLOR_RUTA_RETORNO = Color.web(PaletaColores.COLOR_RUTA_VUELTA);
    private static final Color COLOR_RUTA_ACTIVA = Color.web(PaletaColores.COLOR_RUTA_ACTIVA);
=======
    private Color fondo = Color.web("#1E1E2E");
    private Color colorTitulo = Color.web("#89B4FA");
    private static final Color COLOR_RUTA_PRINCIPAL = Color.web("#00A8FF");
    private static final Color COLOR_RUTA_RETORNO = Color.web("#FF1744");
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
    private static final Color COLOR_PUNTO = Color.web("#F9E2AF");
    private Color colorPuntoBorde = Color.web("#1E1E2E");
    private Color colorPuntoTexto = Color.web("#CDD6F4");
    private static final Color COLOR_INICIO_RUTA = Color.web("#39FF14");
    private static final Color COLOR_FINAL_RUTA = Color.web("#FF3B5C");
    private static final Color COLOR_EXTREMO_COMPARTIDO = Color.web("#FFD740");
    private static final Color COLOR_TERMINAL = Color.web("#00E5FF");
    private static final Font FONT_TITULO = Font.font("System", 14);
    private static final Font FONT_PUNTO = Font.font("System", 11);

<<<<<<< HEAD
    private static final Color FONDO_OSCURO = Color.web("#181825");
    private static final Color[] COLORES_VIAS_OSCURO = {
            Color.web("#9399B2"), Color.web("#7F849C"), Color.web("#6C7086"),
            Color.web("#585B70"), Color.web("#45475A"), Color.web("#45475A"),
            Color.web("#3B3D4F"), Color.web("#4A4538"), Color.web("#585B70")
=======
    private static final Color[] COLORES_VIAS_OSCURO = {
            Color.web("#E892A2"), Color.web("#FCD6A4"), Color.web("#F7FABD"),
            Color.WHITE, Color.web("#AAAAAA"), Color.web("#666666"),
            Color.web("#555555"), Color.web("#8B7355"), Color.web("#777777")
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
    };
    private static final Color[] COLORES_VIAS_CLARO = {
            Color.web("#B44B61"), Color.web("#C47B28"), Color.web("#9C8C15"),
            Color.web("#4C596D"), Color.web("#A0A7B4"), Color.web("#B7BEC9"),
            Color.web("#C3C9D2"), Color.web("#A58D72"), Color.web("#B7BEC9")
    };
    private static final int TAMANO_CELDA_VEHICULOS = 18;

<<<<<<< HEAD
    // ===== Croquis =====
    private static final Color CROQUIS_CALLE = Color.web("#8A8A8A");
    private static final Color CROQUIS_TRAMADO = Color.web("#BDBDBD");
    private static final int CELDAS_URBANAS = 48;
    private static final double UMBRAL_URBANO = 0.12;
    private static final Font FONT_LEYENDA = Font.font("Arial", FontWeight.NORMAL, 10);
    private static final Font FONT_MARCA_AGUA = Font.font("Arial", FontWeight.BOLD, 14);
    private static final Color FONDO_LEYENDA = Color.web("#11111B", 0.82);
    private static final Color TEXTO_LEYENDA = Color.web("#CDD6F4");
    private static final double ALTO_LEYENDA = 24;
    private static final Text MEDIDOR_LEYENDA = new Text();

    /** Zona rotulada en el croquis (posición geográfica del centro del nombre). */
    public record ZonaCroquis(String nombre, double lon, double lat) {}

    private record DensidadUrbana(boolean[] celdas, double cx, double cy) {}

=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
    private static final double[] ZOOMS = {
            0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0, 6.0, 8.0,
            12.0, 16.0, 24.0, 32.0, 48.0, 72.0, 108.0, 162.0, 243.0,
            364.0, 546.0, 729.0
    };

    private final Canvas capaVias;
    private final Canvas canvas;
    private final GeoJsonParser parser = new GeoJsonParser();
    private Proyector proyector = new Proyector();
    private List<Via> vias;
    private STRtree indiceVias;
    private GrafoVial grafo;
    private List<Edge> viasCerradas = List.of();
    private Color[] coloresVias = COLORES_VIAS_OSCURO;
    private boolean temaClaro;
<<<<<<< HEAD
    private boolean modoCroquis = true;
    private boolean[] zonaUrbana;
    private List<ZonaCroquis> zonas = List.of();
    private boolean zonasDefinidas;
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
    private int columnasVehiculos;
    private int[] cantidadVehiculosPorCelda = new int[0];
    private Color[] colorVehiculosPorCelda = new Color[0];
    private boolean cargado = false;

    private final List<List<Via>> gruposVias = crearGruposVias();
    private final List<PuntoImportante> puntos = new ArrayList<>();
    private Set<String> puntosInicioRutas = Set.of();
    private Set<String> puntosFinalRutas = Set.of();
    private List<EventoTrafico> eventos = List.of();
    private List<PosicionVehiculo> vehiculos = List.of();
    private List<ServicioMicro> rutasMicros = List.of();
    private Set<String> rutasMicrosVisibles = Set.of();
    private boolean mostrarCalor;
    private boolean mostrarIdaNeon;
    private boolean mostrarVueltaNeon;
    private Trayecto trayectoPartida;
    private Trayecto trayectoRetorno;

    private double offsetX = 0, offsetY = 0;
    private double zoom = 1.0;
    private double escalaBase = 1.0;
    private int nivelZoom = 2;
    private double anchoAnterior, altoAnterior;

    private double lastMouseX, lastMouseY;
    private boolean necesitaRedibujar = true;
    private boolean viasNecesitanRedibujarse = true;
    private long ultimoRenderNanos;

    private Runnable onMapaCargado;
    private java.util.function.Consumer<String> onError;
    private java.util.function.BiConsumer<Double, Double> onClic;

    private final AnimationTimer renderizador = new AnimationTimer() {
        @Override
        public void handle(long ahora) {
            if (!necesitaRedibujar || ahora - ultimoRenderNanos < INTERVALO_RENDER_NANOS) return;
            long intervaloRender = vehiculos.size() > 1_000
                    ? 1_000_000_000L / 15 : INTERVALO_RENDER_NANOS;
            if (ahora - ultimoRenderNanos < intervaloRender) return;
            necesitaRedibujar = false;
            ultimoRenderNanos = ahora;
            try {
                renderizar();
            } catch (RuntimeException ex) {
                LOG.log(Level.SEVERE, "Se omitió un fotograma por un error del mapa", ex);
            }
        }
    };

    public MapaRutasCanvas() {
        capaVias = new Canvas();
        canvas = new Canvas();
        getChildren().addAll(capaVias, canvas);
        capaVias.widthProperty().bind(widthProperty());
        capaVias.heightProperty().bind(heightProperty());
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        widthProperty().addListener((o, a, b) -> {
            viasNecesitanRedibujarse = true;
            recalcularConservandoCentro();
        });
        heightProperty().addListener((o, a, b) -> {
            viasNecesitanRedibujarse = true;
            recalcularConservandoCentro();
        });

        setOnScroll(e -> {
            if (!cargado) return;
            double delta = e.getDeltaY();
            if (Math.abs(delta) < 0.5) return;
            int nuevoNivel = nivelZoom + (delta > 0 ? 1 : -1);
            nuevoNivel = Math.clamp(nuevoNivel, 0, ZOOMS.length - 1);
            if (nuevoNivel == nivelZoom) return;
            aplicarNivelZoom(nuevoNivel, e.getX(), e.getY());
        });

        setOnMousePressed(e -> { lastMouseX = e.getX(); lastMouseY = e.getY(); });
        setOnMouseDragged(e -> {
            if (!cargado) return;
            offsetX += e.getX() - lastMouseX;
            offsetY += e.getY() - lastMouseY;
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            redibujarMapa();
        });

        setOnMouseClicked(e -> {
            if (!cargado) return;
            if (e.getClickCount() == 2) { resetVista(); return; }
            if (onClic != null) {
                double[] norm = pantallaANormalizado(e.getX(), e.getY());
                double[] lonlat = normalizadoALonLat(norm[0], norm[1]);
                onClic.accept(lonlat[0], lonlat[1]);
            }
        });

<<<<<<< HEAD
        aplicarEstiloBase();
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
        renderizador.start();
        cargarMapa();
    }

    public void cargarMapa() {
        new Thread(() -> {
            try {
                List<Via> cargadas = parser.parsearDesdeRecurso("/mapas/juliaca.geojson");
                Proyector p = new Proyector();
                p.preProyectar(cargadas);

                STRtree indice = new STRtree();
                for (Via via : cargadas) {
                    if (via.highway == null || via.projX == null || via.projX.length < 2) continue;
                    indice.insert(new Envelope(via.minX, via.maxX, via.minY, via.maxY), via);
                }
                indice.build();

<<<<<<< HEAD
                DensidadUrbana densidad = calcularDensidadUrbana(cargadas);
                ZonaCroquis zonaPrincipal = new ZonaCroquis("JULIACA",
                        p.getMinLon() + densidad.cx() * p.getAnchoGeo(),
                        p.getMaxLat() - densidad.cy() * p.getAltoGeo());
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
                GrafoVial g = new GrafoVial();
                g.construir(cargadas, p);
                for (Via v : cargadas) v.liberarCoords();

                Platform.runLater(() -> {
                    vias = cargadas;
                    indiceVias = indice;
                    grafo = g;
                    proyector = p;
<<<<<<< HEAD
                    zonaUrbana = densidad.celdas();
                    if (!zonasDefinidas) zonas = List.of(zonaPrincipal);
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
                    cargado = true;
                    viasCerradas = List.of();
                    viasNecesitanRedibujarse = true;
                    recalcularEscalaBase();
                    resetVista();
                    if (onMapaCargado != null) onMapaCargado.run();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    if (onError != null) onError.accept("No se pudo cargar: " + ex.getMessage());
                });
            }
        }, "cargador-mapa").start();
    }

    private void recalcularEscalaBase() {
        if (canvas.getWidth() <= 1 || canvas.getHeight() <= 1) return;
        double margen = 0.05;
        escalaBase = Math.min(canvas.getWidth() * (1 - 2 * margen),
                canvas.getHeight() * (1 - 2 * margen));
        anchoAnterior = canvas.getWidth();
        altoAnterior = canvas.getHeight();
    }

    private void recalcularConservandoCentro() {
        double ancho = canvas.getWidth();
        double alto = canvas.getHeight();
        if (ancho <= 1 || alto <= 1) return;
        if (anchoAnterior > 1 && altoAnterior > 1) {
            double escalaAnterior = escalaBase * zoom;
            double cx = (anchoAnterior / 2.0 - offsetX) / escalaAnterior;
            double cy = (altoAnterior / 2.0 - offsetY) / escalaAnterior;
            escalaBase = Math.min(ancho, alto) * 0.9;
            offsetX = ancho / 2.0 - cx * escalaBase * zoom;
            offsetY = alto / 2.0 - cy * escalaBase * zoom;
        } else {
            recalcularEscalaBase();
        }
        anchoAnterior = ancho;
        altoAnterior = alto;
        redibujarMapa();
    }

    private void resetVista() {
        if (!cargado) return;
        recalcularEscalaBase();
        nivelZoom = 2;
        zoom = ZOOMS[nivelZoom];
        offsetX = (canvas.getWidth() - escalaBase * zoom) / 2.0;
        offsetY = (canvas.getHeight() - escalaBase * zoom) / 2.0;
        redibujarMapa();
    }

    private double pantallaX(double nx) { return nx * escalaBase * zoom + offsetX; }
    private double pantallaY(double ny) { return ny * escalaBase * zoom + offsetY; }

    private void redibujar() { necesitaRedibujar = true; }
    private void redibujarMapa() {
        viasNecesitanRedibujarse = true;
        redibujar();
    }

    private void renderizar() {
        if (!cargado) return;
        if (viasNecesitanRedibujarse) renderizarVias();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);

        dibujarViasCerradas(gc);
        boolean usarColoresNeon = mostrarIdaNeon || mostrarVueltaNeon;
        if (usarColoresNeon) {
            boolean hayRutaMicroVisible = rutasMicros.stream()
                    .anyMatch(ruta -> rutasMicrosVisibles.contains(ruta.id()));
            if (hayRutaMicroVisible) {
<<<<<<< HEAD
                boolean unica = unicaMicroVisible() != null;
                for (ServicioMicro servicio : rutasMicros) {
                    if (!rutasMicrosVisibles.contains(servicio.id())) continue;
                    if (mostrarIdaNeon) {
                        dibujarTrayecto(gc, servicio.trayecto(),
                                unica ? COLOR_RUTA_PRINCIPAL : Color.web(servicio.color()));
                    }
                    if (mostrarVueltaNeon) {
                        dibujarTrayecto(gc, servicio.trayectoVuelta(),
                                unica ? COLOR_RUTA_RETORNO : Color.web(servicio.color()));
=======
                for (ServicioMicro servicio : rutasMicros) {
                    if (!rutasMicrosVisibles.contains(servicio.id())) continue;
                    if (mostrarIdaNeon) dibujarTrayecto(gc, servicio.trayecto(), COLOR_RUTA_PRINCIPAL);
                    if (mostrarVueltaNeon) {
                        dibujarTrayecto(gc, servicio.trayectoVuelta(), COLOR_RUTA_RETORNO);
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
                    }
                }
            } else {
                if (mostrarIdaNeon && trayectoPartida != null) {
                    dibujarTrayecto(gc, trayectoPartida, COLOR_RUTA_PRINCIPAL);
                }
                if (mostrarVueltaNeon && trayectoRetorno != null) {
                    dibujarTrayecto(gc, trayectoRetorno, COLOR_RUTA_RETORNO);
                }
            }
        } else {
            dibujarRutasMicros(gc);
        }
        if (mostrarCalor || (zoom >= 3.0 && zoom < 18.0)) {
            prepararAgrupacionVehiculos();
        }
        if (mostrarCalor) dibujarMapaCalor(gc);
        dibujarPuntos(gc);
        dibujarEventos(gc);
        dibujarVehiculos(gc);
<<<<<<< HEAD
        dibujarLeyenda(gc);
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
    }

    private void renderizarVias() {
        viasNecesitanRedibujarse = false;
        GraphicsContext gc = capaVias.getGraphicsContext2D();
        double w = capaVias.getWidth(), h = capaVias.getHeight();
        gc.setFill(fondo);
        gc.fillRect(0, 0, w, h);
<<<<<<< HEAD
        if (croquisActivo()) dibujarZonasUrbanas(gc, w, h);
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
        double escalaActual = escalaBase * zoom;
        double vpMinX = (0 - offsetX) / escalaActual - 0.02;
        double vpMaxX = (w - offsetX) / escalaActual + 0.02;
        double vpMinY = (0 - offsetY) / escalaActual - 0.02;
        double vpMaxY = (h - offsetY) / escalaActual + 0.02;

        @SuppressWarnings("unchecked")
        List<Via> visibles = indiceVias == null ? List.of()
                : indiceVias.query(new Envelope(vpMinX, vpMaxX, vpMinY, vpMaxY));

        dibujarVias(gc, visibles);
        dibujarNombresVias(gc, visibles);
<<<<<<< HEAD
        dibujarNombresZonas(gc);
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
        dibujarTitulo(gc);
    }

    private void dibujarTitulo(GraphicsContext gc) {
        gc.setFill(colorTitulo);
        gc.setFont(FONT_TITULO);
        gc.fillText("Juliaca - " + (vias == null ? 0 : vias.size()) + " vias | zoom "
                + String.format("%.2f", zoom) + " | doble clic reset", 15, 25);
    }

    private void dibujarVias(GraphicsContext gc, List<Via> visibles) {
<<<<<<< HEAD
        if (croquisActivo()) {
            dibujarViasCroquis(gc, visibles);
            return;
        }
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
        for (List<Via> g : gruposVias) g.clear();
        for (Via v : visibles) {
            if (v.highway == null || v.projX == null || v.projX.length < 2) continue;
            int idx = indiceColorVia(v.highway);
            gruposVias.get(idx).add(v);
        }
        for (int i = 0; i < gruposVias.size(); i++) {
            List<Via> grupo = gruposVias.get(i);
            if (grupo.isEmpty()) continue;
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setLineJoin(StrokeLineJoin.ROUND);
            gc.beginPath();
            for (Via v : grupo) {
                gc.moveTo(pantallaX(v.projX[0]), pantallaY(v.projY[0]));
                for (int j = 1; j < v.projX.length; j++) {
                    gc.lineTo(pantallaX(v.projX[j]), pantallaY(v.projY[j]));
                }
            }
            gc.setStroke(coloresVias[i]);
            gc.setLineWidth(esSecundaria(grupo.get(0).highway)
                    ? Math.max(1, Math.sqrt(zoom) * 0.45)
                    : Math.max(1.8, 2.0 + Math.sqrt(zoom) * 0.5));
            gc.stroke();
            if (zoom >= 6 && i <= 2) {
<<<<<<< HEAD
                gc.setStroke(fondoClaro() ? Color.web("#FFFFFF", 0.88)
=======
                gc.setStroke(temaClaro ? Color.web("#FFFFFF", 0.88)
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
                        : Color.web("#F3F4F8", 0.72));
                gc.setLineWidth(Math.max(0.7, Math.sqrt(zoom) * 0.12));
                gc.setLineDashes(Math.max(2, zoom * 0.55), Math.max(2, zoom * 0.4));
                gc.stroke();
                gc.setLineDashes();
            }
        }
    }

    private void dibujarTrayecto(GraphicsContext gc, Trayecto t, Color color) {
        if (t == null || t.esVacia()) return;
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.beginPath();
        for (Edge edge : t.edges()) {
            gc.moveTo(pantallaX(edge.origen.x), pantallaY(edge.origen.y));
            gc.lineTo(pantallaX(edge.destino.x), pantallaY(edge.destino.y));
        }
<<<<<<< HEAD
        gc.setStroke(fondoClaro() ? Color.web("#FFFFFF", 0.95) : Color.web("#10111A"));
=======
        gc.setStroke(temaClaro ? Color.web("#FFFFFF", 0.95) : Color.web("#10111A"));
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
        gc.setLineWidth(10);
        gc.stroke();
        gc.setStroke(color);
        gc.setLineWidth(6);
        gc.stroke();
        gc.setLineDashes();
    }

    private void dibujarRutasMicros(GraphicsContext gc) {
<<<<<<< HEAD
        boolean unica = unicaMicroVisible() != null;
        for (ServicioMicro servicio : rutasMicros) {
            if (!rutasMicrosVisibles.contains(servicio.id())) continue;
            dibujarTrayecto(gc, servicio.trayecto(),
                    unica ? COLOR_RUTA_PRINCIPAL : Color.web(servicio.color()));
            dibujarTrayecto(gc, servicio.trayectoVuelta(),
                    unica ? COLOR_RUTA_RETORNO : Color.web(servicio.color()));
        }
    }

    /** La única micro visible, o {@code null} si hay ninguna o más de una. */
    private ServicioMicro unicaMicroVisible() {
        ServicioMicro unica = null;
        for (ServicioMicro servicio : rutasMicros) {
            if (!rutasMicrosVisibles.contains(servicio.id())) continue;
            if (unica != null) return null;
            unica = servicio;
        }
        return unica;
    }

=======
        for (ServicioMicro servicio : rutasMicros) {
            if (!rutasMicrosVisibles.contains(servicio.id())) continue;
            dibujarTrayecto(gc, servicio.trayecto(), Color.web(servicio.color()));
            dibujarTrayecto(gc, servicio.trayectoVuelta(), Color.web(servicio.color()));
        }
    }

>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
    private void dibujarViasCerradas(GraphicsContext gc) {
        if (grafo == null) return;
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setStroke(Color.RED);
        gc.setLineWidth(Math.max(3, Math.sqrt(zoom)));
        gc.setLineDashes(7, 4);
        for (Edge edge : viasCerradas) {
            gc.strokeLine(pantallaX(edge.origen.x), pantallaY(edge.origen.y),
                    pantallaX(edge.destino.x), pantallaY(edge.destino.y));
        }
        gc.setLineDashes();
    }

    private void dibujarNombresVias(GraphicsContext gc, List<Via> visibles) {
        if (zoom < 4.0) return;
        Set<Long> celdasOcupadas = new HashSet<>();
        double celda = Math.max(75, 135 - zoom * 2);
        gc.setFont(Font.font("System", Math.min(13, 9 + Math.log(zoom) * 0.7)));
        for (Via via : visibles) {
            if (via.name == null || via.name.isBlank()
                    || via.projX == null || via.projX.length < 2) continue;
            int medio = via.projX.length / 2;
            double x = pantallaX(via.projX[medio]);
            double y = pantallaY(via.projY[medio]);
            if (x < 8 || x > canvas.getWidth() - 8 || y < 12 || y > canvas.getHeight() - 4) {
                continue;
            }
            long celdaId = ((long) Math.floor(x / celda) << 32)
                    | ((long) Math.floor(y / celda) & 0xffffffffL);
            if (!celdasOcupadas.add(celdaId)) continue;
            String texto = via.name.length() > 40
                    ? via.name.substring(0, 37) + "..." : via.name;
            gc.setLineWidth(3);
<<<<<<< HEAD
            gc.setStroke(fondoClaro() ? Color.WHITE : Color.BLACK);
            gc.strokeText(texto, x + 1, y + 1);
            gc.setFill(fondoClaro() ? Color.web("#202735") : Color.WHITE);
=======
            gc.setStroke(temaClaro ? Color.WHITE : Color.BLACK);
            gc.strokeText(texto, x + 1, y + 1);
            gc.setFill(temaClaro ? Color.web("#202735") : Color.WHITE);
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
            gc.fillText(texto, x, y);
        }
    }

    private void dibujarPuntos(GraphicsContext gc) {
        gc.setFont(FONT_PUNTO);
        for (PuntoImportante p : puntos) {
            double nx = (p.lon() - proyector.getMinLon()) / proyector.getAnchoGeo();
            double ny = (proyector.getMaxLat() - p.lat()) / proyector.getAltoGeo();
            double px = pantallaX(nx);
            double py = pantallaY(ny);
            boolean inicio = puntosInicioRutas.contains(p.id());
            boolean fin = puntosFinalRutas.contains(p.id());
            boolean terminal = p.tipo() == PuntoImportante.TipoPunto.TERMINAL
                    && (inicio || fin);
            Color color = inicio && fin ? COLOR_EXTREMO_COMPARTIDO
                    : inicio ? COLOR_INICIO_RUTA
                    : fin ? COLOR_FINAL_RUTA
                    : terminal ? COLOR_TERMINAL : COLOR_PUNTO;
            double radio = inicio || fin || terminal ? 8 : 6;
            gc.setFill(color);
            gc.fillOval(px - radio, py - radio, radio * 2, radio * 2);
            gc.setStroke(colorPuntoBorde);
            gc.setLineWidth(inicio || fin || terminal ? 2.5 : 2);
            gc.strokeOval(px - radio, py - radio, radio * 2, radio * 2);
            gc.setFill(colorPuntoTexto);
            String etiqueta = inicio && fin ? "INICIO/FINAL · " + p.nombre()
                    : inicio ? "INICIO · " + p.nombre()
                    : fin ? "FINAL · " + p.nombre()
                    : p.nombre();
            gc.fillText(etiqueta, px + radio + 2, py - 6);
        }
    }

    private void actualizarExtremosRutas() {
        Set<String> inicios = new HashSet<>();
        Set<String> finales = new HashSet<>();
        boolean hayRutasActivas = false;
        for (ServicioMicro ruta : rutasMicros) {
            if (!rutasMicrosVisibles.contains(ruta.id())) continue;
            agregarExtremos(ruta.trayecto(), inicios, finales);
            hayRutasActivas = true;
        }
        if (!hayRutasActivas) {
            agregarExtremos(trayectoPartida, inicios, finales);
            agregarExtremos(trayectoRetorno, inicios, finales);
        }
        puntosInicioRutas = Set.copyOf(inicios);
        puntosFinalRutas = Set.copyOf(finales);
    }

    private static void agregarExtremos(Trayecto trayecto, Set<String> inicios,
                                        Set<String> finales) {
        if (trayecto == null || trayecto.puntos().isEmpty()) return;
        inicios.add(trayecto.puntos().getFirst().id());
        finales.add(trayecto.puntos().getLast().id());
    }

    private void dibujarMapaCalor(GraphicsContext gc) {
        for (EventoTrafico evento : eventos) {
            double nx = (evento.lon() - proyector.getMinLon()) / proyector.getAnchoGeo();
            double ny = (proyector.getMaxLat() - evento.lat()) / proyector.getAltoGeo();
            double radio = Math.max(20, 42 * Math.min(1, zoom / 4));
            Color color = evento.tipo() == TipoEvento.TRANCADERA
                    ? Color.ORANGERED : Color.GOLD;
            gc.setFill(color.deriveColor(0, 1, 1, 0.17));
            gc.fillOval(pantallaX(nx) - radio, pantallaY(ny) - radio,
                    radio * 2, radio * 2);
        }
        for (int i = 0; i < cantidadVehiculosPorCelda.length; i++) {
            int cantidad = cantidadVehiculosPorCelda[i];
            if (cantidad == 0) continue;
            double px = (i % columnasVehiculos + 0.5) * TAMANO_CELDA_VEHICULOS;
            double py = (i / columnasVehiculos + 0.5) * TAMANO_CELDA_VEHICULOS;
            double radio = Math.min(40, 8 + Math.sqrt(cantidad) * 2);
            gc.setFill(Color.ORANGERED.deriveColor(0, 1, 1, 0.16));
            gc.fillOval(px - radio, py - radio, radio * 2, radio * 2);
        }
    }

    private void dibujarEventos(GraphicsContext gc) {
        for (EventoTrafico evento : eventos) {
            double nx = (evento.lon() - proyector.getMinLon()) / proyector.getAnchoGeo();
            double ny = (proyector.getMaxLat() - evento.lat()) / proyector.getAltoGeo();
            double px = pantallaX(nx), py = pantallaY(ny);
            Color colorEvento = switch (evento.tipo()) {
                case VIA_CERRADA, ACCIDENTE, MANIFESTACION -> Color.RED;
                case TRANCADERA, SEMAFORO_ROTO -> Color.ORANGE;
                case LLUVIA -> Color.DEEPSKYBLUE;
                case GASOLINERA -> Color.GOLD;
                case VENDEDORES_AMBULANTES -> Color.MEDIUMSEAGREEN;
            };
            gc.setFill(colorEvento);
            gc.fillOval(px - 7, py - 7, 14, 14);
            gc.setStroke(Color.WHITE);
            gc.strokeOval(px - 7, py - 7, 14, 14);
            gc.setFill(colorPuntoTexto);
            gc.fillText(evento.nombre(), px + 9, py + 4);
        }
    }

    private void dibujarVehiculos(GraphicsContext gc) {
        if (zoom < 3.0) return;
        if (zoom < 18.0) {
            for (int i = 0; i < cantidadVehiculosPorCelda.length; i++) {
                int cantidad = cantidadVehiculosPorCelda[i];
                if (cantidad == 0) continue;
                double px = (i % columnasVehiculos + 0.5) * TAMANO_CELDA_VEHICULOS;
                double py = (i / columnasVehiculos + 0.5) * TAMANO_CELDA_VEHICULOS;
                gc.setFill(colorVehiculosPorCelda[i]);
                double diametro = Math.min(11, 4 + Math.log1p(cantidad) * 1.3);
                gc.fillOval(px - diametro / 2, py - diametro / 2, diametro, diametro);
                if (cantidad > 1 && zoom >= 8) {
                    gc.setFill(colorPuntoTexto);
                    gc.setFont(Font.font("System", 9));
                    gc.fillText(Integer.toString(cantidad), px + 5, py - 4);
                }
            }
            return;
        }
        int etiquetasDibujadas = 0;
        for (PosicionVehiculo vehiculo : vehiculos) {
            double px = pantallaX(vehiculo.x()), py = pantallaY(vehiculo.y());
            if (px < -20 || py < -20 || px > canvas.getWidth() + 20
                    || py > canvas.getHeight() + 20) continue;
            boolean bus = vehiculo.tipo() == PosicionVehiculo.TipoVehiculo.BUS;
            double ancho = bus ? 18 : 14, alto = bus ? 10 : 8;
            Color color = vehiculo.estado() == PosicionVehiculo.Estado.SIN_RUTA
                    ? Color.RED
                    : vehiculo.estado() == PosicionVehiculo.Estado.AVERIADO
                    ? Color.ORANGE
                    : vehiculo.estado() == PosicionVehiculo.Estado.DETENIDO
                    || vehiculo.estado() == PosicionVehiculo.Estado.EN_PARADA
                    ? Color.YELLOW
<<<<<<< HEAD
                    : croquisActivo() ? COLOR_RUTA_ACTIVA
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
                    : vehiculo.idRuta().isBlank()
                    ? (bus ? Color.DEEPSKYBLUE : Color.LIGHTGREEN)
                    : Color.web(vehiculo.color());
            gc.setFill(color);
            gc.fillRoundRect(px - ancho / 2, py - alto / 2, ancho, alto, 4, 4);
<<<<<<< HEAD
            gc.setFill(fondoClaro() ? Color.web("#263142") : Color.web("#17202A"));
            gc.fillRect(px - ancho / 3, py - alto / 2 + 1, ancho / 2, alto / 3);
            gc.setFill(fondoClaro() ? Color.web("#263142") : Color.BLACK);
            gc.fillOval(px - ancho / 3, py + alto / 4, 4, 4);
            gc.fillOval(px + ancho / 6, py + alto / 4, 4, 4);
            if (zoom >= 40 && etiquetasDibujadas < 80) {
                gc.setStroke(fondoClaro() ? Color.WHITE : Color.BLACK);
                gc.setLineWidth(3);
                gc.strokeText(vehiculo.nombreRuta(), px + 10, py - 5);
                gc.setFill(fondoClaro() ? Color.web("#202735") : Color.WHITE);
=======
            gc.setFill(temaClaro ? Color.web("#263142") : Color.web("#17202A"));
            gc.fillRect(px - ancho / 3, py - alto / 2 + 1, ancho / 2, alto / 3);
            gc.setFill(temaClaro ? Color.web("#263142") : Color.BLACK);
            gc.fillOval(px - ancho / 3, py + alto / 4, 4, 4);
            gc.fillOval(px + ancho / 6, py + alto / 4, 4, 4);
            if (zoom >= 40 && etiquetasDibujadas < 80) {
                gc.setStroke(temaClaro ? Color.WHITE : Color.BLACK);
                gc.setLineWidth(3);
                gc.strokeText(vehiculo.nombreRuta(), px + 10, py - 5);
                gc.setFill(temaClaro ? Color.web("#202735") : Color.WHITE);
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
                gc.setFont(Font.font("System", 9));
                gc.fillText(vehiculo.nombreRuta() + " · " + vehiculo.pasajeros() + " pax",
                        px + 10, py - 5);
                etiquetasDibujadas++;
            }
        }
    }

    private void prepararAgrupacionVehiculos() {
        columnasVehiculos = Math.max(1, (int) Math.ceil(
                canvas.getWidth() / TAMANO_CELDA_VEHICULOS));
        int filasVehiculos = Math.max(1, (int) Math.ceil(
                canvas.getHeight() / TAMANO_CELDA_VEHICULOS));
        int tamano = columnasVehiculos * filasVehiculos;
        if (cantidadVehiculosPorCelda.length != tamano) {
            cantidadVehiculosPorCelda = new int[tamano];
            colorVehiculosPorCelda = new Color[tamano];
        } else {
            Arrays.fill(cantidadVehiculosPorCelda, 0);
            Arrays.fill(colorVehiculosPorCelda, null);
        }
        for (PosicionVehiculo vehiculo : vehiculos) {
            double px = pantallaX(vehiculo.x());
            double py = pantallaY(vehiculo.y());
            if (px < 0 || py < 0 || px >= canvas.getWidth() || py >= canvas.getHeight()) continue;
            int columna = (int) px / TAMANO_CELDA_VEHICULOS;
            int fila = (int) py / TAMANO_CELDA_VEHICULOS;
            int indice = fila * columnasVehiculos + columna;
            if (cantidadVehiculosPorCelda[indice] == 0) {
                colorVehiculosPorCelda[indice] = colorVehiculo(vehiculo);
            }
            cantidadVehiculosPorCelda[indice]++;
        }
    }

    private Color colorVehiculo(PosicionVehiculo vehiculo) {
        return switch (vehiculo.estado()) {
            case SIN_RUTA -> Color.RED;
            case AVERIADO -> Color.ORANGE;
            case DETENIDO, EN_PARADA -> Color.YELLOW;
<<<<<<< HEAD
            case EN_RUTA -> croquisActivo() ? COLOR_RUTA_ACTIVA
                    : vehiculo.idRuta().isBlank()
=======
            case EN_RUTA -> vehiculo.idRuta().isBlank()
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
                    ? Color.DEEPSKYBLUE : Color.web(vehiculo.color());
        };
    }

    private boolean esSecundaria(String h) {
        return h.equals("residential") || h.equals("service") || h.equals("footway")
                || h.equals("path") || h.equals("steps") || h.equals("track")
                || h.equals("unclassified") || h.equals("living_street")
                || h.equals("pedestrian") || h.equals("cycleway");
    }

    private int indiceColorVia(String tipo) {
        if (tipo == null) return coloresVias.length - 1;
        return switch (tipo) {
            case "motorway", "motorway_link", "trunk", "trunk_link" -> 0;
            case "primary", "primary_link" -> 1;
            case "secondary", "secondary_link" -> 2;
            case "tertiary", "tertiary_link" -> 3;
            case "residential", "living_street" -> 4;
            case "service", "services" -> 5;
            case "footway", "path", "steps" -> 6;
            case "track" -> 7;
            default -> coloresVias.length - 1;
        };
    }

    private static List<List<Via>> crearGruposVias() {
        List<List<Via>> g = new ArrayList<>(COLORES_VIAS_OSCURO.length);
        for (int i = 0; i < COLORES_VIAS_OSCURO.length; i++) g.add(new ArrayList<>());
        return g;
    }

    private void aplicarNivelZoom(int nuevoNivel, double cx, double cy) {
        double nuevoZoom = ZOOMS[nuevoNivel];
        offsetX = cx - (cx - offsetX) * (nuevoZoom / zoom);
        offsetY = cy - (cy - offsetY) * (nuevoZoom / zoom);
        zoom = nuevoZoom;
        nivelZoom = nuevoNivel;
        redibujarMapa();
    }

    public void zoomIn() {
        if (!cargado) return;
        int n = Math.min(nivelZoom + 1, ZOOMS.length - 1);
        if (n != nivelZoom) aplicarNivelZoom(n, canvas.getWidth() / 2, canvas.getHeight() / 2);
    }

    public void zoomOut() {
        if (!cargado) return;
        int n = Math.max(nivelZoom - 1, 0);
        if (n != nivelZoom) aplicarNivelZoom(n, canvas.getWidth() / 2, canvas.getHeight() / 2);
    }

    public void resetVistaPublico() { resetVista(); }

    public void centrarEn(double lon, double lat, double zoomDeseado) {
        if (!cargado) return;
        double nx = (lon - proyector.getMinLon()) / proyector.getAnchoGeo();
        double ny = (proyector.getMaxLat() - lat) / proyector.getAltoGeo();
        int mejor = nivelZoom;
        double mejorDif = Double.MAX_VALUE;
        for (int i = 0; i < ZOOMS.length; i++) {
            double dif = Math.abs(ZOOMS[i] - zoomDeseado);
            if (dif < mejorDif) { mejorDif = dif; mejor = i; }
        }
        zoom = ZOOMS[mejor];
        nivelZoom = mejor;
        double e = escalaBase * zoom;
        offsetX = canvas.getWidth() / 2.0 - nx * e;
        offsetY = canvas.getHeight() / 2.0 - ny * e;
        redibujarMapa();
    }

    public void enfocarTrayecto(Trayecto trayecto) {
        if (!cargado || trayecto == null || trayecto.edges().isEmpty()) return;
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Edge edge : trayecto.edges()) {
            minX = Math.min(minX, Math.min(edge.origen.x, edge.destino.x));
            minY = Math.min(minY, Math.min(edge.origen.y, edge.destino.y));
            maxX = Math.max(maxX, Math.max(edge.origen.x, edge.destino.x));
            maxY = Math.max(maxY, Math.max(edge.origen.y, edge.destino.y));
        }
        double ancho = Math.max(0.0005, maxX - minX);
        double alto = Math.max(0.0005, maxY - minY);
        double zoomMaximo = Math.min((canvas.getWidth() - 100) / (escalaBase * ancho),
                (canvas.getHeight() - 100) / (escalaBase * alto));
        int nivel = 0;
        for (int i = 0; i < ZOOMS.length; i++) {
            if (ZOOMS[i] <= zoomMaximo) nivel = i;
        }
        zoom = ZOOMS[nivel];
        nivelZoom = nivel;
        double centroX = (minX + maxX) / 2;
        double centroY = (minY + maxY) / 2;
        offsetX = canvas.getWidth() / 2.0 - centroX * escalaBase * zoom;
        offsetY = canvas.getHeight() / 2.0 - centroY * escalaBase * zoom;
        redibujarMapa();
    }

    public double[] pantallaANormalizado(double px, double py) {
        double e = escalaBase * zoom;
        return new double[]{(px - offsetX) / e, (py - offsetY) / e};
    }

    public double[] normalizadoALonLat(double nx, double ny) {
        return new double[]{
                proyector.getMinLon() + nx * proyector.getAnchoGeo(),
                proyector.getMaxLat() - ny * proyector.getAltoGeo()
        };
    }

    public double getZoom() { return zoom; }
    public int getTotalVias() { return vias == null ? 0 : vias.size(); }
    public GrafoVial getGrafo() { return grafo; }
    public List<PuntoImportante> getPuntos() { return List.copyOf(puntos); }

    public void setPuntos(List<PuntoImportante> lista) {
        Platform.runLater(() -> {
            puntos.clear();
            puntos.addAll(lista);
            actualizarExtremosRutas();
            redibujar();
        });
    }

    public void setEventos(List<EventoTrafico> lista) {
        Platform.runLater(() -> {
            eventos = List.copyOf(lista);
            if (grafo != null) {
                viasCerradas = grafo.getEdges().stream().filter(edge -> edge.cerrado).toList();
            }
            redibujar();
        });
    }

    public void setTemaClaro(boolean claro) {
        Runnable actualizar = () -> {
            temaClaro = claro;
<<<<<<< HEAD
            aplicarEstiloBase();
=======
            fondo = claro ? Color.web("#F3F6FA") : Color.web("#1E1E2E");
            colorTitulo = claro ? Color.web("#263B5A") : Color.web("#89B4FA");
            colorPuntoBorde = claro ? Color.WHITE : Color.web("#1E1E2E");
            colorPuntoTexto = claro ? Color.web("#253249") : Color.web("#CDD6F4");
            coloresVias = claro ? COLORES_VIAS_CLARO : COLORES_VIAS_OSCURO;
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
            redibujarMapa();
        };
        if (Platform.isFxApplicationThread()) actualizar.run();
        else Platform.runLater(actualizar);
    }

    public void setVehiculos(List<PosicionVehiculo> lista) {
        Runnable actualizar = () -> {
            vehiculos = List.copyOf(lista);
            redibujar();
        };
        if (Platform.isFxApplicationThread()) actualizar.run();
        else Platform.runLater(actualizar);
    }

    public void setRutasMicros(List<ServicioMicro> rutas, Set<String> visibles) {
        Runnable actualizar = () -> {
            rutasMicros = List.copyOf(rutas);
            rutasMicrosVisibles = Set.copyOf(visibles);
            actualizarExtremosRutas();
            redibujar();
        };
        if (Platform.isFxApplicationThread()) actualizar.run();
        else Platform.runLater(actualizar);
    }

    public void setMostrarRutasNeon(boolean ida, boolean vuelta) {
        Runnable actualizar = () -> {
            mostrarIdaNeon = ida;
            mostrarVueltaNeon = vuelta;
            redibujar();
        };
        if (Platform.isFxApplicationThread()) actualizar.run();
        else Platform.runLater(actualizar);
    }

    public void setMostrarCalor(boolean mostrar) {
        Platform.runLater(() -> {
            mostrarCalor = mostrar;
            redibujar();
        });
    }

    public void setTrayectoPartida(Trayecto t) {
        Platform.runLater(() -> {
            trayectoPartida = t;
            actualizarExtremosRutas();
            redibujar();
        });
    }

    public void setTrayectoRetorno(Trayecto t) {
        Platform.runLater(() -> {
            trayectoRetorno = t;
            actualizarExtremosRutas();
            redibujar();
        });
    }

<<<<<<< HEAD
    /** Activa el aspecto de croquis (fondo blanco, calles grises, zonas, leyenda). */
    public void setModoCroquis(boolean activo) {
        Runnable actualizar = () -> {
            modoCroquis = activo;
            aplicarEstiloBase();
            redibujarMapa();
        };
        if (Platform.isFxApplicationThread()) actualizar.run();
        else Platform.runLater(actualizar);
    }

    public boolean isModoCroquis() { return modoCroquis; }

    /** Define los nombres de zona que se rotulan en el croquis. */
    public void setZonas(List<ZonaCroquis> lista) {
        Runnable actualizar = () -> {
            zonas = List.copyOf(lista);
            zonasDefinidas = true;
            redibujarMapa();
        };
        if (Platform.isFxApplicationThread()) actualizar.run();
        else Platform.runLater(actualizar);
    }

    private boolean fondoClaro() {
        return temaClaro;
    }

    /** El croquis blanco solo se usa con tema claro; el tema oscuro conserva su fondo. */
    private boolean croquisActivo() {
        return modoCroquis && temaClaro;
    }

    private void aplicarEstiloBase() {
        if (croquisActivo()) {
            fondo = Color.WHITE;
            colorTitulo = Color.GRAY;
            colorPuntoBorde = Color.WHITE;
            colorPuntoTexto = Color.BLACK;
        } else if (temaClaro) {
            fondo = Color.web("#F3F6FA");
            colorTitulo = Color.web("#263B5A");
            colorPuntoBorde = Color.WHITE;
            colorPuntoTexto = Color.web("#253249");
        } else {
            fondo = FONDO_OSCURO;
            colorTitulo = Color.web("#89B4FA");
            colorPuntoBorde = FONDO_OSCURO;
            colorPuntoTexto = Color.web("#CDD6F4");
        }
        coloresVias = temaClaro ? COLORES_VIAS_CLARO : COLORES_VIAS_OSCURO;
    }

    // ------------------------------------------------------------------ croquis

    /** Calles grises finas, de grosor uniforme. */
    private void dibujarViasCroquis(GraphicsContext gc, List<Via> visibles) {
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setStroke(CROQUIS_CALLE);
        gc.setLineWidth(Math.clamp(0.5 + Math.sqrt(zoom) * 0.12, 0.6, 1.4));
        gc.beginPath();
        for (Via v : visibles) {
            if (v.projX == null || v.projY == null || v.projX.length < 2) continue;
            gc.moveTo(pantallaX(v.projX[0]), pantallaY(v.projY[0]));
            for (int j = 1; j < v.projX.length; j++) {
                gc.lineTo(pantallaX(v.projX[j]), pantallaY(v.projY[j]));
            }
        }
        gc.stroke();
    }

    /** Puntea en gris las celdas urbanizadas; el patrón se mueve con el mapa. */
    private void dibujarZonasUrbanas(GraphicsContext gc, double w, double h) {
        if (zonaUrbana == null) return;
        double celda = escalaBase * zoom / CELDAS_URBANAS;
        if (celda < 2) return;
        int puntosPorLado = Math.max(2, (int) Math.round(celda / 6.0));
        double paso = celda / puntosPorLado;
        double lado = Math.clamp(paso * 0.2, 0.8, 1.6);
        gc.setFill(CROQUIS_TRAMADO);
        int c0 = Math.clamp((long) Math.floor((0 - offsetX) / celda), 0, CELDAS_URBANAS - 1);
        int c1 = Math.clamp((long) Math.floor((w - offsetX) / celda), 0, CELDAS_URBANAS - 1);
        int f0 = Math.clamp((long) Math.floor((0 - offsetY) / celda), 0, CELDAS_URBANAS - 1);
        int f1 = Math.clamp((long) Math.floor((h - offsetY) / celda), 0, CELDAS_URBANAS - 1);
        for (int fila = f0; fila <= f1; fila++) {
            for (int col = c0; col <= c1; col++) {
                if (!zonaUrbana[fila * CELDAS_URBANAS + col]) continue;
                double x0 = offsetX + col * celda;
                double y0 = offsetY + fila * celda;
                int i0 = Math.max(0, (int) Math.floor((0 - x0) / paso));
                int i1 = Math.min(puntosPorLado - 1, (int) Math.floor((w - x0) / paso));
                int j0 = Math.max(0, (int) Math.floor((0 - y0) / paso));
                int j1 = Math.min(puntosPorLado - 1, (int) Math.floor((h - y0) / paso));
                for (int j = j0; j <= j1; j++) {
                    for (int i = i0; i <= i1; i++) {
                        gc.fillRect(x0 + (i + 0.5) * paso - lado / 2,
                                y0 + (j + 0.5) * paso - lado / 2, lado, lado);
                    }
                }
            }
        }
    }

    /** Marca de agua de zona: 14 px, gris muy tenue (opacidad 0.25), en vertical. */
    private void dibujarNombresZonas(GraphicsContext gc) {
        if (zonas.isEmpty()) return;
        for (ZonaCroquis zona : zonas) {
            double nx = (zona.lon() - proyector.getMinLon()) / proyector.getAnchoGeo();
            double ny = (proyector.getMaxLat() - zona.lat()) / proyector.getAltoGeo();
            gc.save();
            gc.translate(pantallaX(nx), pantallaY(ny));
            gc.rotate(-90);
            gc.setFont(FONT_MARCA_AGUA);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFill(Color.gray(0.5, 0.25));
            gc.fillText(zona.nombre().toUpperCase(java.util.Locale.ROOT), 0, 0);
            gc.restore();
        }
    }

    /** Leyenda: barra horizontal de 24 px en la esquina inferior izquierda. */
    private void dibujarLeyenda(GraphicsContext gc) {
        List<String> textos = new ArrayList<>();
        List<Color> colores = new ArrayList<>();
        if (unicaMicroVisible() != null || rutasMicros.stream()
                .noneMatch(r -> rutasMicrosVisibles.contains(r.id()))) {
            textos.add("PARTIDA");
            colores.add(COLOR_RUTA_PRINCIPAL);
            textos.add("RETORNO");
            colores.add(COLOR_RUTA_RETORNO);
        } else {
            textos.add("COLOR PROPIO POR MICRO");
            colores.add(null);
        }
        if (croquisActivo()) {
            textos.add("UNIDAD EN MOVIMIENTO");
            colores.add(COLOR_RUTA_ACTIVA);
        }

        double margen = 10, relleno = 8, muestra = 14, hueco = 4, separacion = 12;
        double ancho = relleno * 2 - separacion;
        double[] anchos = new double[textos.size()];
        MEDIDOR_LEYENDA.setFont(FONT_LEYENDA);
        for (int i = 0; i < textos.size(); i++) {
            MEDIDOR_LEYENDA.setText(textos.get(i));
            anchos[i] = MEDIDOR_LEYENDA.getLayoutBounds().getWidth();
            ancho += anchos[i] + separacion + (colores.get(i) == null ? 0 : muestra + hueco);
        }
        double x = margen;
        double y = canvas.getHeight() - ALTO_LEYENDA - margen;
        if (y < 0 || x + ancho > canvas.getWidth()) return;

        gc.setFill(FONDO_LEYENDA);
        gc.fillRoundRect(x, y, ancho, ALTO_LEYENDA, 6, 6);
        gc.setFont(FONT_LEYENDA);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        double cx = x + relleno, cy = y + ALTO_LEYENDA / 2;
        for (int i = 0; i < textos.size(); i++) {
            if (colores.get(i) != null) {
                gc.setStroke(colores.get(i));
                gc.setLineWidth(3);
                gc.setLineCap(StrokeLineCap.BUTT);
                gc.strokeLine(cx, cy, cx + muestra, cy);
                cx += muestra + hueco;
            }
            gc.setFill(TEXTO_LEYENDA);
            gc.fillText(textos.get(i), cx, cy);
            cx += anchos[i] + separacion;
        }
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);    }

    private static DensidadUrbana calcularDensidadUrbana(List<Via> vias) {
        int n = CELDAS_URBANAS;
        double[] largo = new double[n * n];
        for (Via v : vias) {
            if (v.projX == null || v.projY == null || v.projX.length < 2) continue;
            for (int i = 1; i < v.projX.length; i++) {
                double ax = v.projX[i - 1], ay = v.projY[i - 1];
                double bx = v.projX[i], by = v.projY[i];
                int col = Math.clamp((long) Math.floor((ax + bx) / 2 * n), 0, n - 1);
                int fila = Math.clamp((long) Math.floor((ay + by) / 2 * n), 0, n - 1);
                largo[fila * n + col] += Math.hypot(ax - bx, ay - by);
            }
        }
        boolean[] densa = new boolean[n * n];
        double sx = 0, sy = 0, peso = 0;
        for (int fila = 0; fila < n; fila++) {
            for (int col = 0; col < n; col++) {
                double suma = 0;
                for (int df = -1; df <= 1; df++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int f = fila + df, c = col + dc;
                        if (f >= 0 && f < n && c >= 0 && c < n) suma += largo[f * n + c];
                    }
                }
                if (suma < UMBRAL_URBANO) continue;
                densa[fila * n + col] = true;
                double w = largo[fila * n + col];
                sx += (col + 0.5) / n * w;
                sy += (fila + 0.5) / n * w;
                peso += w;
            }
        }
        return new DensidadUrbana(densa, peso > 0 ? sx / peso : 0.5, peso > 0 ? sy / peso : 0.5);
    }

=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
    public void setOnMapaCargado(Runnable cb) {
        onMapaCargado = cb;
        if (cargado && cb != null) cb.run();
    }

    public void setOnError(java.util.function.Consumer<String> cb) { onError = cb; }
    public void setOnClic(java.util.function.BiConsumer<Double, Double> cb) { onClic = cb; }

    public void setOnMouseMoveCallback(java.util.function.Consumer<double[]> cb) {
        addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, e -> {
            if (!cargado) return;
            double[] n = pantallaANormalizado(e.getX(), e.getY());
            cb.accept(normalizadoALonLat(n[0], n[1]));
        });
        addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> {
            if (!cargado) return;
            double[] n = pantallaALonLatInterno(e.getX(), e.getY());
            cb.accept(n);
        });
    }

    private double[] pantallaALonLatInterno(double px, double py) {
        double[] n = pantallaANormalizado(px, py);
        return normalizadoALonLat(n[0], n[1]);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (cargado) redibujar();
    }

    public void cerrar() {
        renderizador.stop();
    }
}