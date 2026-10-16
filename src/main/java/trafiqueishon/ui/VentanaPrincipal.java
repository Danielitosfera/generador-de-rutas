/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: interfaz
 * Archivo: VentanaPrincipal.java
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
package trafiqueishon.ui;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import trafiqueishon.geo.Edge;
import trafiqueishon.geo.GrafoVial;
import trafiqueishon.io.*;
import trafiqueishon.ruta.*;
import trafiqueishon.simulacion.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.prefs.Preferences;

/**
 * Ventana principal de Trafiqueishon.
 *
 * <p>Diseño defensivo: ningún listener del usuario debería poder tumbar la app.
 * Todo se ejecuta vía {@link #ejecutarSeguro(String, Runnable)} y el
 * {@link AnimationTimer} tiene su propio try/catch que aísla fallos por tick.</p>
 */
public class VentanaPrincipal {

    private static final Logger LOG = Logger.getLogger(VentanaPrincipal.class.getName());
    private static final Preferences PREFERENCIAS =
            Preferences.userNodeForPackage(VentanaPrincipal.class);
    private static final String PREFERENCIA_TEMA_CLARO = "tema-claro";

    // ===== Infraestructura de la ventana =====
    private final BorderPane root = new BorderPane();
    private final StackPane capaCentral = new StackPane();
    private final MapaRutasCanvas mapa = new MapaRutasCanvas();
    private final PanelParadas panelParadas = new PanelParadas();
    private final VistaCroquis vistaCroquis = new VistaCroquis(mapa);
    private final VBox barraSuperior = new VBox();
    private HBox barraEstado;
    private final TabPane panelLateral = new TabPane();
    private Button botonSalirPresentacion;

    // ===== Controles de UI =====
    private final ListView<PuntoImportante> listaPuntos = new ListView<>();
    private final ListView<String> listaTrayectos = new ListView<>();
    private final ListView<EventoTrafico> listaEventos = new ListView<>();
    private final VBox filasRutasMicros = new VBox(6);

    private final Label lblEstado = new Label("Cargando red vial...");
    private final Label lblCoords = new Label("Long.: --  Lat.: --");
    private final Label lblZoom = new Label("Zoom: --");
    private final Label lblRelojStatus = new Label("Hora: --:--");
    private final Label lblHoraSimulada = new Label("Hora simulada: --:--");

    private final CheckBox chkIdaNeon = new CheckBox("Mostrar ida · azul neón");
    private final CheckBox chkVueltaNeon = new CheckBox("Mostrar vuelta · rojo neón");
    private final CheckBox chkMapaCalor = new CheckBox("Mostrar mapa de calor");

    private final ComboBox<TipoEvento> tipoEvento = new ComboBox<>();
    private final TextField nombreEvento = new TextField();
    private final Spinner<Integer> pasajerosSimulados = new Spinner<>(0, 900, 120);
    private final Spinner<Integer> horaInicio = new Spinner<>(0, 23, 7);
    private final Spinner<Integer> minutoInicio = new Spinner<>(0, 59, 0);
    private final Label resumenPasajeros = new Label();
    private final ComboBox<Double> velocidadSimulacion = new ComboBox<>();
    private boolean temaClaro;
    private long ultimoPasoSimulacion;
    private long ultimaActualizacionMapa;

    // ===== Servicios / lógica =====
    private final GestorTrayectos gestor = new GestorTrayectos();
    private final SimuladorBuses simulador = new SimuladorBuses();
    private final GestorEventos gestorEventos = new GestorEventos();
    private final LectorPuntosJson lectorJson = new LectorPuntosJson();
    private final LectorPuntosExcel lectorExcel = new LectorPuntosExcel();
    private final RepositorioServiciosMicro repositorioMicros = new RepositorioServiciosMicro();
    private final ExportadorGeoJson expGeoJson = new ExportadorGeoJson();
    private final ExportadorGpx expGpx = new ExportadorGpx();
    private final ExportadorCsv expCsv = new ExportadorCsv();

    private GrafoVial grafo;
    private final List<PuntoImportante> puntos = new ArrayList<>();
    private final List<EventoTrafico> eventos = new ArrayList<>();
    private final List<ServicioMicro> rutasMicros = new ArrayList<>();
    private final Set<String> rutasMicrosVisibles = new HashSet<>();

    private static final List<String> COLORES_MICROS = List.of(
            "#39FF14", "#FFEA00", "#FF3D00", "#E500FF", "#00F5FF",
            "#FF1493", "#B6FF00", "#FF00A8", "#00FF85", "#FF6EC7");

    // ===== Estado =====
    private boolean colocandoPunto;
    private boolean colocandoEvento;
    private long ultimoTick;
    private boolean presentacionActiva;
    private boolean panelVisibleAntesPresentacion = true;

    // ===== Reloj de simulación =====
    private final AnimationTimer relojSimulacion = new AnimationTimer() {
        @Override
        public void handle(long ahora) {
            try {
                tickSeguro(ahora);
            } catch (Throwable t) {
                // Red de seguridad final: nunca dejamos que un tick tumbe la app.
                LOG.log(Level.SEVERE, "Error crítico en el tick de simulación", t);
                try {
                    simulador.pausar();
                    lblEstado.setText("Simulación pausada por un error; el estado se conserva.");
                } catch (Throwable ignored) {
                    // Ni siquiera el manejo del error puede tumbar la app.
                }
            }
        }
    };

    public VentanaPrincipal() {
        try {
            temaClaro = PREFERENCIAS.getBoolean(PREFERENCIA_TEMA_CLARO, false);
            construirUI();
            configurarCallbacks();
            relojSimulacion.start();
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Fallo al construir la ventana principal", t);
            // Intentamos dejar la app en un estado mínimo pero viva.
            Platform.runLater(() -> {
                Alert alerta = new Alert(Alert.AlertType.ERROR);
                alerta.setTitle("Error al iniciar");
                alerta.setHeaderText("No se pudo inicializar la interfaz");
                alerta.setContentText(t.getMessage() == null
                        ? t.getClass().getSimpleName() : t.getMessage());
                alerta.showAndWait();
            });
        }
    }

    // ==================================================================
    //  CONSTRUCCIÓN DE LA UI
    // ==================================================================

    private void construirUI() {
        root.getStyleClass().add("root-view");
        aplicarClaseTema();
        mapa.setTemaClaro(temaClaro);
        StackPane.setAlignment(panelParadas, Pos.TOP_RIGHT);
        StackPane.setMargin(panelParadas, new Insets(12));
        capaCentral.getChildren().addAll(vistaCroquis, panelParadas);
        root.setCenter(capaCentral);

        barraSuperior.getChildren().setAll(crearToolbar());
        root.setTop(barraSuperior);
        root.setLeft(crearPanelPestanas());

        barraEstado = crearStatusBar();
        root.setBottom(barraEstado);

        root.setFocusTraversable(true);
        root.addEventFilter(KeyEvent.KEY_PRESSED, this::manejarAtajos);
    }

    private void configurarCallbacks() {
        chkIdaNeon.setOnAction(e -> ejecutarSeguro("actualizar visualización",
                this::actualizarVisualizacionRutas));
        chkVueltaNeon.setOnAction(e -> ejecutarSeguro("actualizar visualización",
                this::actualizarVisualizacionRutas));
        chkMapaCalor.setOnAction(e -> ejecutarSeguro("mapa de calor",
                () -> mapa.setMostrarCalor(chkMapaCalor.isSelected())));

        horaInicio.setEditable(true);
        minutoInicio.setEditable(true);
        horaInicio.valueProperty().addListener((o, a, b) -> actualizarHoraPrevia());
        minutoInicio.valueProperty().addListener((o, a, b) -> actualizarHoraPrevia());
        pasajerosSimulados.valueProperty().addListener((o, a, b) -> actualizarResumenPasajeros());

        tipoEvento.getItems().setAll(TipoEvento.values());
        tipoEvento.getSelectionModel().selectFirst();

        velocidadSimulacion.getItems().setAll(0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0);
        velocidadSimulacion.setPrefWidth(90);
        velocidadSimulacion.getSelectionModel().select(Double.valueOf(1.0));
        velocidadSimulacion.valueProperty().addListener((o, a, valor) -> {
            if (valor == null) return;
            ejecutarSeguro("cambiar velocidad", () -> {
                simulador.setVelocidadSimulacion(valor);
                lblEstado.setText(String.format("Velocidad de simulación: %.1fx", valor));
            });
        });

        mapa.setOnMapaCargado(() -> ejecutarSeguro("cargar mapa", () -> {
            grafo = mapa.getGrafo();
            simulador.configurarGrafo(grafo);
            lblEstado.setText("Red lista: " + grafo.numNodos() + " nodos, "
                    + grafo.numEdges() + " tramos.");
            cargarPuntosDesdeRecurso();
            cargarRutasMicrosGuardadas();
        }));
        mapa.setOnError(msg -> lblEstado.setText(msg));
        mapa.setOnClic(this::procesarClicMapa);
        mapa.setOnMouseMoveCallback(lonlat -> {
            lblCoords.setText(String.format("Long.: %.5f  Lat.: %.5f", lonlat[0], lonlat[1]));
            lblZoom.setText(String.format("Zoom: %.2f", mapa.getZoom()));
        });

        gestorEventos.agregarListener(this::procesarEventosActualizados);

        actualizarHoraPrevia();
        actualizarResumenPasajeros();
    }

    private void manejarAtajos(KeyEvent evento) {
        try {
            if (evento.getCode() == KeyCode.TAB) {
                alternarPanelLateral();
                evento.consume();
            } else if (evento.getCode() == KeyCode.F11
                    || (presentacionActiva && evento.getCode() == KeyCode.ESCAPE)) {
                alternarPresentacion();
                evento.consume();
            } else if (evento.getCode() == KeyCode.SPACE) {
                ejecutarSeguro("toggle simulación", this::iniciarOReanudarSimulacion);
                evento.consume();
            }
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Error en atajo de teclado", t);
        }
    }

    // ==================================================================
    //  TOOLBAR
    // ==================================================================

    private VBox crearToolbar() {
        MenuBar menuBar = crearMenuBar();

        HBox controles = new HBox(10);
        controles.setPadding(new Insets(7, 12, 7, 12));
        controles.setAlignment(Pos.CENTER_LEFT);
        controles.getStyleClass().add("toolbar");

        Label titulo = new Label("TRAFIQUEISHON");
        titulo.getStyleClass().add("tab-titulo");

        Button play = crearBotonIcono("▶", "Iniciar / continuar (Espacio)",
                this::iniciarOReanudarSimulacion);
        Button pause = crearBotonIcono("⏸", "Pausar", this::pausarSimulacion);
        Button stop = crearBotonIcono("⏹", "Detener y limpiar", this::detenerSimulacion);
        Button reset = crearBotonIcono("↺", "Reiniciar simulación", () -> {
            limpiarSimulacion(false);
            iniciarSimulacion();
        });

        HBox grupoSimulacion = grupo(play, pause, stop, reset);

        HBox grupoVelocidad = grupo(new Label("Velocidad"), velocidadSimulacion);

        Button btnSoloMapa = new Button("Solo mapa");
        btnSoloMapa.setOnAction(e -> alternarPanelLateral());
        HBox grupoVista = grupo(btnSoloMapa);

        Button zoomOut = crearBotonIcono("−", "Alejar", mapa::zoomOut);
        Button zoomIn = crearBotonIcono("+", "Acercar", mapa::zoomIn);
        HBox grupoZoom = grupo(zoomOut, zoomIn);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        controles.getChildren().addAll(
                titulo,
                new Separator(Orientation.VERTICAL),
                grupoSimulacion,
                new Separator(Orientation.VERTICAL),
                grupoVelocidad,
                new Separator(Orientation.VERTICAL),
                grupoVista,
                espacio,
                lblRelojStatus,
                new Separator(Orientation.VERTICAL),
                grupoZoom);

        return new VBox(menuBar, controles);
    }

    private MenuBar crearMenuBar() {
        Menu archivo = new Menu("Archivo");
        MenuItem importar = item("Importar ubicaciones JSON", this::cargarPuntosJson);
        MenuItem importarExcel = item("Importar ubicaciones Excel", this::cargarPuntosExcel);
        MenuItem plantilla = item("Crear plantilla Excel", this::crearPlantillaExcel);
        MenuItem guardar = item("Guardar ubicaciones JSON", this::guardarPuntos);
        MenuItem salir = new MenuItem("Salir");
        salir.setOnAction(e -> Platform.exit());
        archivo.getItems().addAll(importar, importarExcel, plantilla,
                new SeparatorMenuItem(), guardar, new SeparatorMenuItem(), salir);

        Menu simulacionMenu = new Menu("Simulación");
        simulacionMenu.getItems().addAll(
                item("Iniciar / continuar", this::iniciarOReanudarSimulacion),
                item("Pausar", this::pausarSimulacion),
                item("Detener / reiniciar", this::detenerSimulacion));

        Menu vistas = new Menu("Vistas");
        RadioMenuItem oscuro = new RadioMenuItem("Tema oscuro");
        RadioMenuItem claro = new RadioMenuItem("Tema claro");
        ToggleGroup temas = new ToggleGroup();
        oscuro.setToggleGroup(temas);
        claro.setToggleGroup(temas);
        oscuro.setSelected(!temaClaro);
        claro.setSelected(temaClaro);
        oscuro.setOnAction(e -> aplicarTema(false));
        claro.setOnAction(e -> aplicarTema(true));
        CheckMenuItem croquis = new CheckMenuItem("Vista croquis (fondo blanco)");
        croquis.setSelected(true);
        croquis.setOnAction(e -> ejecutarSeguro("vista croquis",
                () -> vistaCroquis.setModoCroquis(croquis.isSelected())));
        vistas.getItems().addAll(
                item("Mostrar / ocultar panel (Tab)", this::alternarPanelLateral),
                item("Modo presentación (F11)", this::alternarPresentacion),
                new SeparatorMenuItem(),
                oscuro, claro, new SeparatorMenuItem(),
                croquis, new SeparatorMenuItem(),
                item("Ajustar mapa", mapa::resetVistaPublico));

        Menu ayuda = new Menu("Ayuda");
        MenuItem teclado = new MenuItem("Tab panel · F11 presentación · Esc salir · Espacio play");
        teclado.setDisable(true);
        ayuda.getItems().add(teclado);

        return new MenuBar(archivo, simulacionMenu, vistas, ayuda);
    }

    private MenuItem item(String texto, Runnable accion) {
        MenuItem menuItem = new MenuItem(texto);
        menuItem.setOnAction(e -> ejecutarSeguro("acción '" + texto + "'", accion));
        return menuItem;
    }

    private Button crearBotonIcono(String icono, String tooltip, Runnable accion) {
        Button b = new Button(icono);
        b.setTooltip(new Tooltip(tooltip));
        b.setOnAction(e -> ejecutarSeguro("acción '" + tooltip + "'", accion));
        b.getStyleClass().add("boton-toolbar");
        return b;
    }

    private HBox grupo(javafx.scene.Node... nodos) {
        HBox h = new HBox(4, nodos);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("grupo-toolbar");
        return h;
    }

    // ==================================================================
    //  PANEL LATERAL
    // ==================================================================

    private TabPane crearPanelPestanas() {
        panelLateral.setPrefWidth(410);
        panelLateral.setMinWidth(340);
        panelLateral.setMaxWidth(480);
        panelLateral.getStyleClass().add("panel-lateral");
        panelLateral.getTabs().addAll(
                pestana("📍 Puntos", crearPanelPuntos()),
                pestana("🛣 Trayectos", crearPanelTrayectos()),
                pestana("🚌 Micros", crearPanelFlota()),
                pestana("👥 Pasajeros", crearPanelPasajeros()),
                pestana("▶ Simular", crearPanelSimulacion()),
                pestana("⚡ Eventos", crearPanelEventos()));
        return panelLateral;
    }

    private Tab pestana(String titulo, VBox contenido) {
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        Tab tab = new Tab(titulo, scroll);
        tab.setClosable(false);
        return tab;
    }

    private void alternarPanelLateral() {
        ejecutarSeguro("alternar panel", () -> {
            boolean mostrar = root.getLeft() == null;
            root.setLeft(mostrar ? panelLateral : null);
        });
    }

    private void aplicarTema(boolean claro) {
        temaClaro = claro;
        aplicarClaseTema();
        mapa.setTemaClaro(claro);
        PREFERENCIAS.putBoolean(PREFERENCIA_TEMA_CLARO, claro);
    }

    private void aplicarClaseTema() {
        root.getStyleClass().removeAll("theme-dark", "theme-light");
        root.getStyleClass().add(temaClaro ? "theme-light" : "theme-dark");
    }

    private void alternarPresentacion() {
        ejecutarSeguro("modo presentación", () -> {
            presentacionActiva = !presentacionActiva;
            if (presentacionActiva) {
                panelVisibleAntesPresentacion = root.getLeft() != null;
                root.setTop(null);
                root.setLeft(null);
                root.setBottom(null);
                mostrarBotonSalirPresentacion();
            } else {
                root.setTop(barraSuperior);
                root.setLeft(panelVisibleAntesPresentacion ? panelLateral : null);
                root.setBottom(barraEstado);
                ocultarBotonSalirPresentacion();
            }
        });
    }

    private void mostrarBotonSalirPresentacion() {
        if (botonSalirPresentacion == null) {
            botonSalirPresentacion = new Button("✕ Salir (Esc)");
            botonSalirPresentacion.getStyleClass().add("boton-flotante");
            botonSalirPresentacion.setOnAction(e -> alternarPresentacion());
        }
        if (!capaCentral.getChildren().contains(botonSalirPresentacion)) {
            capaCentral.getChildren().add(botonSalirPresentacion);
            StackPane.setAlignment(botonSalirPresentacion, Pos.TOP_RIGHT);
            StackPane.setMargin(botonSalirPresentacion, new Insets(12));
        }
    }

    private void ocultarBotonSalirPresentacion() {
        if (botonSalirPresentacion != null) {
            capaCentral.getChildren().remove(botonSalirPresentacion);
        }
    }

    private VBox panelBase(String titulo) {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(12));
        Label label = new Label(titulo);
        label.getStyleClass().add("tab-titulo");
        label.setWrapText(true);
        panel.getChildren().add(label);
        return panel;
    }

    // ==================================================================
    //  PANELES INDIVIDUALES
    // ==================================================================

    private VBox crearPanelPuntos() {
        VBox panel = panelBase("Puntos de ruta editables");
        listaPuntos.setPlaceholder(new Label("Sin ubicaciones cargadas."));
        listaPuntos.setPrefHeight(180);
        listaPuntos.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PuntoImportante punto, boolean vacio) {
                super.updateItem(punto, vacio);
                setText(vacio || punto == null ? null : punto.tipo() + " · " + punto.nombre()
                        + String.format(" (%.5f, %.5f)", punto.lon(), punto.lat()));
            }
        });

        panel.getChildren().addAll(
                boton("Agregar haciendo clic en mapa", () -> {
                    colocandoPunto = true;
                    colocandoEvento = false;
                    lblEstado.setText("Haz clic en el mapa para colocar la ubicación.");
                }),
                boton("Agregar coordenadas manualmente", () -> editarPunto(null, null)),
                boton("Editar seleccionado", () ->
                        editarPunto(listaPuntos.getSelectionModel().getSelectedItem(), null)),
                boton("Eliminar seleccionado", this::eliminarPunto),
                listaPuntos,
                boton("Guardar ubicaciones JSON", this::guardarPuntos),
                boton("Importar JSON", this::cargarPuntosJson),
                boton("Importar Excel", this::cargarPuntosExcel),
                boton("Crear plantilla Excel", this::crearPlantillaExcel));
        return panel;
    }

    private VBox crearPanelTrayectos() {
        VBox panel = panelBase("Calcular, consultar y exportar");
        Button partida = boton("Calcular PARTIDA", () -> calcular("PARTIDA"));
        Button retorno = boton("Calcular RETORNO", () -> calcular("RETORNO"));
        listaTrayectos.setPlaceholder(new Label("Sin trayectos calculados."));
        listaTrayectos.setPrefHeight(160);

        panel.getChildren().addAll(partida, retorno,
                boton("Enfocar ruta de ida", () -> enfocarRuta("PARTIDA")),
                boton("Enfocar ruta de vuelta", () -> enfocarRuta("RETORNO")),
                listaTrayectos,
                boton("Exportar rutas…", this::exportarRutas),
                boton("Limpiar trayectos", () -> {
                    gestor.limpiar();
                    listaTrayectos.getItems().clear();
                    mapa.setTrayectoPartida(null);
                    mapa.setTrayectoRetorno(null);
                    chkIdaNeon.setSelected(false);
                    chkVueltaNeon.setSelected(false);
                    actualizarVisualizacionRutas();
                }));
        return panel;
    }

    private VBox crearPanelFlota() {
        VBox panel = panelBase("Rutas predefinidas de micros");
        Button crearRuta = boton("Crear ruta de micro", this::crearRutaMicro);
        Button eliminarRuta = boton("Eliminar última ruta", this::eliminarRutaMicro);
        ScrollPane lista = new ScrollPane(filasRutasMicros);
        lista.setFitToWidth(true);
        lista.setPrefHeight(280);
        lista.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        panel.getChildren().addAll(
                new Label("Cada micro sigue sus paraderos en orden y sale según su frecuencia."),
                crearRuta, lista, eliminarRuta,
                boton("Exportar micros y sentidos…", this::exportarRutas));
        return panel;
    }

    private VBox crearPanelPasajeros() {
        VBox panel = panelBase("Demanda de pasajeros por salida");
        pasajerosSimulados.setEditable(true);
        panel.getChildren().addAll(
                new Label("Pasajeros que esperan abordar"),
                pasajerosSimulados,
                resumenPasajeros,
                new Label("Factores de demanda: mañana 2.5x · mediodía 1.8x · tarde 2.8x · noche 0.2x."),
                new Label("Capacidad máxima: 18 pasajeros por micro despachada."));
        actualizarResumenPasajeros();
        return panel;
    }

    private VBox crearPanelSimulacion() {
        VBox panel = panelBase("Simulación en tiempo real");
        panel.getChildren().addAll(
                new Label("Hora inicial (salidas desde paraderos)"),
                new HBox(6, new Label("Hora"), horaInicio, new Label("Minuto"), minutoInicio),
                lblHoraSimulada,
                chkIdaNeon, chkVueltaNeon,
                boton("Iniciar simulación", this::iniciarSimulacion),
                boton("Pausar simulación", this::pausarSimulacion),
                boton("Detener simulación", this::detenerSimulacion));
        return panel;
    }

    private VBox crearPanelEventos() {
        VBox panel = panelBase("Incidentes viales");
        nombreEvento.setPromptText("Nombre del evento (opcional)");
        tipoEvento.setMaxWidth(Double.MAX_VALUE);
        listaEventos.setPlaceholder(new Label("Sin eventos activos."));
        listaEventos.setPrefHeight(160);
        panel.getChildren().addAll(
                new Label("Tipo de evento"), tipoEvento,
                nombreEvento,
                boton("Ubicar evento en el mapa", () -> {
                    colocandoEvento = true;
                    colocandoPunto = false;
                    lblEstado.setText("Haz clic sobre la vía afectada para colocar el evento.");
                }),
                listaEventos,
                boton("Eliminar evento seleccionado", this::eliminarEvento),
                chkMapaCalor);
        return panel;
    }

    // ==================================================================
    //  ACCIONES DE SIMULACIÓN
    // ==================================================================

    private void iniciarOReanudarSimulacion() {
        ejecutarSeguro("iniciar/reanudar", () -> {
            if (simulador.haySimulacion()) {
                simulador.reanudar();
                lblEstado.setText("Simulación reanudada.");
            } else {
                iniciarSimulacion();
            }
        });
    }

    private void pausarSimulacion() {
        ejecutarSeguro("pausar", () -> {
            simulador.pausar();
            lblEstado.setText("Simulación pausada.");
        });
    }

    private void detenerSimulacion() {
        ejecutarSeguro("detener", () -> limpiarSimulacion(true));
    }

    private void limpiarSimulacion(boolean mostrarResultados) {
        try {
            int transportados = simulador.pasajerosTransportados();
            double ocupacion = simulador.ocupacionPromedio() * 100;
            int incidencias = simulador.vehiculosConIncidencia();
            boolean habiaSimulacion = simulador.haySimulacion();
            double segundos = simulador.segundosTranscurridos();

            simulador.limpiar();
            mapa.setVehiculos(List.of());
            lblEstado.setText("Simulación detenida y estado reiniciado.");

            if (mostrarResultados && habiaSimulacion) {
                Alert resultados = new Alert(Alert.AlertType.INFORMATION);
                estilizarDialogo(resultados);
                resultados.setTitle("Resultados de simulación");
                resultados.setHeaderText("Resumen de la ejecución");
                resultados.setContentText("Abordajes simulados: " + transportados
                        + "\nOcupación media de la flota: " + String.format("%.1f%%", ocupacion)
                        + "\nVehículos con ruta pendiente o avería: " + incidencias
                        + "\nTiempo simulado: " + String.format("%.1f min", segundos / 60));
                resultados.show();
            }
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Error al limpiar simulación", t);
        }
    }

    private void iniciarSimulacion() {
        if (grafo == null) {
            lblEstado.setText("La red vial todavía no está lista.");
            return;
        }
        List<ServicioMicro> seleccionadas = rutasMicros.stream()
                .filter(r -> rutasMicrosVisibles.contains(r.id()))
                .toList();
        if (seleccionadas.isEmpty()) {
            lblEstado.setText("Activa al menos una ruta en «Micros».");
            return;
        }
        try {
            PerfilHoraPunta.aplicar(grafo,
                    horaInicio.getValue() * 60 + minutoInicio.getValue());
            List<ServicioMicro> actualizadas = new ArrayList<>();
            CalculadorTrayecto calc = new CalculadorTrayecto(grafo);
            for (ServicioMicro ruta : seleccionadas) {
                Trayecto ida = calc.calcular(ruta.id(), ruta.nombre(),
                        ruta.trayecto().empresa(), "SERVICIO", ruta.trayecto().puntos());
                Trayecto vuelta = calc.calcular(ruta.id() + "-vuelta",
                        ruta.nombre() + " · vuelta", ruta.trayecto().empresa(),
                        "SERVICIO_VUELTA", ruta.trayectoVuelta().puntos());
                actualizadas.add(new ServicioMicro(ruta.id(), ruta.nombre(), ida, vuelta,
                        ruta.frecuenciaMinutos(), ruta.unidadesDisponibles(),
                        ruta.pasajerosEsperando(), ruta.color()));
            }
            simulador.setVelocidadSimulacion(velocidadSimulacion.getValue());
            simulador.iniciar(actualizadas, horaInicio.getValue(), minutoInicio.getValue());
            ultimoTick = 0;
            mapa.setVehiculos(simulador.posiciones());
            int hora = simulador.horaActualMinutos();
            String hhmm = String.format("%02d:%02d", hora / 60, hora % 60);
            lblHoraSimulada.setText("Hora simulada: " + hhmm);
            lblRelojStatus.setText("Hora: " + hhmm);
            lblEstado.setText("Servicios activos: " + actualizadas.size()
                    + ". Salidas programadas desde el primer paradero, según frecuencia.");
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Error al iniciar simulación", t);
            lblEstado.setText("No se pudo iniciar la simulación: " + t.getMessage());
        }
    }

    private void actualizarHoraPrevia() {
        if (simulador.estaEjecutando()) return;
        String hora = String.format("%02d:%02d", horaInicio.getValue(), minutoInicio.getValue());
        lblHoraSimulada.setText("Hora inicial: " + hora);
        lblRelojStatus.setText("Hora: " + hora);
    }

    private void actualizarResumenPasajeros() {
        resumenPasajeros.setText("Demanda base por ruta: " + pasajerosSimulados.getValue()
                + " pasajeros; ajustada por la hora del día.");
    }

    // ==================================================================
    //  RUTAS DE MICROS
    // ==================================================================

    private void crearRutaMicro() {
        editarRutaMicro(null);
    }

    private void editarRutaMicro(ServicioMicro existente) {
        if (grafo == null || puntos.size() < 2) {
            lblEstado.setText("Carga al menos dos paraderos antes de definir una ruta.");
            return;
        }
        try {
            Dialog<Boolean> dialogo = new Dialog<>();
            estilizarDialogo(dialogo);
            dialogo.setTitle(existente == null ? "Crear ruta de micro" : "Editar ruta de micro");
            dialogo.setHeaderText("Configura la línea, sus paraderos, frecuencia y color.");
            ButtonType guardar = new ButtonType("Guardar ruta", ButtonBar.ButtonData.OK_DONE);
            dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);

            TextField nombre = new TextField(existente == null
                    ? "Micro " + (rutasMicros.size() + 1) : existente.nombre());
            Spinner<Integer> frecuencia = new Spinner<>(1, 180,
                    existente == null ? 10 : existente.frecuenciaMinutos());
            long unidadesExistentes = rutasMicros.stream()
                    .filter(ruta -> existente == null || !ruta.id().equals(existente.id()))
                    .mapToLong(ServicioMicro::unidadesDisponibles).sum();
            int maxUnidades = (int) Math.max(1,
                    Math.min(SimuladorBuses.MAX_UNIDADES,
                            SimuladorBuses.MAX_UNIDADES - unidadesExistentes));
            if (maxUnidades == 1 && existente == null
                    && unidadesExistentes >= SimuladorBuses.MAX_UNIDADES) {
                mostrarError("Capacidad de flota alcanzada",
                        "La flota configurada ya suma 6.000 unidades.");
                return;
            }
            int unidadesIniciales = Math.min(maxUnidades, existente == null ? 4
                    : existente.unidadesDisponibles());
            Spinner<Integer> unidades = new Spinner<>(1, maxUnidades, unidadesIniciales);
            unidades.setEditable(true);
            unidades.setPrefWidth(125);
            Spinner<Integer> pasajeros = new Spinner<>(0, 900,
                    existente == null ? pasajerosSimulados.getValue()
                            : existente.pasajerosEsperando());
            ColorPicker color = new ColorPicker(Color.web(existente == null
                    ? siguienteColorMicro() : existente.color()));
            CheckBox optimizarOrden = new CheckBox("Optimizar el orden por tiempo de viaje");
            optimizarOrden.setSelected(true);

            ListView<PuntoImportante> disponibles = new ListView<>();
            disponibles.getItems().setAll(puntos);
            disponibles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            disponibles.setPrefHeight(230);
            disponibles.setCellFactory(list -> new ListCell<>() {
                @Override protected void updateItem(PuntoImportante p, boolean vacio) {
                    super.updateItem(p, vacio);
                    setText(vacio || p == null ? null : p.nombre());
                }
            });

            ListView<PuntoImportante> secuencia = new ListView<>(
                    FXCollections.observableArrayList(
                            existente == null ? List.of() : existente.trayecto().puntos()));
            secuencia.setPrefHeight(230);
            secuencia.setCellFactory(list -> new ListCell<>() {
                @Override protected void updateItem(PuntoImportante p, boolean vacio) {
                    super.updateItem(p, vacio);
                    setText(vacio || p == null ? null
                            : (getIndex() + 1) + ". " + p.nombre());
                }
            });

            Button agregar = new Button("Añadir →");
            agregar.setOnAction(e -> {
                for (PuntoImportante p : new ArrayList<>(disponibles.getItems())) {
                    int idx = disponibles.getItems().indexOf(p);
                    if (disponibles.getSelectionModel().isSelected(idx)
                            && !secuencia.getItems().contains(p)) {
                        secuencia.getItems().add(p);
                    }
                }
            });
            Button subir = new Button("↑");
            subir.setOnAction(e -> moverParadero(secuencia, -1));
            Button bajar = new Button("↓");
            bajar.setOnAction(e -> moverParadero(secuencia, 1));
            Button quitar = new Button("Quitar");
            quitar.setOnAction(e -> {
                int idx = secuencia.getSelectionModel().getSelectedIndex();
                if (idx >= 0) secuencia.getItems().remove(idx);
            });
            VBox acciones = new VBox(8, agregar, subir, bajar, quitar);
            acciones.setAlignment(Pos.CENTER);

            VBox listaDisponibles = new VBox(5, new Label("Ubicaciones disponibles"), disponibles);
            VBox listaSecuencia = new VBox(5, new Label("Paraderos en orden"), secuencia);
            HBox selectorParaderos = new HBox(8, listaDisponibles, acciones, listaSecuencia);
            HBox.setHgrow(listaDisponibles, Priority.ALWAYS);
            HBox.setHgrow(listaSecuencia, Priority.ALWAYS);

            GridPane contenido = new GridPane();
            contenido.setHgap(8);
            contenido.setVgap(8);
            contenido.addRow(0, new Label("Nombre"), nombre);
            contenido.addRow(1, new Label("Frecuencia (min)"), frecuencia);
            contenido.addRow(2, new Label("Unidades"), unidades);
            contenido.addRow(3, new Label("Demanda"), pasajeros);
            contenido.addRow(4, new Label("Color único"), color);
            contenido.add(new Label("Incluye y ordena los paraderos (mínimo 2)"), 0, 5);
            contenido.add(selectorParaderos, 0, 6, 2, 1);
            contenido.add(optimizarOrden, 0, 7, 2, 1);
            dialogo.getDialogPane().setContent(contenido);
            dialogo.setResultConverter(b -> b == guardar);

            Optional<Boolean> confirmado = dialogo.showAndWait();
            if (confirmado.isEmpty() || !confirmado.get()) return;

            String rutaNombre = nombre.getText().trim();
            List<PuntoImportante> seleccionados = List.copyOf(secuencia.getItems());
            if (rutaNombre.isBlank() || seleccionados.size() < 2) {
                mostrarError("Ruta incompleta",
                        "Indica un nombre y selecciona al menos dos paraderos.");
                return;
            }
            String id = existente == null ? "micro-" + UUID.randomUUID() : existente.id();
            String colorHex = colorHex(color.getValue());
            boolean colorEnUso = rutasMicros.stream().anyMatch(r ->
                    !r.id().equals(id) && r.color().equalsIgnoreCase(colorHex));
            if (colorEnUso) {
                mostrarError("Color repetido", "Cada ruta necesita un color distinto.");
                return;
            }
            CalculadorTrayecto calc = new CalculadorTrayecto(grafo);
            Trayecto ida = optimizarOrden.isSelected()
                    ? calc.calcularOrdenOptimo(id, rutaNombre,
                    "Trafiqueishon", "SERVICIO", seleccionados)
                    : calc.calcular(id, rutaNombre, "Trafiqueishon", "SERVICIO", seleccionados);
            List<PuntoImportante> puntosVuelta = new ArrayList<>(ida.puntos());
            Collections.reverse(puntosVuelta);
            Trayecto vuelta = calc.calcular(id + "-vuelta",
                    rutaNombre + " · vuelta", "Trafiqueishon", "SERVICIO_VUELTA", puntosVuelta);
            ServicioMicro servicio = new ServicioMicro(id, rutaNombre, ida, vuelta,
                    frecuencia.getValue(), unidades.getValue(), pasajeros.getValue(), colorHex);
            long unidadesConfiguradas = rutasMicros.stream()
                    .filter(ruta -> !ruta.id().equals(id))
                    .mapToLong(ServicioMicro::unidadesDisponibles).sum()
                    + servicio.unidadesDisponibles();
            if (unidadesConfiguradas > SimuladorBuses.MAX_UNIDADES) {
                mostrarError("Capacidad de flota excedida",
                        "La suma de unidades de todas las rutas no puede superar 6.000.");
                return;
            }
            if (existente == null) {
                rutasMicros.add(servicio);
                rutasMicrosVisibles.add(id);
            } else {
                int idx = rutasMicros.indexOf(existente);
                if (idx >= 0) rutasMicros.set(idx, servicio);
                else rutasMicros.add(servicio);
            }
            gestor.agregar(ida);
            gestor.eliminar(id + "-vuelta");
            actualizarFilasRutasMicro();
            actualizarVisualizacionRutas();
            guardarRutasMicros();
            lblEstado.setText(rutaNombre + ": " + seleccionados.size() + " paraderos, cada "
                    + frecuencia.getValue() + " min · " + ida.resumen());
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Error al crear/editar ruta", t);
            lblEstado.setText("No se pudo guardar la ruta: " + t.getMessage());
        }
    }

    private void moverParadero(ListView<PuntoImportante> secuencia, int desplazamiento) {
        int actual = secuencia.getSelectionModel().getSelectedIndex();
        int destino = actual + desplazamiento;
        if (actual < 0 || destino < 0 || destino >= secuencia.getItems().size()) return;
        Collections.swap(secuencia.getItems(), actual, destino);
        secuencia.getSelectionModel().select(destino);
    }

    private void eliminarRutaMicro() {
        if (rutasMicros.isEmpty()) {
            lblEstado.setText("No hay rutas de micros para quitar.");
            return;
        }
        ServicioMicro ultima = rutasMicros.remove(rutasMicros.size() - 1);
        rutasMicrosVisibles.remove(ultima.id());
        gestor.eliminar(ultima.id());
        actualizarFilasRutasMicro();
        actualizarVisualizacionRutas();
        guardarRutasMicros();
        lblEstado.setText("Ruta eliminada: " + ultima.nombre());
    }

    private void actualizarFilasRutasMicro() {
        filasRutasMicros.getChildren().clear();
        if (rutasMicros.isEmpty()) {
            filasRutasMicros.getChildren().add(new Label("No hay rutas de micro definidas."));
            return;
        }
        for (ServicioMicro ruta : rutasMicros) {
            CheckBox visible = new CheckBox(ruta.nombre() + " · " + ruta.frecuenciaMinutos()
                    + " min · " + ruta.unidadesDisponibles() + " micros");
            visible.setSelected(rutasMicrosVisibles.contains(ruta.id()));
            visible.setStyle("-fx-text-fill: " + ruta.color() + ";");
            Label recorrido = new Label(ruta.trayecto().puntos().stream()
                    .map(PuntoImportante::nombre).collect(Collectors.joining(" → ")));
            recorrido.setWrapText(true);
            recorrido.getStyleClass().add("micro-recorrido");
            visible.setOnAction(e -> {
                if (visible.isSelected()) rutasMicrosVisibles.add(ruta.id());
                else rutasMicrosVisibles.remove(ruta.id());
                actualizarVisualizacionRutas();
            });
            Button editar = new Button("Editar");
            editar.setOnAction(e -> editarRutaMicro(ruta));
            Button eliminar = new Button("×");
            eliminar.setOnAction(e -> {
                rutasMicros.remove(ruta);
                rutasMicrosVisibles.remove(ruta.id());
                gestor.eliminar(ruta.id());
                actualizarFilasRutasMicro();
                actualizarVisualizacionRutas();
                guardarRutasMicros();
            });
            VBox info = new VBox(3, visible, recorrido);
            HBox fila = new HBox(6, info, editar, eliminar);
            fila.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(info, Priority.ALWAYS);
            filasRutasMicros.getChildren().add(fila);
        }
    }

    private void cargarRutasMicrosGuardadas() {
        try {
            boolean repararColores = false;
            for (RepositorioServiciosMicro.RutaGuardada guardada : repositorioMicros.cargar()) {
                try {
                    CalculadorTrayecto calc = new CalculadorTrayecto(grafo);
                    Trayecto ida = calc.calcular(guardada.id(), guardada.nombre(),
                            "Trafiqueishon", "SERVICIO", guardada.paraderos());
                    List<PuntoImportante> paraderosVuelta = new ArrayList<>(guardada.paraderos());
                    Collections.reverse(paraderosVuelta);
                    Trayecto vuelta = calc.calcular(guardada.id() + "-vuelta",
                            guardada.nombre() + " · vuelta", "Trafiqueishon",
                            "SERVICIO_VUELTA", paraderosVuelta);
                    String colorGuardado = colorHex(Color.web(guardada.color()));
                    String color = colorGuardado;
                    if (rutasMicros.stream().anyMatch(r ->
                            r.color().equalsIgnoreCase(colorGuardado))) {
                        color = siguienteColorMicro();
                        repararColores = true;
                    }
                    ServicioMicro servicio = new ServicioMicro(guardada.id(), guardada.nombre(),
                            ida, vuelta, guardada.frecuenciaMinutos(),
                            guardada.unidadesDisponibles(), guardada.pasajerosEsperando(), color);
                    rutasMicros.add(servicio);
                    rutasMicrosVisibles.add(servicio.id());
                    gestor.agregar(ida);
                } catch (Throwable t) {
                    LOG.log(Level.WARNING, "Se omitió ruta guardada: " + guardada.nombre(), t);
                }
            }
            actualizarFilasRutasMicro();
            actualizarVisualizacionRutas();
            if (repararColores) guardarRutasMicros();
            if (!rutasMicros.isEmpty()) {
                lblEstado.setText("Rutas de micros restauradas: " + rutasMicros.size());
            }
        } catch (IOException ex) {
            lblEstado.setText("No se pudieron cargar rutas guardadas: " + ex.getMessage());
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Error inesperado al cargar rutas", t);
        }
    }

    private void guardarRutasMicros() {
        try {
            repositorioMicros.guardar(rutasMicros);
        } catch (IOException ex) {
            lblEstado.setText("No se pudieron guardar las rutas: " + ex.getMessage());
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Error al guardar rutas", t);
        }
    }

    private void actualizarVisualizacionRutas() {
        mapa.setRutasMicros(rutasMicros, rutasMicrosVisibles);
        mapa.setMostrarRutasNeon(chkIdaNeon.isSelected(), chkVueltaNeon.isSelected());
        actualizarPanelParaderos();
    }

    /** El panel de paraderos solo se muestra con exactamente UNA micro seleccionada. */
    private void actualizarPanelParaderos() {
        List<ServicioMicro> seleccionadas = rutasMicros.stream()
                .filter(ruta -> rutasMicrosVisibles.contains(ruta.id())).toList();
        if (seleccionadas.size() == 1) panelParadas.mostrar(seleccionadas.getFirst());
        else panelParadas.ocultar();
    }

    // ==================================================================
    //  PUNTOS
    // ==================================================================

    private void procesarClicMapa(double lon, double lat) {
        try {
            if (colocandoPunto) {
                colocandoPunto = false;
                editarPunto(null, new double[]{lon, lat});
            } else if (colocandoEvento) {
                colocandoEvento = false;
                agregarEvento(lon, lat);
            }
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Error al procesar clic de mapa", t);
        }
    }

    private void editarPunto(PuntoImportante existente, double[] coordenadas) {
        try {
            Dialog<PuntoImportante> dialogo = new Dialog<>();
            estilizarDialogo(dialogo);
            dialogo.setTitle(existente == null ? "Nueva ubicación" : "Editar ubicación");
            dialogo.setHeaderText("Nombre y coordenadas (longitud, latitud)");
            ButtonType aceptar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
            dialogo.getDialogPane().getButtonTypes().addAll(aceptar, ButtonType.CANCEL);

            TextField id = new TextField(existente == null ? nuevoIdPunto() : existente.id());
            TextField nombre = new TextField(existente == null ? "" : existente.nombre());
            TextField lon = new TextField(String.valueOf(existente != null ? existente.lon()
                    : coordenadas == null ? -70.13 : coordenadas[0]));
            TextField lat = new TextField(String.valueOf(existente != null ? existente.lat()
                    : coordenadas == null ? -15.49 : coordenadas[1]));
            ComboBox<PuntoImportante.TipoPunto> tipo = new ComboBox<>();
            tipo.getItems().setAll(PuntoImportante.TipoPunto.values());
            tipo.getSelectionModel().select(existente == null
                    ? PuntoImportante.TipoPunto.PARADA : existente.tipo());

            GridPane campos = new GridPane();
            campos.setHgap(8);
            campos.setVgap(8);
            campos.addRow(0, new Label("ID"), id);
            campos.addRow(1, new Label("Nombre"), nombre);
            campos.addRow(2, new Label("Longitud"), lon);
            campos.addRow(3, new Label("Latitud"), lat);
            campos.addRow(4, new Label("Tipo"), tipo);
            dialogo.getDialogPane().setContent(campos);
            dialogo.setResultConverter(boton -> {
                if (boton != aceptar) return null;
                try {
                    return new PuntoImportante(id.getText().trim(), nombre.getText().trim(),
                            Double.parseDouble(lon.getText().trim()),
                            Double.parseDouble(lat.getText().trim()), tipo.getValue());
                } catch (RuntimeException ex) {
                    lblEstado.setText("Ubicación inválida: " + ex.getMessage());
                    return null;
                }
            });

            Optional<PuntoImportante> resultado = dialogo.showAndWait();
            resultado.ifPresent(punto -> {
                boolean idDuplicado = puntos.stream().anyMatch(actual ->
                        actual.id().equals(punto.id()) && actual != existente);
                if (idDuplicado) {
                    mostrarError("ID duplicado", "Cada ubicación debe tener un ID único.");
                    return;
                }
                if (existente != null) puntos.remove(existente);
                puntos.add(punto);
                actualizarListaPuntos();
                lblEstado.setText("Ubicación actualizada. Guarda el JSON para conservar.");
            });
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Error al editar punto", t);
        }
    }

    private void eliminarPunto() {
        PuntoImportante punto = listaPuntos.getSelectionModel().getSelectedItem();
        if (punto == null) {
            lblEstado.setText("Selecciona una ubicación para eliminar.");
            return;
        }
        puntos.remove(punto);
        actualizarListaPuntos();
        lblEstado.setText("Ubicación eliminada. Guarda el JSON para conservar cambios.");
    }

    private String nuevoIdPunto() {
        int consecutivo = 1;
        while (true) {
            String id = "p" + consecutivo++;
            if (puntos.stream().noneMatch(p -> p.id().equals(id))) return id;
        }
    }

    private void actualizarListaPuntos() {
        listaPuntos.getItems().setAll(puntos);
        mapa.setPuntos(puntos);
    }

    private void cargarPuntosDesdeRecurso() {
        try {
            puntos.clear();
            puntos.addAll(lectorJson.leerRecurso("/datos/puntos.json"));
            actualizarListaPuntos();
            lblEstado.setText("Red lista. " + puntos.size() + " ubicaciones cargadas.");
        } catch (IOException ex) {
            lblEstado.setText("No se pudieron cargar las ubicaciones: " + ex.getMessage());
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Error al cargar puntos del recurso", t);
        }
    }

    private void cargarPuntosJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importar ubicaciones JSON");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON", "*.json"));
        File archivo = chooser.showOpenDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (archivo == null) return;
        try {
            puntos.clear();
            puntos.addAll(lectorJson.leer(archivo.toPath()));
            actualizarListaPuntos();
            lblEstado.setText("Ubicaciones importadas: " + puntos.size());
        } catch (IOException | RuntimeException ex) {
            lblEstado.setText("Error al importar ubicaciones: " + ex.getMessage());
        }
    }

    private void guardarPuntos() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar ubicaciones JSON");
        chooser.setInitialFileName("puntos.json");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON", "*.json"));
        File archivo = chooser.showSaveDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (archivo == null) return;
        try {
            lectorJson.guardar(puntos, archivo.toPath());
            lblEstado.setText("Ubicaciones guardadas: " + archivo.getAbsolutePath());
        } catch (IOException ex) {
            lblEstado.setText("Error al guardar ubicaciones: " + ex.getMessage());
        }
    }

    private void cargarPuntosExcel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importar ubicaciones Excel");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File archivo = chooser.showOpenDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (archivo == null) return;
        try {
            puntos.clear();
            puntos.addAll(lectorExcel.leer(archivo.toPath()));
            actualizarListaPuntos();
            lblEstado.setText("Ubicaciones importadas: " + puntos.size());
        } catch (IOException | RuntimeException ex) {
            lblEstado.setText("Error al importar Excel: " + ex.getMessage());
        }
    }

    private void crearPlantillaExcel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar plantilla Excel");
        chooser.setInitialFileName("plantilla-puntos.xlsx");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File archivo = chooser.showSaveDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (archivo == null) return;
        try (var libro = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var hoja = libro.createSheet("Puntos");
            var cabecera = hoja.createRow(0);
            String[] columnas = {"id", "nombre", "lon", "lat", "tipo"};
            for (int i = 0; i < columnas.length; i++) {
                cabecera.createCell(i).setCellValue(columnas[i]);
            }
            try (var salida = java.nio.file.Files.newOutputStream(archivo.toPath())) {
                libro.write(salida);
            }
            lblEstado.setText("Plantilla creada: " + archivo.getAbsolutePath());
        } catch (IOException ex) {
            lblEstado.setText("Error al crear plantilla: " + ex.getMessage());
        }
    }

    // ==================================================================
    //  TRAYECTOS
    // ==================================================================

    private void calcular(String sentido) {
        try {
            if (grafo == null) {
                lblEstado.setText("La red vial todavía no está lista.");
                return;
            }
            if (puntos.size() < 2) {
                lblEstado.setText("Se necesitan al menos 2 ubicaciones.");
                return;
            }
            List<PuntoImportante> orden = new ArrayList<>(puntos);
            CalculadorTrayecto calculador = new CalculadorTrayecto(grafo);
            String id = sentido.toLowerCase() + "_" + System.currentTimeMillis();
            Trayecto trayecto;
            if ("RETORNO".equals(sentido)) {
                Trayecto partidaOptima = calculador.calcularOrdenOptimo(id + "_base",
                        "Trayecto PARTIDA", "Trafiqueishon", "PARTIDA", orden);
                orden = new ArrayList<>(partidaOptima.puntos());
                Collections.reverse(orden);
                trayecto = calculador.calcular(id, "Trayecto RETORNO",
                        "Trafiqueishon", sentido, orden);
            } else {
                trayecto = calculador.calcularOrdenOptimo(id, "Trayecto PARTIDA",
                        "Trafiqueishon", sentido, orden);
            }
            gestor.agregar(trayecto);
            if ("PARTIDA".equals(sentido)) mapa.setTrayectoPartida(trayecto);
            else mapa.setTrayectoRetorno(trayecto);
            actualizarVisualizacionRutas();
            listaTrayectos.getItems().add(sentido + " · " + trayecto.nombre()
                    + " · " + trayecto.resumen());
            lblEstado.setText("Trayecto " + sentido + ": " + trayecto.resumen());
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Error al calcular trayecto", t);
            lblEstado.setText("Error al calcular el trayecto: " + t.getMessage());
        }
    }

    private void enfocarRuta(String sentido) {
        try {
            Trayecto trayecto = gestor.getTrayectos().stream()
                    .filter(t -> sentido.equals(t.sentido()))
                    .reduce((a, b) -> b)
                    .orElse(null);
            if (trayecto == null) {
                lblEstado.setText("Calcula primero la ruta de " + sentido.toLowerCase() + ".");
                return;
            }
            mapa.enfocarTrayecto(trayecto);
            lblEstado.setText("Vista enfocada en " + sentido.toLowerCase() + ": "
                    + trayecto.resumen());
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Error al enfocar ruta", t);
        }
    }

    private void exportarRutas() {
        try {
            List<OpcionExportacion> opciones = opcionesExportables();
            if (opciones.isEmpty()) {
                lblEstado.setText("No hay rutas calculadas o servicios para exportar.");
                return;
            }
            Dialog<ButtonType> dialogo = new Dialog<>();
            estilizarDialogo(dialogo);
            dialogo.setTitle("Exportar rutas");
            dialogo.setHeaderText("Elige las rutas y sentidos que quieres incluir.");
            dialogo.getDialogPane().getButtonTypes().addAll(
                    ButtonType.OK, ButtonType.CANCEL);

            ListView<OpcionExportacion> lista = new ListView<>(
                    FXCollections.observableArrayList(opciones));
            lista.setCellFactory(view -> new ListCell<>() {
                private final CheckBox check = new CheckBox();
                {
                    check.setMaxWidth(Double.MAX_VALUE);
                    check.setOnAction(event -> {
                        OpcionExportacion opcion = getItem();
                        if (opcion != null) opcion.seleccionada = check.isSelected();
                    });
                }

                @Override
                protected void updateItem(OpcionExportacion opcion, boolean vacio) {
                    super.updateItem(opcion, vacio);
                    if (vacio || opcion == null) {
                        setGraphic(null);
                    } else {
                        check.setText(opcion.ruta.nombre());
                        check.setSelected(opcion.seleccionada);
                        check.setTextFill(Color.web(opcion.ruta.color()));
                        setGraphic(check);
                    }
                }
            });
            lista.setPrefHeight(320);
            lista.setPrefWidth(430);

            ComboBox<String> formato = new ComboBox<>(FXCollections.observableArrayList(
                    "PDF", "GeoJSON", "GPX", "CSV", "TXT"));
            formato.getSelectionModel().selectFirst();
            Button seleccionarVisibles = new Button("Marcar visibles");
            seleccionarVisibles.setOnAction(event -> {
                opciones.forEach(opcion -> opcion.seleccionada = opcion.esVisible());
                lista.refresh();
            });
            Button seleccionarTodas = new Button("Marcar todas");
            seleccionarTodas.setOnAction(event -> {
                opciones.forEach(opcion -> opcion.seleccionada = true);
                lista.refresh();
            });
            Button limpiarSeleccion = new Button("Desmarcar");
            limpiarSeleccion.setOnAction(event -> {
                opciones.forEach(opcion -> opcion.seleccionada = false);
                lista.refresh();
            });
            HBox accionesLista = new HBox(6, seleccionarVisibles,
                    seleccionarTodas, limpiarSeleccion);
            VBox contenido = new VBox(10,
                    new Label("Rutas y sentidos"), lista, accionesLista,
                    new Label("Formato"), formato);
            contenido.setPadding(new Insets(4));
            dialogo.getDialogPane().setContent(contenido);
            dialogo.getDialogPane().setPrefWidth(500);
            Optional<ButtonType> resultado = dialogo.showAndWait();
            if (resultado.isEmpty() || resultado.get() != ButtonType.OK) return;

            List<RutaExportable> seleccionadas = opciones.stream()
                    .filter(opcion -> opcion.seleccionada)
                    .map(opcion -> opcion.ruta).toList();
            if (seleccionadas.isEmpty()) {
                lblEstado.setText("Marca al menos una ruta para exportar.");
                return;
            }
            String extension = switch (formato.getValue()) {
                case "PDF" -> "pdf";
                case "GeoJSON" -> "geojson";
                case "GPX" -> "gpx";
                case "CSV" -> "csv";
                case "TXT" -> "txt";
                default -> throw new IllegalArgumentException("Formato de exportación inválido.");
            };
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exportar " + seleccionadas.size() + " rutas");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    formato.getValue(), "*." + extension));
            chooser.setInitialFileName("rutas-seleccionadas." + extension);
            File archivo = chooser.showSaveDialog(root.getScene() == null ? null : root.getScene().getWindow());
            if (archivo == null) return;
            Path destino = archivo.toPath();
            if (!destino.getFileName().toString().toLowerCase(java.util.Locale.ROOT)
                    .endsWith("." + extension)) {
                destino = destino.resolveSibling(destino.getFileName() + "." + extension);
            }
            switch (extension) {
                case "pdf" -> new ExportadorPdf().exportar(seleccionadas, destino);
                case "geojson" -> expGeoJson.exportar(seleccionadas, destino);
                case "gpx" -> expGpx.exportar(seleccionadas, destino);
                case "csv" -> expCsv.exportar(seleccionadas, destino);
                case "txt" -> new ExportadorTxt().exportar(seleccionadas, destino);
                default -> throw new IllegalArgumentException("Formato desconocido: " + extension);
            }
            lblEstado.setText("Exportadas " + seleccionadas.size() + " rutas: " + destino);
        } catch (IOException | RuntimeException ex) {
            lblEstado.setText("Error al exportar: " + ex.getMessage());
        }
    }

    private List<OpcionExportacion> opcionesExportables() {
        List<OpcionExportacion> opciones = new ArrayList<>();
        for (ServicioMicro servicio : rutasMicros) {
            boolean visible = rutasMicrosVisibles.contains(servicio.id());
            opciones.add(new OpcionExportacion(
                    new RutaExportable(servicio.nombre() + " · Ida", servicio.color(),
                            servicio.trayecto()), visible, visible));
            opciones.add(new OpcionExportacion(
                    new RutaExportable(servicio.nombre() + " · Vuelta", servicio.color(),
                            servicio.trayectoVuelta()), visible, visible));
        }
        List<Trayecto> calculados = gestor.getTrayectos();
        for (int i = 0; i < calculados.size(); i++) {
            Trayecto trayecto = calculados.get(i);
            boolean retorno = "RETORNO".equalsIgnoreCase(trayecto.sentido());
            String color = retorno ? PaletaColores.COLOR_RUTA_VUELTA : PaletaColores.COLOR_RUTA_IDA;
            boolean visible = retorno ? chkVueltaNeon.isSelected() : chkIdaNeon.isSelected();
            opciones.add(new OpcionExportacion(new RutaExportable(trayecto.nombre(), color, trayecto),
                    i == calculados.size() - 1, visible));
        }
        return opciones;
    }

    // ==================================================================
    //  EVENTOS
    // ==================================================================

    private void agregarEvento(double lon, double lat) {
        try {
            TipoEvento tipo = tipoEvento.getValue();
            if (tipo == null) return;
            String nombre = nombreEvento.getText().isBlank()
                    ? tipo + " " + (eventos.size() + 1) : nombreEvento.getText().trim();
            eventos.add(new EventoTrafico(nombre, tipo, lon, lat));
            gestorEventos.publicar(eventos);
            lblEstado.setText("Evento agregado: " + nombre
                    + ". Las rutas se recalculan sin detener la simulación.");
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Error al agregar evento", t);
        }
    }

    private void eliminarEvento() {
        EventoTrafico evento = listaEventos.getSelectionModel().getSelectedItem();
        if (evento == null) {
            lblEstado.setText("Selecciona un evento para eliminar.");
            return;
        }
        eventos.remove(evento);
        gestorEventos.publicar(eventos);
        lblEstado.setText("Evento eliminado y costos recalculados.");
    }

    private void procesarEventosActualizados(List<EventoTrafico> nuevosEventos) {
        try {
            eventos.clear();
            eventos.addAll(nuevosEventos);
            simulador.actualizarEventos(eventos);
            listaEventos.getItems().setAll(eventos);
            mapa.setEventos(eventos);
            aplicarEventosAlGrafo();
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Error al procesar eventos actualizados", t);
        }
    }

    private void aplicarEventosAlGrafo() {
        if (grafo == null) return;
        try {
            for (Edge edge : grafo.getEdges()) {
                edge.cerrado = false;
                edge.factorTrafico = 1.0;
            }
            for (EventoTrafico evento : eventos) {
                double radioEvento = switch (evento.tipo()) {
                    case VIA_CERRADA -> 35;
                    case ACCIDENTE -> 60;
                    case MANIFESTACION -> 100;
                    default -> 120;
                };
                for (Edge edge : grafo.getEdges()) {
                    if (distanciaPuntoTramo(evento.lon(), evento.lat(), edge) > radioEvento) continue;
                    if (evento.tipo().bloqueaVia()) {
                        edge.cerrado = true;
                    } else {
                        edge.factorTrafico = Math.min(10,
                                edge.factorTrafico * evento.tipo().factor());
                    }
                }
            }
            recalcularServiciosPorEventos();
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Error al aplicar eventos al grafo", t);
        }
    }

    private void recalcularServiciosPorEventos() {
        if (grafo == null || rutasMicros.isEmpty()) return;
        try {
            CalculadorTrayecto calc = new CalculadorTrayecto(grafo);
            for (int i = 0; i < rutasMicros.size(); i++) {
                ServicioMicro servicio = rutasMicros.get(i);
                try {
                    Trayecto ida = calc.calcular(servicio.id(), servicio.nombre(),
                            servicio.trayecto().empresa(), "SERVICIO",
                            servicio.trayecto().puntos());
                    Trayecto vuelta = calc.calcular(servicio.id() + "-vuelta",
                            servicio.nombre() + " · vuelta",
                            servicio.trayectoVuelta().empresa(),
                            "SERVICIO_VUELTA", servicio.trayectoVuelta().puntos());
                    ServicioMicro actualizado = new ServicioMicro(servicio.id(),
                            servicio.nombre(), ida, vuelta, servicio.frecuenciaMinutos(),
                            servicio.unidadesDisponibles(), servicio.pasajerosEsperando(),
                            servicio.color());
                    rutasMicros.set(i, actualizado);
                    gestor.agregar(ida);
                } catch (Throwable t) {
                    LOG.log(Level.WARNING, "No se pudo recalcular " + servicio.nombre(), t);
                }
            }
            simulador.actualizarServicios(rutasMicros);
            actualizarFilasRutasMicro();
            actualizarVisualizacionRutas();
            guardarRutasMicros();
            if (simulador.estaEjecutando()) {
                lblEstado.setText("Eventos aplicados; simulación activa y rutas recalculadas.");
            }
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Error al recalcular servicios por eventos", t);
        }
    }

    private double distanciaPuntoTramo(double lon, double lat, Edge edge) {
        try {
            double escalaX = 111_320 * Math.cos(Math.toRadians(lat));
            double ax = edge.origen.lon * escalaX;
            double ay = edge.origen.lat * 111_320;
            double bx = edge.destino.lon * escalaX;
            double by = edge.destino.lat * 111_320;
            double px = lon * escalaX;
            double py = lat * 111_320;
            double dx = bx - ax, dy = by - ay;
            double l2 = dx * dx + dy * dy;
            double t = l2 == 0 ? 0
                    : Math.clamp(((px - ax) * dx + (py - ay) * dy) / l2, 0, 1);
            return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
        } catch (Throwable t) {
            return Double.MAX_VALUE;
        }
    }

    // ==================================================================
    //  UTILIDADES
    // ==================================================================

    private Button boton(String texto, Runnable accion) {
        Button b = new Button(texto);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> ejecutarSeguro("acción '" + texto + "'", accion));
        return b;
    }

    /**
     * Ejecuta una acción capturando <b>cualquier</b> Throwable. Es la única
     * vía por la que se ejecutan acciones disparadas por el usuario.
     */
    private void ejecutarSeguro(String operacion, Runnable accion) {
        try {
            accion.run();
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Falló " + operacion, t);
            try {
                lblEstado.setText("No se pudo completar " + operacion + ": "
                        + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage()));
            } catch (Throwable ignored) {
                // Ni siquiera actualizar el label puede tumbar la app.
            }
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        estilizarDialogo(alerta);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }

    private void estilizarDialogo(Dialog<?> dialogo) {
        var hojaEstilos = getClass().getResource("/estilos/estilo.css");
        if (hojaEstilos != null) {
            String url = hojaEstilos.toExternalForm();
            if (!dialogo.getDialogPane().getStylesheets().contains(url)) {
                dialogo.getDialogPane().getStylesheets().add(url);
            }
        }
        dialogo.getDialogPane().getStyleClass().removeAll("theme-dark", "theme-light");
        dialogo.getDialogPane().getStyleClass().add(temaClaro ? "theme-light" : "theme-dark");
    }

    private String siguienteColorMicro() {
        Set<String> usados = rutasMicros.stream()
                .map(r -> r.color().toUpperCase()).collect(Collectors.toSet());
        for (String candidato : COLORES_MICROS) {
            if (!usados.contains(candidato)) return candidato;
        }
        for (int i = 0; i < 360; i++) {
            String candidato = colorHex(Color.hsb(i, 1, 1));
            if (!usados.contains(candidato)) return candidato;
        }
        return "#FFFFFF";
    }

    private String colorHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    private HBox crearStatusBar() {
        HBox bar = new HBox(12);
        bar.setPadding(new Insets(6, 12, 6, 12));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("status-bar");

        Region esp = new Region();
        HBox.setHgrow(esp, Priority.ALWAYS);

        bar.getChildren().addAll(
                lblEstado,
                esp,
                lblCoords,
                new Separator(Orientation.VERTICAL),
                lblZoom);
        return bar;
    }

    // ==================================================================
    //  TICK DE SIMULACIÓN (aislado)
    // ==================================================================

    private void tickSeguro(long ahora) {
        if (ultimoTick == 0 || !simulador.estaEjecutando()) {
            ultimoTick = ahora;
            return;
        }
        if (ahora - ultimoTick < 50_000_000L) return;
        double delta = (ahora - ultimoTick) / 1_000_000_000.0;
        // Cap defensivo: si la ventana estuvo minimizada mucho tiempo,
        // evitamos saltos gigantes.
        if (delta > 1.0) delta = 1.0;

        simulador.avanzar(delta);
        if (ahora - ultimaActualizacionMapa >= 100_000_000L) {
            mapa.setVehiculos(simulador.posiciones());
            ultimaActualizacionMapa = ahora;
        }

        int minutos = simulador.horaActualMinutos();
        String hhmm = String.format("%02d:%02d", minutos / 60, minutos % 60);
        lblHoraSimulada.setText("Hora simulada: " + hhmm);
        lblRelojStatus.setText("Hora: " + hhmm);

        ultimoTick = ahora;
        ultimoPasoSimulacion = ahora;
    }

    // ==================================================================
    //  API PÚBLICA
    // ==================================================================

    public BorderPane getRoot() {
        return root;
    }

    public void cerrar() {
        try {
            relojSimulacion.stop();
        } catch (Throwable ignored) { }
        try {
            simulador.detener();
        } catch (Throwable ignored) { }
        try {
            mapa.cerrar();
        } catch (Throwable ignored) { }
    }

    private static final class OpcionExportacion {
        private final RutaExportable ruta;
        private final boolean visible;
        private boolean seleccionada;

        private OpcionExportacion(RutaExportable ruta, boolean seleccionada, boolean visible) {
            this.ruta = ruta;
            this.seleccionada = seleccionada;
            this.visible = visible;
        }

        private boolean esVisible() {
            return visible;
        }
    }
}