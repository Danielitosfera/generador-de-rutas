package trafiqueishon.io;

import trafiqueishon.ruta.Trayecto;

import java.util.Objects;

public record RutaExportable(String nombre, String color, Trayecto trayecto) {
    public RutaExportable {
        if (nombre == null || nombre.isBlank() || color == null || color.isBlank()) {
            throw new IllegalArgumentException("La ruta exportable debe tener nombre y color.");
        }
        Objects.requireNonNull(trayecto, "El trayecto exportable es obligatorio.");
    }
}
