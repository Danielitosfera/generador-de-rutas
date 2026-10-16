/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: datos
 * Archivo: LectorPuntosJson.java
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
package trafiqueishon.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import trafiqueishon.ruta.PuntoImportante;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class LectorPuntosJson {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<PuntoImportante> leer(Path archivo) throws IOException {
        try (InputStream in = Files.newInputStream(archivo)) {
            return parsear(mapper.readTree(in));
        }
    }

    public List<PuntoImportante> leerRecurso(String recurso) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(recurso)) {
            if (in == null) throw new IOException("No se encontro el recurso: " + recurso);
            return parsear(mapper.readTree(in));
        }
    }

    public void guardar(List<PuntoImportante> puntos, Path archivo) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode array = root.putArray("puntos");
        for (PuntoImportante punto : puntos) {
            ObjectNode nodo = array.addObject();
            nodo.put("id", punto.id());
            nodo.put("nombre", punto.nombre());
            nodo.put("x", punto.lon());
            nodo.put("y", punto.lat());
            nodo.put("tipo", punto.tipo().name());
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(archivo.toFile(), root);
    }

    private List<PuntoImportante> parsear(JsonNode root) throws IOException {
        JsonNode arr = root.path("puntos");
        if (!arr.isArray()) throw new IOException("El archivo no contiene 'puntos'.");
        List<PuntoImportante> lista = new ArrayList<>();
        for (JsonNode p : arr) {
            String id = p.path("id").asText(null);
            String nombre = p.path("nombre").asText(id);
            double lon = p.path("x").asDouble(Double.NaN);
            double lat = p.path("y").asDouble(Double.NaN);
            String tipoStr = p.path("tipo").asText("PARADA");
            PuntoImportante.TipoPunto tipo;
            try {
                tipo = PuntoImportante.TipoPunto.valueOf(tipoStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                tipo = PuntoImportante.TipoPunto.PARADA;
            }
            try {
                lista.add(new PuntoImportante(id, nombre, lon, lat, tipo));
            } catch (IllegalArgumentException e) {
                throw new IOException("Punto invalido: " + id + " - " + e.getMessage());
            }
        }
        return List.copyOf(lista);
    }
}