/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: datos
 * Archivo: RepositorioServiciosMicro.java
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import trafiqueishon.ruta.PuntoImportante;
import trafiqueishon.simulacion.ServicioMicro;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class RepositorioServiciosMicro {
    private static final TypeReference<List<RutaGuardada>> TIPO_RUTAS = new TypeReference<>() {};
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private final Path archivo;

    public RepositorioServiciosMicro() {
        this(Path.of(System.getProperty("user.home"), ".trafiqueishon", "rutas-micros.json"));
    }

    public RepositorioServiciosMicro(Path archivo) {
        this.archivo = archivo.toAbsolutePath();
    }

    public List<RutaGuardada> cargar() throws IOException {
        if (!Files.exists(archivo)) return List.of();
        try (var entrada = Files.newInputStream(archivo)) {
            List<RutaGuardada> rutas = mapper.readValue(entrada, TIPO_RUTAS);
            if (rutas == null || rutas.stream().anyMatch(r -> r == null
                    || r.id() == null || r.nombre() == null
                    || r.paraderos() == null || r.paraderos().size() < 2)) {
                throw new IOException("El archivo de rutas guardadas contiene rutas inválidas.");
            }
            return List.copyOf(rutas);
        }
    }

    public void guardar(List<ServicioMicro> servicios) throws IOException {
        List<RutaGuardada> rutas = servicios.stream().map(servicio -> new RutaGuardada(
                servicio.id(), servicio.nombre(),
                servicio.trayecto().puntos(),
                servicio.frecuenciaMinutos(), servicio.unidadesDisponibles(),
                servicio.pasajerosEsperando(), servicio.color())).toList();
        Files.createDirectories(archivo.getParent());
        Path temporal = Files.createTempFile(archivo.getParent(), "rutas-micros-", ".tmp");
        try {
            try (var salida = Files.newOutputStream(temporal)) {
                mapper.writeValue(salida, rutas);
            }
            try {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    public Path ubicacionArchivo() {
        return archivo;
    }

    public record RutaGuardada(String id, String nombre, List<PuntoImportante> paraderos,
                               int frecuenciaMinutos, int unidadesDisponibles,
                               int pasajerosEsperando, String color) {
        public RutaGuardada {
            paraderos = paraderos == null ? List.of() : List.copyOf(paraderos);
        }
    }
}
