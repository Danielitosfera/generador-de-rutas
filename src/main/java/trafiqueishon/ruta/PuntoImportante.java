package trafiqueishon.ruta;

public record PuntoImportante(
        String id,
        String nombre,
        double lon,
        double lat,
        TipoPunto tipo) {

    public enum TipoPunto {
        ORIGEN, DESTINO, PARADA, REFERENCIA, PEaje, CONTROL, TERMINAL, POI
    }

    public PuntoImportante {
        if (id == null || id.isBlank() || nombre == null || nombre.isBlank()
                || !Double.isFinite(lon) || !Double.isFinite(lat)
                || lon < -180 || lon > 180 || lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Punto importante invalido.");
        }
        tipo = tipo == null ? TipoPunto.PARADA : tipo;
    }

    public PuntoImportante(String id, String nombre, double lon, double lat) {
        this(id, nombre, lon, lat, TipoPunto.PARADA);
    }
}