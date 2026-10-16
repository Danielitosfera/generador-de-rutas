<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: datos
 * Archivo: ExportadorGeoJson.java
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import trafiqueishon.geo.Edge;
import trafiqueishon.geo.Nodo;
<<<<<<< HEAD
import trafiqueishon.ruta.PaletaColores;
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
import trafiqueishon.ruta.Trayecto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ExportadorGeoJson {

    private final ObjectMapper mapper = new ObjectMapper();

    public void exportar(Trayecto trayecto, Path destino) throws IOException {
<<<<<<< HEAD
        exportar(List.of(new RutaExportable(trayecto.nombre(), PaletaColores.COLOR_RUTA_IDA, trayecto)), destino);
=======
        exportar(List.of(new RutaExportable(trayecto.nombre(), "#00A8FF", trayecto)), destino);
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
    }

    public void exportar(List<RutaExportable> rutas, Path destino) throws IOException {
        if (rutas.isEmpty()) throw new IllegalArgumentException("Selecciona al menos una ruta.");
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "FeatureCollection");
        ArrayNode features = mapper.createArrayNode();
        for (RutaExportable ruta : rutas) {
            Trayecto trayecto = ruta.trayecto();
            ObjectNode props = mapper.createObjectNode();
            props.put("nombre", ruta.nombre());
            props.put("empresa", trayecto.empresa());
            props.put("sentido", trayecto.sentido());
            props.put("color", ruta.color());
            props.put("distancia_km", trayecto.distanciaMetros() / 1000.0);
            props.put("tiempo_min", trayecto.tiempoSegundos() / 60.0);

            ArrayNode coords = mapper.createArrayNode();
            for (Edge e : trayecto.edges()) {
                Nodo n = e.origen();
                coords.add(mapper.createArrayNode().add(n.lon).add(n.lat));
            }
            if (!trayecto.edges().isEmpty()) {
                Nodo ultimo = trayecto.edges().getLast().destino();
                coords.add(mapper.createArrayNode().add(ultimo.lon).add(ultimo.lat));
            }

            ObjectNode geometry = mapper.createObjectNode();
            geometry.put("type", "LineString");
            geometry.set("coordinates", coords);

            ObjectNode feature = mapper.createObjectNode();
            feature.put("type", "Feature");
            feature.set("properties", props);
            feature.set("geometry", geometry);
            features.add(feature);
        }
        root.set("features", features);

        Files.writeString(destino, mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(root));
    }
}