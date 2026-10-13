package trafiqueishon.ui;

import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VentanaPrincipal {
    private static final Logger LOG = Logger.getLogger(VentanaPrincipal.class.getName());
    private final BorderPane root = new BorderPane();
    private final MapaRutasCanvas mapa = new MapaRutasCanvas();
    private final ListView<PuntoImportante> listaPuntos = new ListView<>();
    private final ListView<String> listaTrayectos = new ListView<>();
    private final ListView<EventoTrafico> listaEventos = new ListView<>();
    private final VBox filasRutasMicros = new VBox(6);
    private final Label lblEstado = new Label("Cargando red vial...");
    private final Label lblCoords = new Label("Long.: --  Lat.: --");
    private final Label lblZoom = new Label("Zoom: --");
    private final Label lblRelojStatus = new Label("Hora: --:--");
    private final HBox barraEstado;
    private final VBox barraSuperior = new VBox();
    private final TabPane panelLateral = new TabPane();
    private final CheckBox chkIdaNeon = new CheckBox("Mostrar ida · azul neón");
    private final CheckBox chkVueltaNeon = new CheckBox("Mostrar vuelta · rojo neón");
    private final CheckBox chkMapaCalor = new CheckBox("Mostrar mapa de calor");
    private final ComboBox<TipoEvento> tipoEvento = new ComboBox<>();
    private final TextField nombreEvento = new TextField();
    private final Spinner<Integer> pasajerosSimulados = new Spinner<>(0, 900, 120);
    private final Spinner<Integer> horaInicio = new Spinner<>(0, 23, 7);
    private final Spinner<Integer> minutoInicio = new Spinner<>(0, 59, 0);
    private final Label resumenPasajeros = new Label();
    private final Label lblHoraSimulada = new Label("Hora simulada: --:--");
    private final ComboBox<Double> velocidadSimulacion = new ComboBox<>();

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
    private boolean colocandoPunto;
    private boolean colocandoEvento;
    private long ultimoTick;
    private int ultimoMinutoHoraPunta = -1;
    private boolean presentacionActiva;
    private boolean panelVisibleAntesPresentacion = true;
    private Button botonPanelMapa;

    private final AnimationTimer relojSimulacion = new AnimationTimer() {
        @Override
        public void handle(long ahora) {
            try {
                if (ultimoTick != 0 && simulador.estaEjecutando()) {
                    simulador.avanzar((ahora - ultimoTick) / 1_000_000_000.0);
                    mapa.setVehiculos(simulador.posiciones());
                    if (grafo != null && ultimoMinutoHoraPunta != simulador.horaActualMinutos()) {
                        PerfilHoraPunta.aplicar(grafo, simulador.horaActualMinutos());
                        ultimoMinutoHoraPunta = simulador.horaActualMinutos();
                    }
                    lblHoraSimulada.setText("Hora simulada: "
                            + String.format("%02d:%02d", simulador.horaActualMinutos() / 60,
                            simulador.horaActualMinutos() % 60));
                    lblRelojStatus.setText("Hora: "
                            + String.format("%02d:%02d", simulador.horaActualMinutos() / 60,
                            simulador.horaActualMinutos() % 60));
                }
            } catch (RuntimeException ex) {
                LOG.log(Level.SEVERE, "Error aislado durante el tick de simulación", ex);
                simulador.pausar();
                lblEstado.setText("Simulación pausada por un error; las unidades conservan su estado.");
            }
            ultimoTick = ahora;
        }
    };

    public VentanaPrincipal() {
        root.getStyleClass().add("root-view");
        root.setCenter(mapa);
        barraSuperior.getChildren().setAll(crearToolbar());
        root.setTop(barraSuperior);
        root.setLeft(crearPanelPestanas());
        barraEstado = crearStatusBar();
        root.setBottom(barraEstado);
        root.setFocusTraversable(true);
        root.addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (evento.getCode() == KeyCode.TAB) {
                alternarPanelLateral();
                evento.consume();
            } else if (evento.getCode() == KeyCode.F11
                    || (presentacionActiva && evento.getCode() == KeyCode.ESCAPE)) {
                alternarPresentacion();
                evento.consume();
            }
        });

        chkIdaNeon.setOnAction(e -> actualizarVisualizacionRutas());
        chkVueltaNeon.setOnAction(e -> actualizarVisualizacionRutas());
        chkMapaCalor.setOnAction(e -> mapa.setMostrarCalor(chkMapaCalor.isSelected()));
        horaInicio.setEditable(true);
        minutoInicio.setEditable(true);
        horaInicio.valueProperty().addListener((obs, anterior, nuevo) -> actualizarHoraPrevia());
        minutoInicio.valueProperty().addListener((obs, anterior, nuevo) -> actualizarHoraPrevia());
        pasajerosSimulados.valueProperty().addListener((obs, antes, despues) -> actualizarResumenPasajeros());
        tipoEvento.getItems().setAll(TipoEvento.values());
        tipoEvento.getSelectionModel().selectFirst();
        velocidadSimulacion.getItems().setAll(0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0);
        velocidadSimulacion.setPrefWidth(88);
        velocidadSimulacion.getSelectionModel().select(Double.valueOf(1.0));
        velocidadSimulacion.valueProperty().addListener((obs, anterior, valor) -> {
            if (valor == null) return;
            simulador.setVelocidadSimulacion(valor);
            lblEstado.setText(String.format("Velocidad de simulación: %.1fx", valor));
        });

        mapa.setOnMapaCargado(() -> {
            grafo = mapa.getGrafo();
            simulador.configurarGrafo(grafo);
            lblEstado.setText("Red lista: " + grafo.numNodos() + " nodos, "
                    + grafo.numEdges() + " tramos.");
            cargarPuntosDesdeRecurso();
            cargarRutasMicrosGuardadas();
        });
        mapa.setOnError(msg -> lblEstado.setText(msg));
        gestorEventos.agregarListener(this::procesarEventosActualizados);
        mapa.setOnClic(this::procesarClicMapa);
        mapa.setOnMouseMoveCallback(lonlat -> {
            lblCoords.setText(String.format("Long.: %.5f  Lat.: %.5f", lonlat[0], lonlat[1]));
            lblZoom.setText(String.format("Zoom: %.2f", mapa.getZoom()));
        });
        relojSimulacion.start();
        actualizarHoraPrevia();
    }

    private VBox crearToolbar() {
        Menu archivo = new Menu("Archivo");
        MenuItem importar = new MenuItem("Importar ubicaciones JSON");
        importar.setOnAction(e -> ejecutarSeguro("importar JSON", this::cargarPuntosJson));
        MenuItem importarExcel = new MenuItem("Importar ubicaciones Excel");
        importarExcel.setOnAction(e -> ejecutarSeguro("importar Excel", this::cargarPuntosExcel));
        MenuItem plantilla = new MenuItem("Crear plantilla Excel");
        plantilla.setOnAction(e -> ejecutarSeguro("crear plantilla",
                this::crearPlantillaExcel));
        MenuItem guardar = new MenuItem("Guardar ubicaciones JSON");
        guardar.setOnAction(e -> ejecutarSeguro("guardar ubicaciones", this::guardarPuntos));
        archivo.getItems().addAll(importar, importarExcel, plantilla,
                new SeparatorMenuItem(), guardar);

        Menu simulacionMenu = new Menu("Simulación");
        MenuItem iniciar = new MenuItem("Iniciar / continuar");
        iniciar.setOnAction(e -> ejecutarSeguro("iniciar simulación",
                this::iniciarOReanudarSimulacion));
        MenuItem pausar = new MenuItem("Pausar");
        pausar.setOnAction(e -> ejecutarSeguro("pausar simulación", this::pausarSimulacion));
        MenuItem detener = new MenuItem("Detener / reiniciar");
        detener.setOnAction(e -> ejecutarSeguro("detener simulación", this::detenerSimulacion));
        simulacionMenu.getItems().addAll(iniciar, pausar, detener);

        Menu vistas = new Menu("Vistas");
        MenuItem alternarPanel = new MenuItem("Mostrar / ocultar (Tab)");
        alternarPanel.setOnAction(e -> alternarPanelLateral());
        MenuItem presentacion = new MenuItem("Modo presentación (F11)");
        presentacion.setOnAction(e -> alternarPresentacion());
        MenuItem vistaCompleta = new MenuItem("Ajustar mapa");
        vistaCompleta.setOnAction(e -> ejecutarSeguro("ajustar mapa", mapa::resetVistaPublico));
        vistas.getItems().addAll(alternarPanel, presentacion,
                new SeparatorMenuItem(), vistaCompleta);

        Menu ayuda = new Menu("Ayuda");
        MenuItem teclado = new MenuItem("Atajos: Tab panel · F11 presentación · Esc salir");
        teclado.setDisable(true);
        ayuda.getItems().add(teclado);
        MenuBar menuBar = new MenuBar(archivo, simulacionMenu, vistas, ayuda);

        HBox controles = new HBox(8);
        controles.setPadding(new Insets(7, 12, 7, 12));
        controles.setAlignment(Pos.CENTER_LEFT);
        controles.getStyleClass().add("toolbar");
        Label titulo = new Label("TRAFIQUEISHON");
        titulo.getStyleClass().add("tab-titulo");
        Button play = new Button("▶");
        play.setTooltip(new Tooltip("Iniciar / continuar"));
        play.setOnAction(e -> ejecutarSeguro("iniciar simulación",
                this::iniciarOReanudarSimulacion));
        Button pause = new Button("⏸");
        pause.setTooltip(new Tooltip("Pausar"));
        pause.setOnAction(e -> ejecutarSeguro("pausar simulación", this::pausarSimulacion));
        Button stop = new Button("⏹");
        stop.setTooltip(new Tooltip("Detener y limpiar"));
        stop.setOnAction(e -> ejecutarSeguro("detener simulación", this::detenerSimulacion));
        Button reset = new Button("↺");
        reset.setTooltip(new Tooltip("Reiniciar simulación"));
        reset.setOnAction(e -> ejecutarSeguro("reiniciar simulación", () -> {
            limpiarSimulacion(false);
            iniciarSimulacion();
        }));
        botonPanelMapa = new Button("Solo mapa");
        botonPanelMapa.setOnAction(e -> alternarPanelLateral());
        Button zoomOut = new Button("−");
        zoomOut.setOnAction(e -> mapa.zoomOut());
        Button zoomIn = new Button("+");
        zoomIn.setOnAction(e -> mapa.zoomIn());
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        controles.getChildren().addAll(titulo, new Separator(), play, pause, stop, reset,
                new Label("Velocidad"), velocidadSimulacion, botonPanelMapa, espacio,
                lblRelojStatus, lblCoords, lblZoom, zoomOut, zoomIn);
        return new VBox(menuBar, controles);
    }

    private TabPane crearPanelPestanas() {
        panelLateral.setPrefWidth(350);
        panelLateral.getStyleClass().add("panel-lateral");
        panelLateral.getTabs().addAll(
                pestana("📍 Puntos", crearPanelPuntos()),
                pestana("🛣 Rutas", new VBox(8, crearPanelTrayectos(), crearPanelFlota())),
                pestana("👥 Pasajeros", crearPanelPasajeros()),
                pestana("🚌 Simular", crearPanelSimulacion()),
                pestana("⚡ Eventos", crearPanelEventos()));
        return panelLateral;
    }

    private Tab pestana(String titulo, VBox contenido) {
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        Tab tab = new Tab(titulo, scroll);
        tab.setClosable(false);
        return tab;
    }

    private void alternarPanelLateral() {
        boolean mostrar = root.getLeft() == null;
        root.setLeft(mostrar ? panelLateral : null);
        if (botonPanelMapa != null) botonPanelMapa.setText(mostrar ? "Solo mapa" : "Panel");
    }

    private void alternarPresentacion() {
        presentacionActiva = !presentacionActiva;
        if (presentacionActiva) {
            panelVisibleAntesPresentacion = root.getLeft() != null;
            root.setTop(null);
            root.setLeft(null);
            root.setBottom(null);
        } else {
            root.setTop(barraSuperior);
            root.setLeft(panelVisibleAntesPresentacion ? panelLateral : null);
            root.setBottom(barraEstado);
            if (botonPanelMapa != null) {
                botonPanelMapa.setText(panelVisibleAntesPresentacion ? "Solo mapa" : "Panel");
            }
        }
    }

    private VBox panelBase(String titulo) {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(10));
        Label label = new Label(titulo);
        label.getStyleClass().add("tab-titulo");
        panel.getChildren().add(label);
        return panel;
    }

    private VBox crearPanelPuntos() {
        VBox panel = panelBase("Puntos de ruta editables");
        listaPuntos.setPlaceholder(new Label("Sin ubicaciones cargadas."));
        listaPuntos.setPrefHeight(190);
        listaPuntos.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PuntoImportante punto, boolean vacio) {
                super.updateItem(punto, vacio);
                setText(vacio || punto == null ? null : punto.tipo() + " · " + punto.nombre()
                        + String.format(" (%.5f, %.5f)", punto.lon(), punto.lat()));
            }
        });
        Button agregarMapa = boton("Agregar haciendo clic en mapa", () -> {
            colocandoPunto = true;
            colocandoEvento = false;
            lblEstado.setText("Haz clic en el mapa para colocar la ubicación.");
        });
        Button agregarManual = boton("Agregar coordenadas manualmente", () ->
                editarPunto(null, null));
        Button editar = boton("Editar seleccionado", () ->
                editarPunto(listaPuntos.getSelectionModel().getSelectedItem(), null));
        Button quitar = boton("Eliminar seleccionado", this::eliminarPunto);
        Button guardar = boton("Guardar ubicaciones JSON", this::guardarPuntos);
        Button cargarJson = boton("Importar JSON", this::cargarPuntosJson);
        Button cargarExcel = boton("Importar Excel", this::cargarPuntosExcel);
        Button plantilla = boton("Crear plantilla Excel", this::crearPlantillaExcel);
        panel.getChildren().addAll(agregarMapa, agregarManual, editar, quitar, listaPuntos,
                guardar, cargarJson, cargarExcel, plantilla);
        return panel;
    }

    private VBox crearPanelTrayectos() {
        VBox panel = panelBase("Calcular, consultar y exportar");
        Button partida = boton("Calcular PARTIDA", () -> calcular("PARTIDA"));
        Button retorno = boton("Calcular RETORNO", () -> calcular("RETORNO"));
        listaTrayectos.setPlaceholder(new Label("Sin trayectos calculados."));
        listaTrayectos.setPrefHeight(160);
        Button enfocarPartida = boton("Enfocar ruta de ida", () -> enfocarRuta("PARTIDA"));
        Button enfocarRetorno = boton("Enfocar ruta de vuelta", () -> enfocarRuta("RETORNO"));
        Button geojson = boton("Exportar GeoJSON", () -> exportar("geojson"));
        Button gpx = boton("Exportar GPX", () -> exportar("gpx"));
        Button csv = boton("Exportar CSV", () -> exportar("csv"));
        Button limpiar = boton("Limpiar trayectos", () -> {
            gestor.limpiar();
            listaTrayectos.getItems().clear();
            mapa.setTrayectoPartida(null);
            mapa.setTrayectoRetorno(null);
            chkIdaNeon.setSelected(false);
            chkVueltaNeon.setSelected(false);
            actualizarVisualizacionRutas();
        });
        panel.getChildren().addAll(partida, retorno, enfocarPartida, enfocarRetorno,
                listaTrayectos, geojson, gpx, csv, limpiar);
        return panel;
    }

    private VBox crearPanelFlota() {
        VBox panel = panelBase("Rutas predefinidas de micros");
        Button crearRuta = boton("Crear ruta de micro", this::crearRutaMicro);
        Button eliminarRuta = boton("Eliminar ruta seleccionada", this::eliminarRutaMicro);
        ScrollPane lista = new ScrollPane(filasRutasMicros);
        lista.setFitToWidth(true);
        lista.setPrefHeight(260);
        lista.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        panel.getChildren().addAll(new Label(
                "Cada micro sigue sus paraderos en orden y sale según su frecuencia."),
                crearRuta, lista, eliminarRuta);
        return panel;
    }

    private VBox crearPanelPasajeros() {
        VBox panel = panelBase("Demanda de pasajeros por salida");
        pasajerosSimulados.setEditable(true);
        panel.getChildren().addAll(new Label("Pasajeros que esperan abordar"),
                pasajerosSimulados, resumenPasajeros,
                new Label("Factores de demanda: mañana 2.5x · mediodía 1.8x · tarde 2.8x · "
                        + "noche 0.2x."),
                new Label("Capacidad máxima: 18 pasajeros por micro despachada."));
        actualizarResumenPasajeros();
        return panel;
    }

    private VBox crearPanelSimulacion() {
        VBox panel = panelBase("Simulación en tiempo real");
        Button iniciar = boton("Iniciar simulación", this::iniciarSimulacion);
        Button pausar = boton("Pausar simulación", this::pausarSimulacion);
        Button detener = boton("Detener simulación", this::detenerSimulacion);
        panel.getChildren().addAll(new Label("Hora inicial (salidas desde paraderos)"),
                new HBox(6, new Label("Hora"), horaInicio, new Label("Minuto"), minutoInicio),
                lblHoraSimulada, chkIdaNeon, chkVueltaNeon, iniciar, pausar, detener);
        return panel;
    }

    private void iniciarOReanudarSimulacion() {
        if (simulador.haySimulacion()) {
            simulador.reanudar();
            lblEstado.setText("Simulación reanudada.");
        } else {
            iniciarSimulacion();
        }
    }

    private void pausarSimulacion() {
        simulador.pausar();
        lblEstado.setText("Simulación pausada.");
    }

    private void detenerSimulacion() {
        limpiarSimulacion(true);
    }

    private void limpiarSimulacion(boolean mostrarResultados) {
        int transportados = simulador.pasajerosTransportados();
        double ocupacion = simulador.ocupacionPromedio() * 100;
        int incidencias = simulador.vehiculosConIncidencia();
        boolean habiaSimulacion = simulador.haySimulacion();
        simulador.limpiar();
        mapa.setVehiculos(List.of());
        lblEstado.setText("Simulación detenida y estado reiniciado.");
        if (mostrarResultados && habiaSimulacion) {
            Alert resultados = new Alert(Alert.AlertType.INFORMATION);
            resultados.setTitle("Resultados de simulación");
            resultados.setHeaderText("Resumen de la ejecución");
            resultados.setContentText("Abordajes simulados: " + transportados
                    + "\nOcupación media de la flota: " + String.format("%.1f%%", ocupacion)
                    + "\nVehículos con ruta pendiente o avería: " + incidencias
                    + "\nTiempo simulado: " + String.format("%.1f min",
                    simulador.segundosTranscurridos() / 60));
            resultados.show();
        }
    }

    private void actualizarResumenPasajeros() {
        resumenPasajeros.setText("Demanda base por ruta: " + pasajerosSimulados.getValue()
                + " pasajeros; ajustada por la hora del día.");
    }

    private void actualizarHoraPrevia() {
        if (simulador.estaEjecutando()) return;
        String hora = String.format("%02d:%02d", horaInicio.getValue(), minutoInicio.getValue());
        lblHoraSimulada.setText("Hora inicial: " + hora);
        lblRelojStatus.setText("Hora: " + hora);
    }

    private VBox crearPanelEventos() {
        VBox panel = panelBase("Incidentes viales");
        nombreEvento.setPromptText("Nombre del evento (opcional)");
        tipoEvento.setMaxWidth(Double.MAX_VALUE);
        Button ubicar = boton("Ubicar evento en el mapa", () -> {
            colocandoEvento = true;
            colocandoPunto = false;
            lblEstado.setText("Haz clic sobre la vía afectada para colocar el evento.");
        });
        listaEventos.setPlaceholder(new Label("Sin eventos activos."));
        listaEventos.setPrefHeight(160);
        Button quitar = boton("Eliminar evento seleccionado", this::eliminarEvento);
        panel.getChildren().addAll(new Label("Tipo de evento"), tipoEvento,
                nombreEvento, ubicar, listaEventos, quitar, chkMapaCalor);
        return panel;
    }

    private void crearRutaMicro() {
        editarRutaMicro(null);
    }

    private void editarRutaMicro(ServicioMicro existente) {
        if (grafo == null || puntos.size() < 2) {
            lblEstado.setText("Carga al menos dos paraderos antes de definir una ruta.");
            return;
        }
        Dialog<Boolean> dialogo = new Dialog<>();
        dialogo.setTitle(existente == null ? "Crear ruta de micro" : "Editar ruta de micro");
        dialogo.setHeaderText("Configura la línea, sus paraderos, frecuencia y color fluorescente.");
        ButtonType guardar = new ButtonType("Guardar ruta", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);

        TextField nombre = new TextField(existente == null
                ? "Micro " + (rutasMicros.size() + 1) : existente.nombre());
        Spinner<Integer> frecuencia = new Spinner<>(1, 180,
                existente == null ? 10 : existente.frecuenciaMinutos());
        Spinner<Integer> unidades = new Spinner<>(1, 50,
                existente == null ? 4 : existente.unidadesDisponibles());
        Spinner<Integer> pasajeros = new Spinner<>(0, 900,
                existente == null ? pasajerosSimulados.getValue()
                        : existente.pasajerosEsperando());
        ColorPicker color = new ColorPicker(Color.web(existente == null
                ? siguienteColorMicro() : existente.color()));
        ListView<PuntoImportante> disponibles = new ListView<>();
        disponibles.getItems().setAll(puntos);
        disponibles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        disponibles.setPrefHeight(230);
        disponibles.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PuntoImportante punto, boolean vacio) {
                super.updateItem(punto, vacio);
                setText(vacio || punto == null ? null : punto.nombre());
            }
        });
        ListView<PuntoImportante> secuencia = new ListView<>(FXCollections.observableArrayList(
                existente == null ? List.of() : existente.trayecto().puntos()));
        secuencia.setPrefHeight(230);
        secuencia.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PuntoImportante punto, boolean vacio) {
                super.updateItem(punto, vacio);
                setText(vacio || punto == null ? null
                        : (getIndex() + 1) + ". " + punto.nombre());
            }
        });
        Button agregar = new Button("Añadir →");
        agregar.setOnAction(e -> {
            for (PuntoImportante punto : disponibles.getItems()) {
                if (disponibles.getSelectionModel().isSelected(
                        disponibles.getItems().indexOf(punto))
                        && !secuencia.getItems().contains(punto)) {
                    secuencia.getItems().add(punto);
                }
            }
        });
        Button subir = new Button("↑");
        subir.setOnAction(e -> moverParadero(secuencia, -1));
        Button bajar = new Button("↓");
        bajar.setOnAction(e -> moverParadero(secuencia, 1));
        Button quitar = new Button("Quitar");
        quitar.setOnAction(e -> {
            int indice = secuencia.getSelectionModel().getSelectedIndex();
            if (indice >= 0) secuencia.getItems().remove(indice);
        });
        VBox acciones = new VBox(8, agregar, subir, bajar, quitar);
        acciones.setAlignment(Pos.CENTER);
        VBox listaDisponibles = new VBox(5, new Label("Ubicaciones disponibles"),
                disponibles);
        VBox listaSecuencia = new VBox(5, new Label("Paraderos en orden de recorrido"),
                secuencia);
        HBox selectorParaderos = new HBox(8, listaDisponibles, acciones, listaSecuencia);
        HBox.setHgrow(listaDisponibles, Priority.ALWAYS);
        HBox.setHgrow(listaSecuencia, Priority.ALWAYS);
        GridPane contenido = new GridPane();
        contenido.setHgap(8);
        contenido.setVgap(8);
        contenido.addRow(0, new Label("Nombre de micro/ruta"), nombre);
        contenido.addRow(1, new Label("Frecuencia (minutos)"), frecuencia);
        contenido.addRow(2, new Label("Unidades de la ruta"), unidades);
        contenido.addRow(3, new Label("Demanda de pasajeros"), pasajeros);
        contenido.addRow(4, new Label("Color único de ruta"), color);
        contenido.add(new Label(
                "Terminales: dos puntos más alejados; ajusta el orden de las paradas (mínimo 2)"),
                0, 5);
        contenido.add(selectorParaderos, 0, 6, 2, 1);
        dialogo.getDialogPane().setContent(contenido);
        dialogo.setResultConverter(boton -> boton == guardar);
        Optional<Boolean> confirmado = dialogo.showAndWait();
        if (confirmado.isEmpty() || !confirmado.get()) return;

        String rutaNombre = nombre.getText().trim();
        List<PuntoImportante> seleccionados = List.copyOf(secuencia.getItems());
        if (rutaNombre.isBlank() || seleccionados.size() < 2) {
            mostrarError("Ruta incompleta", "Indica un nombre y selecciona al menos dos paraderos.");
            return;
        }
        try {
            String id = existente == null ? "micro-" + java.util.UUID.randomUUID()
                    : existente.id();
            String colorHex = colorHex(color.getValue());
            boolean colorEnUso = rutasMicros.stream().anyMatch(ruta ->
                    !ruta.id().equals(id) && ruta.color().equalsIgnoreCase(colorHex));
            if (colorEnUso) {
                mostrarError("Color repetido", "Cada ruta de micro necesita un color distinto.");
                return;
            }
            CalculadorTrayecto calculador = new CalculadorTrayecto(grafo);
            Trayecto trayecto = calculador.calcularOrdenOptimo(id, rutaNombre,
                    "Trafiqueishon", "SERVICIO", seleccionados);
            List<PuntoImportante> puntosVuelta = new ArrayList<>(trayecto.puntos());
            java.util.Collections.reverse(puntosVuelta);
            Trayecto vuelta = calculador.calcular(id + "-vuelta",
                    rutaNombre + " · vuelta", "Trafiqueishon", "SERVICIO_VUELTA",
                    puntosVuelta);
            ServicioMicro servicio = new ServicioMicro(id, rutaNombre, trayecto, vuelta,
                    frecuencia.getValue(), unidades.getValue(), pasajeros.getValue(), colorHex);
            if (existente == null) {
                rutasMicros.add(servicio);
                rutasMicrosVisibles.add(id);
            } else {
                int indice = rutasMicros.indexOf(existente);
                rutasMicros.set(indice, servicio);
            }
            gestor.agregar(trayecto);
            gestor.eliminar(id + "-vuelta");
            actualizarFilasRutasMicro();
            actualizarVisualizacionRutas();
            guardarRutasMicros();
            lblEstado.setText(rutaNombre + ": " + seleccionados.size() + " paraderos, cada "
                    + frecuencia.getValue() + " min, inicia en "
                    + trayecto.puntos().getFirst().nombre()
                    + " · " + trayecto.resumen());
        } catch (RuntimeException ex) {
            lblEstado.setText("No se pudo guardar la ruta: " + ex.getMessage());
        }
    }

    private String siguienteColorMicro() {
        Set<String> usados = rutasMicros.stream()
                .map(ruta -> ruta.color().toUpperCase()).collect(java.util.stream.Collectors.toSet());
        for (String candidato : COLORES_MICROS) {
            if (!usados.contains(candidato)) return candidato;
        }
        for (int i = 0; i < 360; i++) {
            String candidato = colorHex(Color.hsb(i, 1, 1));
            if (!usados.contains(candidato)) return candidato;
        }
        throw new IllegalStateException("No se encontró un color libre para la ruta.");
    }

    private String colorHex(Color color) {
        return String.format("#%02X%02X%02X", (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
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
                    .map(PuntoImportante::nombre)
                    .collect(java.util.stream.Collectors.joining(" → ")));
            recorrido.setWrapText(true);
            recorrido.setStyle("-fx-text-fill: #CDD6F4; -fx-font-size: 10px;");
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
            VBox informacion = new VBox(3, visible, recorrido);
            HBox fila = new HBox(6, informacion, editar, eliminar);
            fila.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(informacion, Priority.ALWAYS);
            filasRutasMicros.getChildren().add(fila);
        }
    }

    private void moverParadero(ListView<PuntoImportante> secuencia, int desplazamiento) {
        int actual = secuencia.getSelectionModel().getSelectedIndex();
        int destino = actual + desplazamiento;
        if (actual < 0 || destino < 0 || destino >= secuencia.getItems().size()) return;
        java.util.Collections.swap(secuencia.getItems(), actual, destino);
        secuencia.getSelectionModel().select(destino);
    }

    private void eliminarRutaMicro() {
        if (rutasMicros.isEmpty()) {
            lblEstado.setText("No hay rutas de micros para quitar.");
            return;
        }
        ServicioMicro ultima = rutasMicros.removeLast();
        rutasMicrosVisibles.remove(ultima.id());
        gestor.eliminar(ultima.id());
        actualizarFilasRutasMicro();
        actualizarVisualizacionRutas();
        guardarRutasMicros();
        lblEstado.setText("Ruta eliminada: " + ultima.nombre());
    }

    private void cargarRutasMicrosGuardadas() {
        try {
            boolean repararColores = false;
            for (RepositorioServiciosMicro.RutaGuardada guardada : repositorioMicros.cargar()) {
                try {
                    CalculadorTrayecto calculador = new CalculadorTrayecto(grafo);
                    Trayecto ida = calculador.calcular(guardada.id(), guardada.nombre(),
                            "Trafiqueishon", "SERVICIO", guardada.paraderos());
                    List<PuntoImportante> paraderosVuelta = new ArrayList<>(guardada.paraderos());
                    java.util.Collections.reverse(paraderosVuelta);
                    Trayecto vuelta = calculador.calcular(guardada.id() + "-vuelta",
                            guardada.nombre() + " · vuelta", "Trafiqueishon",
                            "SERVICIO_VUELTA", paraderosVuelta);
                    String colorGuardado = colorHex(Color.web(guardada.color()));
                    String color = colorGuardado;
                    if (rutasMicros.stream().anyMatch(r -> r.color().equalsIgnoreCase(colorGuardado))) {
                        color = siguienteColorMicro();
                        repararColores = true;
                    }
                    ServicioMicro servicio = new ServicioMicro(guardada.id(), guardada.nombre(),
                            ida, vuelta, guardada.frecuenciaMinutos(),
                            guardada.unidadesDisponibles(), guardada.pasajerosEsperando(),
                            color);
                    rutasMicros.add(servicio);
                    rutasMicrosVisibles.add(servicio.id());
                    gestor.agregar(ida);
                } catch (RuntimeException ex) {
                    lblEstado.setText("Se omitió la ruta guardada '" + guardada.nombre()
                            + "': " + ex.getMessage());
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
        }
    }

    private void guardarRutasMicros() {
        try {
            repositorioMicros.guardar(rutasMicros);
        } catch (IOException ex) {
            lblEstado.setText("No se pudieron guardar las rutas: " + ex.getMessage());
        }
    }

    private void actualizarVisualizacionRutas() {
        mapa.setRutasMicros(rutasMicros, rutasMicrosVisibles);
        mapa.setMostrarRutasNeon(chkIdaNeon.isSelected(), chkVueltaNeon.isSelected());
    }

    private Button boton(String texto, Runnable accion) {
        Button boton = new Button(texto);
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setOnAction(evento -> ejecutarSeguro("acción '" + texto + "'", accion));
        return boton;
    }

    private void ejecutarSeguro(String operacion, Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException ex) {
            LOG.log(Level.SEVERE, "Falló " + operacion, ex);
            lblEstado.setText("No se pudo completar " + operacion + ": " + ex.getMessage());
        }
    }

    private HBox crearStatusBar() {
        HBox bar = new HBox(15);
        bar.setPadding(new Insets(6, 12, 6, 12));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("status-bar");
        bar.getChildren().addAll(lblCoords,
                new Separator(javafx.geometry.Orientation.VERTICAL), lblZoom,
                new Separator(javafx.geometry.Orientation.VERTICAL), lblEstado);
        return bar;
    }

    private void procesarClicMapa(double lon, double lat) {
        if (colocandoPunto) {
            colocandoPunto = false;
            editarPunto(null, new double[]{lon, lat});
        } else if (colocandoEvento) {
            colocandoEvento = false;
            agregarEvento(lon, lat);
        }
    }

    private void editarPunto(PuntoImportante existente, double[] coordenadas) {
        Dialog<PuntoImportante> dialogo = new Dialog<>();
        dialogo.setTitle(existente == null ? "Nueva ubicación" : "Editar ubicación");
        dialogo.setHeaderText("Nombre y coordenadas geográficas (longitud, latitud)");
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
            } catch (IllegalArgumentException ex) {
                lblEstado.setText("Ubicación inválida: " + ex.getMessage());
                return null;
            }
        });
        Optional<PuntoImportante> resultado = dialogo.showAndWait();
        resultado.ifPresent(punto -> {
            boolean idDuplicado = puntos.stream()
                    .anyMatch(actual -> actual.id().equals(punto.id()) && actual != existente);
            if (idDuplicado) {
                mostrarError("ID duplicado", "Cada ubicación debe tener un ID único.");
                return;
            }
            if (existente != null) puntos.remove(existente);
            puntos.add(punto);
            actualizarListaPuntos();
            lblEstado.setText("Ubicación actualizada. Guarda el JSON para conservar cambios.");
        });
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
            if (puntos.stream().noneMatch(punto -> punto.id().equals(id))) return id;
        }
    }

    private void actualizarListaPuntos() {
        listaPuntos.getItems().setAll(puntos);
        mapa.setPuntos(puntos);
    }

    private void agregarEvento(double lon, double lat) {
        TipoEvento tipo = tipoEvento.getValue();
        if (tipo == null) return;
        String nombre = nombreEvento.getText().isBlank()
                ? tipo + " " + (eventos.size() + 1) : nombreEvento.getText().trim();
        eventos.add(new EventoTrafico(nombre, tipo, lon, lat));
        gestorEventos.publicar(eventos);
        lblEstado.setText("Evento agregado: " + nombre
                + ". Las rutas se recalculan sin detener la simulación.");
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
        eventos.clear();
        eventos.addAll(nuevosEventos);
        simulador.actualizarEventos(eventos);
        listaEventos.getItems().setAll(eventos);
        mapa.setEventos(eventos);
        aplicarEventosAlGrafo();
    }

    private void aplicarEventosAlGrafo() {
        if (grafo == null) return;
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
    }

    private void recalcularServiciosPorEventos() {
        if (grafo == null || rutasMicros.isEmpty()) return;
        CalculadorTrayecto calculador = new CalculadorTrayecto(grafo);
        for (int i = 0; i < rutasMicros.size(); i++) {
            ServicioMicro servicio = rutasMicros.get(i);
            try {
                Trayecto ida = calculador.calcular(servicio.id(), servicio.nombre(),
                        servicio.trayecto().empresa(), "SERVICIO",
                        servicio.trayecto().puntos());
                Trayecto vuelta = calculador.calcular(servicio.id() + "-vuelta",
                        servicio.nombre() + " · vuelta", servicio.trayectoVuelta().empresa(),
                        "SERVICIO_VUELTA", servicio.trayectoVuelta().puntos());
                ServicioMicro actualizado = new ServicioMicro(servicio.id(), servicio.nombre(),
                        ida, vuelta, servicio.frecuenciaMinutos(),
                        servicio.unidadesDisponibles(), servicio.pasajerosEsperando(),
                        servicio.color());
                rutasMicros.set(i, actualizado);
                gestor.agregar(ida);
            } catch (RuntimeException ex) {
                lblEstado.setText("No se pudo recalcular " + servicio.nombre()
                        + " tras el evento: " + ex.getMessage());
            }
        }
        simulador.actualizarServicios(rutasMicros);
        actualizarFilasRutasMicro();
        actualizarVisualizacionRutas();
        guardarRutasMicros();
        if (simulador.estaEjecutando()) {
            lblEstado.setText("Eventos aplicados; simulación activa y rutas recalculadas con A*.");
        }
    }

    private double distanciaPuntoTramo(double lon, double lat, Edge edge) {
        double escalaX = 111_320 * Math.cos(Math.toRadians(lat));
        double ax = edge.origen.lon * escalaX;
        double ay = edge.origen.lat * 111_320;
        double bx = edge.destino.lon * escalaX;
        double by = edge.destino.lat * 111_320;
        double px = lon * escalaX;
        double py = lat * 111_320;
        double dx = bx - ax, dy = by - ay;
        double longitud2 = dx * dx + dy * dy;
        double t = longitud2 == 0 ? 0
                : Math.clamp(((px - ax) * dx + (py - ay) * dy) / longitud2, 0, 1);
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

    private static double distanciaMetros(double lon1, double lat1, double lon2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * 6_371_000 * Math.asin(Math.sqrt(a));
    }

    private void iniciarSimulacion() {
        List<ServicioMicro> seleccionadas = rutasMicros.stream()
                .filter(ruta -> rutasMicrosVisibles.contains(ruta.id()))
                .toList();
        if (seleccionadas.isEmpty()) {
            lblEstado.setText("Activa al menos una ruta en «Rutas y micros disponibles».");
            return;
        }
        try {
            PerfilHoraPunta.aplicar(grafo,
                    horaInicio.getValue() * 60 + minutoInicio.getValue());
            List<ServicioMicro> actualizadas = new ArrayList<>();
            for (ServicioMicro ruta : seleccionadas) {
                Trayecto trayectoActualizado = new CalculadorTrayecto(grafo).calcular(
                        ruta.id(), ruta.nombre(), ruta.trayecto().empresa(), "SERVICIO",
                        ruta.trayecto().puntos());
                Trayecto vueltaActualizada = new CalculadorTrayecto(grafo).calcular(
                        ruta.id() + "-vuelta", ruta.nombre() + " · vuelta",
                        ruta.trayecto().empresa(), "SERVICIO_VUELTA",
                        ruta.trayectoVuelta().puntos());
                actualizadas.add(new ServicioMicro(ruta.id(), ruta.nombre(), trayectoActualizado,
                        vueltaActualizada,
                        ruta.frecuenciaMinutos(), ruta.unidadesDisponibles(),
                        ruta.pasajerosEsperando(), ruta.color()));
            }
            simulador.setVelocidadSimulacion(velocidadSimulacion.getValue());
            simulador.iniciar(actualizadas, horaInicio.getValue(), minutoInicio.getValue());
            ultimoTick = 0;
            mapa.setVehiculos(simulador.posiciones());
            PerfilHoraPunta.aplicar(grafo, simulador.horaActualMinutos());
            ultimoMinutoHoraPunta = simulador.horaActualMinutos();
            int hora = simulador.horaActualMinutos();
            lblHoraSimulada.setText("Hora simulada: " + String.format("%02d:%02d", hora / 60,
                    hora % 60));
            lblRelojStatus.setText("Hora: " + String.format("%02d:%02d", hora / 60, hora % 60));
            lblEstado.setText("Servicios activos: " + actualizadas.size()
                    + ". Salidas programadas desde el primer paradero, según frecuencia.");
        } catch (RuntimeException ex) {
            lblEstado.setText("No se pudo iniciar la simulación: " + ex.getMessage());
        }
    }

    private void enfocarRuta(String sentido) {
        Trayecto trayecto = gestor.getTrayectos().stream()
                .filter(t -> sentido.equals(t.sentido()))
                .reduce((anterior, actual) -> actual)
                .orElse(null);
        if (trayecto == null) {
            lblEstado.setText("Calcula primero la ruta de " + sentido.toLowerCase() + ".");
            return;
        }
        mapa.enfocarTrayecto(trayecto);
        lblEstado.setText("Vista enfocada en " + sentido.toLowerCase() + ": "
                + trayecto.resumen());
    }

    private void cargarPuntosDesdeRecurso() {
        try {
            puntos.clear();
            puntos.addAll(lectorJson.leerRecurso("/datos/puntos.json"));
            actualizarListaPuntos();
            lblEstado.setText("Red lista. " + puntos.size() + " ubicaciones cargadas.");
        } catch (IOException ex) {
            lblEstado.setText("No se pudieron cargar las ubicaciones: " + ex.getMessage());
        }
    }

    private void cargarPuntosJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importar ubicaciones JSON");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File archivo = chooser.showOpenDialog(root.getScene().getWindow());
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
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File archivo = chooser.showSaveDialog(root.getScene().getWindow());
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
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File archivo = chooser.showOpenDialog(root.getScene().getWindow());
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
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File archivo = chooser.showSaveDialog(root.getScene().getWindow());
        if (archivo == null) return;
        try (var libro = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var hoja = libro.createSheet("Puntos");
            var cabecera = hoja.createRow(0);
            String[] columnas = {"id", "nombre", "lon", "lat", "tipo"};
            for (int i = 0; i < columnas.length; i++) cabecera.createCell(i).setCellValue(columnas[i]);
            try (var salida = java.nio.file.Files.newOutputStream(archivo.toPath())) {
                libro.write(salida);
            }
            lblEstado.setText("Plantilla creada: " + archivo.getAbsolutePath());
        } catch (IOException ex) {
            lblEstado.setText("Error al crear plantilla: " + ex.getMessage());
        }
    }

    private void calcular(String sentido) {
        if (grafo == null) {
            lblEstado.setText("La red vial todavía no está lista.");
            return;
        }
        if (puntos.size() < 2) {
            lblEstado.setText("Se necesitan al menos 2 ubicaciones.");
            return;
        }
        try {
            List<PuntoImportante> orden = new ArrayList<>(puntos);
            if ("RETORNO".equals(sentido)) java.util.Collections.reverse(orden);
            Trayecto trayecto = new CalculadorTrayecto(grafo).calcular(
                    sentido.toLowerCase() + "_" + System.currentTimeMillis(),
                    "Trayecto " + sentido, "Trafiqueishon", sentido, orden);
            gestor.agregar(trayecto);
            if ("PARTIDA".equals(sentido)) {
                mapa.setTrayectoPartida(trayecto);
            } else {
                mapa.setTrayectoRetorno(trayecto);
            }
            actualizarVisualizacionRutas();
            listaTrayectos.getItems().add(sentido + " · " + trayecto.nombre()
                    + " · " + trayecto.resumen());
            lblEstado.setText("Trayecto " + sentido + ": " + trayecto.resumen());
        } catch (RuntimeException ex) {
            lblEstado.setText("Error al calcular el trayecto: " + ex.getMessage());
        }
    }

    private void exportar(String formato) {
        if (gestor.getTrayectos().isEmpty()) {
            lblEstado.setText("Calcula primero un trayecto.");
            return;
        }
        Trayecto trayecto = gestor.getTrayectos().getLast();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar trayecto");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                formato.toUpperCase(), "*." + formato));
        chooser.setInitialFileName(trayecto.nombre().replaceAll("[^A-Za-z0-9_-]", "_")
                + "." + formato);
        File archivo = chooser.showSaveDialog(root.getScene().getWindow());
        if (archivo == null) return;
        try {
            Path destino = archivo.toPath();
            switch (formato) {
                case "geojson" -> expGeoJson.exportar(trayecto, destino);
                case "gpx" -> expGpx.exportar(trayecto, destino);
                case "csv" -> expCsv.exportar(trayecto, destino);
                default -> throw new IllegalArgumentException("Formato de exportación desconocido.");
            }
            lblEstado.setText("Exportado: " + destino);
        } catch (IOException | RuntimeException ex) {
            lblEstado.setText("Error al exportar: " + ex.getMessage());
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }

    public void cerrar() {
        relojSimulacion.stop();
        simulador.detener();
        mapa.cerrar();
    }
}
