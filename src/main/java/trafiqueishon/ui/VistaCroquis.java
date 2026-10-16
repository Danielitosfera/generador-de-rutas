/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: interfaz
 * Archivo: VistaCroquis.java
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
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import trafiqueishon.ruta.PaletaColores;

/**
 * Marco del croquis: el mapa con un borde exterior de franja verde delgada y una
 * línea amarilla. Solo organiza la presentación; no calcula rutas ni guarda archivos.
 */
public class VistaCroquis extends BorderPane {

    private static final double GROSOR_FRANJA = 6;
    private static final double GROSOR_LINEA = 1.5;

    private final MapaRutasCanvas mapa;
    private final Border borde = crearBorde();

    public VistaCroquis(MapaRutasCanvas mapa) {
        this.mapa = mapa;
        setCenter(mapa);
        setModoCroquis(true);
    }

    /** Activa o desactiva el aspecto de croquis (mapa blanco con tema claro y borde). */
    public void setModoCroquis(boolean activo) {
        mapa.setModoCroquis(activo);
        setBorder(activo ? borde : null);
    }

    private static Border crearBorde() {
        BorderStroke franjaVerde = new BorderStroke(
                Color.web(PaletaColores.COLOR_RUTA_ACTIVA), BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(GROSOR_FRANJA));
        double centro = (GROSOR_FRANJA - GROSOR_LINEA) / 2;
        BorderStroke lineaAmarilla = new BorderStroke(
                Color.YELLOW, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                new BorderWidths(GROSOR_LINEA), new Insets(centro));
        return new Border(franjaVerde, lineaAmarilla);
    }
}
