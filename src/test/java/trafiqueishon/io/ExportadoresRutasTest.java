<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: datos
 * Archivo: ExportadoresRutasTest.java
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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import trafiqueishon.geo.Edge;
import trafiqueishon.geo.Nodo;
import trafiqueishon.ruta.PuntoImportante;
import trafiqueishon.ruta.Trayecto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportadoresRutasTest {
    @TempDir
    Path temporal;

    @Test
    void exportaVariasRutasComoFeaturesGpxYCsv() throws Exception {
        RutaExportable ida = ruta("Micro Azul · Ida", -70.0, "#00A8FF");
        RutaExportable vuelta = ruta("Micro Azul · Vuelta", -69.999, "#FF1744");

        Path geoJson = temporal.resolve("rutas.geojson");
        new ExportadorGeoJson().exportar(List.of(ida, vuelta), geoJson);
        assertEquals(2, new ObjectMapper().readTree(geoJson.toFile())
                .get("features").size());

        Path gpx = temporal.resolve("rutas.gpx");
        new ExportadorGpx().exportar(List.of(ida, vuelta), gpx);
        assertEquals(2, Files.readString(gpx).split("<trk>", -1).length - 1);

        Path csv = temporal.resolve("rutas.csv");
        new ExportadorCsv().exportar(List.of(ida, vuelta), csv);
        String contenidoCsv = Files.readString(csv);
        assertTrue(contenidoCsv.contains("Micro Azul · Ida"));
        assertTrue(contenidoCsv.contains("Micro Azul · Vuelta"));
        assertEquals(9, contenidoCsv.lines().toList().getLast().split(",", -1).length);
    }

    @Test
    void pdfIncluyeUnaLeyendaYSePuedeAbrir() throws Exception {
        Path pdf = temporal.resolve("rutas.pdf");
        List<RutaExportable> rutas = java.util.stream.IntStream.range(0, 9)
                .mapToObj(i -> ruta("Micro " + i + " · Ida", -70.0 + i * 0.001,
                        i % 2 == 0 ? "#00A8FF" : "#FF1744"))
                .toList();
        new ExportadorPdf().exportar(rutas, pdf);

        try (PDDocument documento = PDDocument.load(pdf.toFile())) {
            assertEquals(2, documento.getNumberOfPages());
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("Micro 0"));
            assertTrue(texto.contains("Inicio"));
            assertTrue(texto.contains("Final"));
            assertTrue(Files.size(pdf) > 0);
        }
    }

    private RutaExportable ruta(String nombre, double lon, String color) {
        Nodo a = new Nodo(0, 0, 0, lon, -15);
        Nodo b = new Nodo(1, 1, 0, lon + 0.001, -15);
        Edge edge = new Edge(0, a, b, 100, "residential",
                "Avenida", 30, false, 1, null);
        Trayecto trayecto = new Trayecto(nombre, nombre, "Empresa",
                List.of(new PuntoImportante("a", "Inicio", lon, -15),
                        new PuntoImportante("b", "Final", lon + 0.001, -15)),
                List.of(edge), 100, edge.costoBase, "SERVICIO");
        return new RutaExportable(nombre, color, trayecto);
    }
}
