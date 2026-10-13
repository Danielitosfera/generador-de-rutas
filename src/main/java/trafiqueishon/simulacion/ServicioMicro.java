package trafiqueishon.simulacion;

import trafiqueishon.ruta.Trayecto;

import java.util.Objects;

public record ServicioMicro(
        String id,
        String nombre,
        Trayecto trayecto,
        Trayecto trayectoVuelta,
        int frecuenciaMinutos,
        int unidadesDisponibles,
        int pasajerosEsperando,
        String color) {

    public ServicioMicro {
        if (id == null || id.isBlank() || nombre == null || nombre.isBlank()
                || frecuenciaMinutos < 1 || frecuenciaMinutos > 180
                || unidadesDisponibles < 1 || unidadesDisponibles > 50
                || pasajerosEsperando < 0 || color == null || color.isBlank()) {
            throw new IllegalArgumentException("Configuración de ruta de micro inválida.");
        }
        Objects.requireNonNull(trayecto, "El trayecto de la micro es obligatorio.");
        if (trayecto.edges().isEmpty()) {
            throw new IllegalArgumentException("La ruta de micro debe tener tramos.");
        }
        Objects.requireNonNull(trayectoVuelta, "La ruta de retorno es obligatoria.");
        if (trayectoVuelta.edges().isEmpty()) {
            throw new IllegalArgumentException("La ruta de retorno debe tener tramos.");
        }
        if (pasajerosEsperando > unidadesDisponibles * 18 * 100) {
            throw new IllegalArgumentException("La demanda excede el límite operativo.");
        }
    }
}
