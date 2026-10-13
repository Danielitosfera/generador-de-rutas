package trafiqueishon.ui;

import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
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

public class VentanaPrincipal {
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
    private final Slider velocidadSimulacion = new Slider(0.25, 10, 1);

    private final GestorTrayectos gestor = new GestorTrayectos();
    private final SimuladorBuses simulador = new SimuladorBuses();
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
            "#39FF14", "#FFD60A", "#FF6D00", "#D500F9", "#00E5FF",
            "#FF2D95", "#B2FF59", "#FF9100", "#7C4DFF", "#64FFDA");
    private boolean colocandoPunto;
    private boolean colocandoEvento;
    private long ultimoTick;
    private int ultimoMinutoHoraPunta = -1;

    private final AnimationTimer relojSimulacion = new AnimationTimer() {
        @Override
        public void handle(long ahora) {
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
            ultimoTick = ahora;
        }
    };

    public VentanaPrincipal() {
        root.getStyleClass().add("root-view");
        root.setCenter(mapa);
        root.setTop(crearToolbar());
        root.setLeft(crearMenuAcordeon());
        root.setBottom(crearStatusBar());

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
        velocidadSimulacion.valueProperty().addListener((obs, anterior, valor) -> {
            simulador.setVelocidadSimulacion(valor.doubleValue());
            lblEstado.setText(String.format("Velocidad de simulación: %.2fx", valor.doubleValue()));
        });

        mapa.setOnMapaCargado(() -> {
            grafo = mapa.getGrafo();
            lblEstado.setText("Red lista: " + grafo.numNodos() + " nodos, "
                    + grafo.numEdges() + " tramos.");
            cargarPuntosDesdeRecurso();
            cargarRutasMicrosGuardadas();
        });
        mapa.setOnError(msg -> lblEstado.setText(msg));
        mapa.setOnClic(this::procesarClicMapa);
        mapa.setOnMouseMoveCallback(lonlat -> {
            lblCoords.setText(String.format("Long.: %.5f  Lat.: %.5f", lonlat[0], lonlat[1]));
            lblZoom.setText(String.format("Zoom: %.2f", mapa.getZoom()));
        });
        relojSimulacion.start();
        actualizarHoraPrevia();
    }

    private HBox crearToolbar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(10, 14, 10, 14));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("toolbar");
        Label titulo = new Label("TRAFIQUEISHON / SIMULADOR DE BUSES");
        titulo.getStyleClass().add("tab-titulo");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Button zoomOut = new Button("−");
        Button zoomIn = new Button("+");
        Button reset = new Button("Vista completa");
        Button plaza = new Button("Plaza");
        Button terminal = new Button("Terminal");
        zoomOut.setOnAction(e -> mapa.zoomOut());
        zoomIn.setOnAction(e -> mapa.zoomIn());
        reset.setOnAction(e -> mapa.resetVistaPublico());
        plaza.setOnAction(e -> mapa.centrarEn(-70.133, -15.499, 4));
        terminal.setOnAction(e -> mapa.centrarEn(-70.140, -15.490, 4));
        bar.getChildren().addAll(titulo, espacio, zoomOut, zoomIn, reset,
                new Separator(), plaza, terminal);
        return bar;
    }

    private ScrollPane crearMenuAcordeon() {
        Accordion acordeon = new Accordion();
        acordeon.setPrefWidth(330);
        acordeon.getStyleClass().add("panel-lateral");
        TitledPane ubicaciones = new TitledPane("Ubicaciones", crearPanelPuntos());
        TitledPane trayectos = new TitledPane("Trayectos", crearPanelTrayectos());
        TitledPane flota = new TitledPane("Rutas y micros disponibles", crearPanelFlota());
        TitledPane pasajeros = new TitledPane("Pasajeros", crearPanelPasajeros());
        TitledPane simulacion = new TitledPane("Simulación de buses", crearPanelSimulacion());
        TitledPane eventos = new TitledPane("Eventos y mapa de calor", crearPanelEventos());
        acordeon.getPanes().addAll(ubicaciones, trayectos, flota, pasajeros, simulacion, eventos);
        acordeon.setExpandedPane(ubicaciones);
        ScrollPane desplazamiento = new ScrollPane(acordeon);
        desplazamiento.setFitToWidth(true);
        desplazamiento.setPrefWidth(350);
        desplazamiento.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazamiento.getStyleClass().add("panel-lateral");
        return desplazamiento;
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
                new Label("Máximo 18 pasajeros por micro despachada."));
        actualizarResumenPasajeros();
        return panel;
    }

    private VBox crearPanelSimulacion() {
        VBox panel = panelBase("Simulación en tiempo real");
        velocidadSimulacion.setShowTickLabels(true);
        velocidadSimulacion.setMajorTickUnit(2);
        Button iniciar = boton("Iniciar simulación", this::iniciarSimulacion);
        Button detener = boton("Detener simulación", () -> {
            simulador.detener();
            mapa.setVehiculos(List.of());
            lblEstado.setText("Simulación detenida.");
        });
        panel.getChildren().addAll(new Label("Hora inicial (salidas desde paraderos)"),
                new HBox(6, new Label("Hora"), horaInicio, new Label("Minuto"), minutoInicio),
                lblHoraSimulada, new Label("Velocidad del reloj"), velocidadSimulacion,
                chkIdaNeon, chkVueltaNeon, iniciar, detener);
        return panel;
    }

    private void actualizarResumenPasajeros() {
        resumenPasajeros.setText("Demanda por ruta: " + pasajerosSimulados.getValue()
                + " pasajeros; se despachan hasta 18 por salida.");
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
        if (grafo == null || puntos.size() < 2) {
            lblEstado.setText("Carga al menos dos paraderos antes de definir una ruta.");
            return;
        }
        Dialog<Boolean> dialogo = new Dialog<>();
        dialogo.setTitle("Definir ruta preestablecida");
        dialogo.setHeaderText("Selecciona los paraderos en el orden mostrado; el micro saldrá del primero.");
        ButtonType guardar = new ButtonType("Crear ruta", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);

        TextField nombre = new TextField("Micro " + (rutasMicros.size() + 1));
        Spinner<Integer> frecuencia = new Spinner<>(1, 180, 10);
        Spinner<Integer> unidades = new Spinner<>(1, 50, 4);
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
        ListView<PuntoImportante> secuencia = new ListView<>(
                FXCollections.observableArrayList());
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
        contenido.add(new Label("El orden de la derecha define la ruta (mínimo 2)"), 0, 3);
        contenido.add(selectorParaderos, 0, 4, 2, 1);
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
            String id = "micro-" + java.util.UUID.randomUUID();
            CalculadorTrayecto calculador = new CalculadorTrayecto(grafo);
            Trayecto trayecto = calculador.calcularOrdenOptimo(id, rutaNombre,
                    "Trafiqueishon", "SERVICIO", seleccionados);
            List<PuntoImportante> puntosVuelta = new ArrayList<>(trayecto.puntos());
            java.util.Collections.reverse(puntosVuelta);
            Trayecto vuelta = calculador.calcular(id + "-vuelta",
                    rutaNombre + " · vuelta", "Trafiqueishon", "SERVICIO_VUELTA",
                    puntosVuelta);
            String color = colorMicro(rutasMicros.size());
            ServicioMicro servicio = new ServicioMicro(id, rutaNombre, trayecto, vuelta,
                    frecuencia.getValue(), unidades.getValue(), pasajerosSimulados.getValue(), color);
            rutasMicros.add(servicio);
            rutasMicrosVisibles.add(id);
            gestor.agregar(trayecto);
            actualizarFilasRutasMicro();
            actualizarVisualizacionRutas();
            guardarRutasMicros();
            lblEstado.setText(rutaNombre + ": " + seleccionados.size() + " paraderos, cada "
                    + frecuencia.getValue() + " min, sale de " + seleccionados.getFirst().nombre()
                    + " · " + trayecto.resumen());
        } catch (RuntimeException ex) {
            lblEstado.setText("No se pudo crear la ruta: " + ex.getMessage());
        }
    }

    private String colorMicro(int indice) {
        if (indice < COLORES_MICROS.size()) return COLORES_MICROS.get(indice);
        Color color = Color.hsb((indice * 137.508) % 360, 0.9, 1.0);
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
            HBox fila = new HBox(6, informacion, eliminar);
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
                    ServicioMicro servicio = new ServicioMicro(guardada.id(), guardada.nombre(),
                            ida, vuelta, guardada.frecuenciaMinutos(),
                            guardada.unidadesDisponibles(), guardada.pasajerosEsperando(),
                            guardada.color());
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
        boton.setOnAction(evento -> accion.run());
        return boton;
    }

    private HBox crearStatusBar() {
        HBox bar = new HBox(15);
        bar.setPadding(new Insets(6, 12, 6, 12));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("status-bar");
        bar.getChildren().addAll(lblRelojStatus,
                new Separator(javafx.geometry.Orientation.VERTICAL), lblCoords,
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
        listaEventos.getItems().setAll(eventos);
        mapa.setEventos(eventos);
        aplicarEventosAlGrafo();
        lblEstado.setText("Evento agregado: " + nombre + ". Se actualizaron los costos viales.");
    }

    private void eliminarEvento() {
        EventoTrafico evento = listaEventos.getSelectionModel().getSelectedItem();
        if (evento == null) {
            lblEstado.setText("Selecciona un evento para eliminar.");
            return;
        }
        eventos.remove(evento);
        listaEventos.getItems().setAll(eventos);
        mapa.setEventos(eventos);
        aplicarEventosAlGrafo();
        lblEstado.setText("Evento eliminado y costos recalculados.");
    }

    private void aplicarEventosAlGrafo() {
        if (grafo == null) return;
        for (Edge edge : grafo.getEdges()) {
            edge.cerrado = false;
            edge.factorTrafico = 1.0;
        }
        for (EventoTrafico evento : eventos) {
            for (Edge edge : grafo.getEdges()) {
                if (distanciaPuntoTramo(evento.lon(), evento.lat(), edge) > 250) continue;
                if (evento.tipo() == TipoEvento.VIA_CERRADA) {
                    edge.cerrado = true;
                } else {
                    edge.factorTrafico = Math.max(edge.factorTrafico, evento.tipo().factor());
                }
            }
        }
        recalcularServiciosPorEventos();
        if (simulador.estaEjecutando()) {
            simulador.detener();
            mapa.setVehiculos(List.of());
            lblEstado.setText("Eventos aplicados. Reinicia la simulación para recalcular rutas cerradas.");
        }
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
        actualizarFilasRutasMicro();
        actualizarVisualizacionRutas();
        guardarRutasMicros();
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
                        pasajerosSimulados.getValue(), ruta.color()));
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
