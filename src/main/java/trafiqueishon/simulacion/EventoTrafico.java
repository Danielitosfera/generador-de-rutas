package trafiqueishon.simulacion;

public record EventoTrafico(String nombre, TipoEvento tipo, double lon, double lat) {
    public EventoTrafico {
        if (nombre == null || nombre.isBlank() || tipo == null
                || !Double.isFinite(lon) || !Double.isFinite(lat)
                || lon < -180 || lon > 180 || lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Evento de tráfico inválido.");
        }
    }
}
