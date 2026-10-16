/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: interfaz
 * Archivo: PanelParadas.java
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

import javafx.geometry.Insets;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import trafiqueishon.ruta.PuntoImportante;
import trafiqueishon.ruta.Trayecto;
import trafiqueishon.simulacion.ServicioMicro;

import java.util.List;

/**
 * Panel desplegable con los paraderos de UNA micro: "Recorrido de partida" y
 * "Recorrido de retorno", cada uno en una lista colapsable con scroll. Está oculto
 * ({@code visible} y {@code managed} en falso) hasta que se llama a
 * {@link #mostrar(ServicioMicro)}.
 */
public class PanelParadas extends VBox {

    private static final double ANCHO = 260;
    private static final double ALTO_FILA = 24;
    private static final double ALTO_MAXIMO_LISTA = 180;

    private final TitledPane contenedor = new TitledPane();
    private final ListView<String> listaPartida = new ListView<>();
    private final ListView<String> listaRetorno = new ListView<>();

    public PanelParadas() {
        TitledPane partida = new TitledPane("Recorrido de Partida", listaPartida);
        TitledPane retorno = new TitledPane("Recorrido de Retorno", listaRetorno);
        partida.setExpanded(true);
        retorno.setExpanded(false);
        VBox listas = new VBox(4, partida, retorno);
        listas.setPadding(new Insets(4));
        contenedor.setContent(listas);
        contenedor.setExpanded(true);
        getChildren().add(contenedor);

        setPrefWidth(ANCHO);
        setMaxWidth(ANCHO);
        setMaxHeight(Region.USE_PREF_SIZE);
        ocultar();
    }

    /** Muestra los paraderos de la micro indicada. Llamar desde el hilo de JavaFX. */
    public void mostrar(ServicioMicro servicio) {
        contenedor.setText("Paraderos · " + servicio.nombre());
        llenar(listaPartida, servicio.trayecto());
        llenar(listaRetorno, servicio.trayectoVuelta());
        setVisible(true);
        setManaged(true);
    }

    /** Oculta el panel (0 o más de 1 micro seleccionada). */
    public void ocultar() {
        setVisible(false);
        setManaged(false);
    }

    private static void llenar(ListView<String> lista, Trayecto trayecto) {
        List<String> nombres = trayecto.puntos().stream().map(PuntoImportante::nombre).toList();
        lista.getItems().setAll(nombres);
        lista.setPrefHeight(Math.min(ALTO_MAXIMO_LISTA, nombres.size() * ALTO_FILA + 2));
        lista.setMaxHeight(ALTO_MAXIMO_LISTA);
    }
}
