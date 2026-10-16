/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: datos
 * Archivo: LectorPuntosJsonTest.java
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import trafiqueishon.ruta.PuntoImportante;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LectorPuntosJsonTest {
    @TempDir
    Path directorioTemporal;

    @Test
    void guardaYRecuperaUbicacionesConCoordenadasYTipo() throws Exception {
        LectorPuntosJson lector = new LectorPuntosJson();
        List<PuntoImportante> originales = List.of(new PuntoImportante(
                "terminal-1", "Terminal", -70.12345, -15.54321,
                PuntoImportante.TipoPunto.TERMINAL));
        Path archivo = directorioTemporal.resolve("puntos.json");

        lector.guardar(originales, archivo);

        assertEquals(originales, lector.leer(archivo));
    }
}
