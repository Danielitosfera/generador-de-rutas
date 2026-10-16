/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: datos
 * Archivo: RepositorioServiciosMicroTest.java
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
import trafiqueishon.geo.Edge;
import trafiqueishon.geo.Nodo;
import trafiqueishon.ruta.PuntoImportante;
import trafiqueishon.ruta.Trayecto;
import trafiqueishon.simulacion.ServicioMicro;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepositorioServiciosMicroTest {
    @TempDir
    Path temporal;

    @Test
    void guardaYRecuperaLaConfiguracionDeLaRuta() throws Exception {
        Nodo a = new Nodo(0, 0, 0, -70, -15);
        Nodo b = new Nodo(1, 1, 0, -69.999, -15);
        Edge edge = new Edge(0, a, b, 100, "residential",
                "Avenida", 30, false, 1, null);
        List<PuntoImportante> puntos = List.of(
                new PuntoImportante("a", "Paradero A", -70, -15),
                new PuntoImportante("b", "Paradero B", -69.999, -15));
        Trayecto ida = new Trayecto("r1", "Micro Centro", "Empresa", puntos,
                List.of(edge), 100, edge.costoBase, "SERVICIO");
        Trayecto vuelta = new Trayecto("r1-vuelta", "Micro Centro vuelta", "Empresa",
                List.of(puntos.get(1), puntos.get(0)), List.of(edge), 100,
                edge.costoBase, "SERVICIO_VUELTA");
        ServicioMicro servicio = new ServicioMicro("r1", "Micro Centro", ida, vuelta,
                12, ServicioMicro.MAX_UNIDADES, 80, "#39FF14");
        RepositorioServiciosMicro repositorio = new RepositorioServiciosMicro(
                temporal.resolve("rutas.json"));

        repositorio.guardar(List.of(servicio));

        RepositorioServiciosMicro.RutaGuardada cargada = repositorio.cargar().getFirst();
        assertEquals(servicio.id(), cargada.id());
        assertEquals(servicio.nombre(), cargada.nombre());
        assertEquals(servicio.trayecto().puntos(), cargada.paraderos());
        assertEquals(12, cargada.frecuenciaMinutos());
        assertEquals(ServicioMicro.MAX_UNIDADES, cargada.unidadesDisponibles());
        assertEquals(80, cargada.pasajerosEsperando());
        assertEquals(servicio.color(), cargada.color());
    }
}
