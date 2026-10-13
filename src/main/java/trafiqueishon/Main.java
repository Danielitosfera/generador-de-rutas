package trafiqueishon;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import trafiqueishon.ui.VentanaPrincipal;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        VentanaPrincipal ventana = new VentanaPrincipal();
        Scene scene = new Scene(ventana.getRoot(), 1400, 850);

        var css = getClass().getResource("/estilos/estilo.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("Trafiqueishon - Simulador de Buses");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> ventana.cerrar());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}