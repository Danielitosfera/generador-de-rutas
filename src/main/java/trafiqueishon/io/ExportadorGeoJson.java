package trafiqueishon.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import trafiqueishon.geo.Edge;
import trafiqueishon.geo.Nodo;
import trafiqueishon.ruta.Trayecto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ExportadorGeoJson {

    private final ObjectMapper mapper = new ObjectMapper();

    public void exportar(Trayecto trayecto, Path destino) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "FeatureCollection");

        ObjectNode props = mapper.createObjectNode();
        props.put("nombre", trayecto.nombre());
        props.put("empresa", trayecto.empresa());
        props.put("sentido", trayecto.sentido());
        props.put("distancia_km", trayecto.distanciaMetros() / 1000.0);
        props.put("tiempo_min", trayecto.tiempoSegundos() / 60.0);

        ArrayNode coords = mapper.createArrayNode();
        for (Edge e : trayecto.edges()) {
            Nodo n = e.origen();
            coords.add(mapper.createArrayNode().add(n.lon).add(n.lat));
        }
        if (!trayecto.edges().isEmpty()) {
            Nodo ultimo = trayecto.edges().get(trayecto.edges().size() - 1).destino();
            coords.add(mapper.createArrayNode().add(ultimo.lon).add(ultimo.lat));
        }

        ObjectNode geometry = mapper.createObjectNode();
        geometry.put("type", "LineString");
        geometry.set("coordinates", coords);

        ObjectNode feature = mapper.createObjectNode();
        feature.put("type", "Feature");
        feature.set("properties", props);
        feature.set("geometry", geometry);

        ArrayNode features = mapper.createArrayNode();
        features.add(feature);
        root.set("features", features);

        Files.writeString(destino, mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(root));
    }
}