package trafiqueishon.ui;

import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;
import trafiqueishon.geo.*;
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
    private Color fondo = Color.web("#1E1E2E");
    private Color colorTitulo = Color.web("#89B4FA");
    private static final Color COLOR_RUTA_PRINCIPAL = Color.web("#00A8FF");
    private static final Color COLOR_RUTA_RETORNO = Color.web("#FF1744");
    private static final Color COLOR_PUNTO = Color.web("#F9E2AF");
    private Color colorPuntoBorde = Color.web("#1E1E2E");
    private Color colorPuntoTexto = Color.web("#CDD6F4");
    private static final Color COLOR_INICIO_RUTA = Color.web("#39FF14");
    private static final Color COLOR_FINAL_RUTA = Color.web("#FF3B5C");
    private static final Color COLOR_EXTREMO_COMPARTIDO = Color.web("#FFD740");
    private static final Color COLOR_TERMINAL = Color.web("#00E5FF");
    private static final Font FONT_TITULO = Font.font("System", 14);
    private static final Font FONT_PUNTO = Font.font("System", 11);

    private static final Color[] COLORES_VIAS_OSCURO = {
            Color.web("#E892A2"), Color.web("#FCD6A4"), Color.web("#F7FABD"),
            Color.WHITE, Color.web("#AAAAAA"), Color.web("#666666"),
            Color.web("#555555"), Color.web("#8B7355"), Color.web("#777777")
    };
    private static final Color[] COLORES_VIAS_CLARO = {
            Color.web("#B44B61"), Color.web("#C47B28"), Color.web("#9C8C15"),
            Color.web("#4C596D"), Color.web("#A0A7B4"), Color.web("#B7BEC9"),
            Color.web("#C3C9D2"), Color.web("#A58D72"), Color.web("#B7BEC9")
    };
    private static final int TAMANO_CELDA_VEHICULOS = 18;

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

                GrafoVial g = new GrafoVial();
                g.construir(cargadas, p);
                for (Via v : cargadas) v.liberarCoords();

                Platform.runLater(() -> {
                    vias = cargadas;
                    indiceVias = indice;
                    grafo = g;
                    proyector = p;
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
                for (ServicioMicro servicio : rutasMicros) {
                    if (!rutasMicrosVisibles.contains(servicio.id())) continue;
                    if (mostrarIdaNeon) dibujarTrayecto(gc, servicio.trayecto(), COLOR_RUTA_PRINCIPAL);
                    if (mostrarVueltaNeon) {
                        dibujarTrayecto(gc, servicio.trayectoVuelta(), COLOR_RUTA_RETORNO);
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
    }

    private void renderizarVias() {
        viasNecesitanRedibujarse = false;
        GraphicsContext gc = capaVias.getGraphicsContext2D();
        double w = capaVias.getWidth(), h = capaVias.getHeight();
        gc.setFill(fondo);
        gc.fillRect(0, 0, w, h);
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
        dibujarTitulo(gc);
    }

    private void dibujarTitulo(GraphicsContext gc) {
        gc.setFill(colorTitulo);
        gc.setFont(FONT_TITULO);
        gc.fillText("Juliaca - " + (vias == null ? 0 : vias.size()) + " vias | zoom "
                + String.format("%.2f", zoom) + " | doble clic reset", 15, 25);
    }

    private void dibujarVias(GraphicsContext gc, List<Via> visibles) {
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
                gc.setStroke(temaClaro ? Color.web("#FFFFFF", 0.88)
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
        gc.setStroke(temaClaro ? Color.web("#FFFFFF", 0.95) : Color.web("#10111A"));
        gc.setLineWidth(10);
        gc.stroke();
        gc.setStroke(color);
        gc.setLineWidth(6);
        gc.stroke();
        gc.setLineDashes();
    }

    private void dibujarRutasMicros(GraphicsContext gc) {
        for (ServicioMicro servicio : rutasMicros) {
            if (!rutasMicrosVisibles.contains(servicio.id())) continue;
            dibujarTrayecto(gc, servicio.trayecto(), Color.web(servicio.color()));
            dibujarTrayecto(gc, servicio.trayectoVuelta(), Color.web(servicio.color()));
        }
    }

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
            gc.setStroke(temaClaro ? Color.WHITE : Color.BLACK);
            gc.strokeText(texto, x + 1, y + 1);
            gc.setFill(temaClaro ? Color.web("#202735") : Color.WHITE);
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
                    : vehiculo.idRuta().isBlank()
                    ? (bus ? Color.DEEPSKYBLUE : Color.LIGHTGREEN)
                    : Color.web(vehiculo.color());
            gc.setFill(color);
            gc.fillRoundRect(px - ancho / 2, py - alto / 2, ancho, alto, 4, 4);
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
            case EN_RUTA -> vehiculo.idRuta().isBlank()
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
            fondo = claro ? Color.web("#F3F6FA") : Color.web("#1E1E2E");
            colorTitulo = claro ? Color.web("#263B5A") : Color.web("#89B4FA");
            colorPuntoBorde = claro ? Color.WHITE : Color.web("#1E1E2E");
            colorPuntoTexto = claro ? Color.web("#253249") : Color.web("#CDD6F4");
            coloresVias = claro ? COLORES_VIAS_CLARO : COLORES_VIAS_OSCURO;
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