<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: interfaz
 * Archivo: Main.java
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
package trafiqueishon;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import trafiqueishon.ui.VentanaPrincipal;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        VentanaPrincipal ventana = new VentanaPrincipal();
        Scene scene = new Scene(ventana.getRoot(), 1200, 760);

        var css = getClass().getResource("/estilos/estilo.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("Trafiqueishon - Simulador de Buses");
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setOnCloseRequest(e -> ventana.cerrar());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}