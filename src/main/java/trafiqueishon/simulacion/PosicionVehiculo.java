package trafiqueishon.simulacion;

public record PosicionVehiculo(double x, double y, TipoVehiculo tipo,
                               int pasajeros, int capacidad, String idRuta, String nombreRuta,
                               String color, Estado estado) {
    public PosicionVehiculo(double x, double y, TipoVehiculo tipo, int pasajeros,
                            int capacidad, String idRuta, String nombreRuta, String color) {
        this(x, y, tipo, pasajeros, capacidad, idRuta, nombreRuta, color, Estado.EN_RUTA);
    }

    public PosicionVehiculo(double x, double y, TipoVehiculo tipo) {
        this(x, y, tipo, 0, tipo == TipoVehiculo.BUS ? 50 : 18, "", "", "#00A8FF",
                Estado.EN_RUTA);
    }

    public PosicionVehiculo(double x, double y, TipoVehiculo tipo, int pasajeros, int capacidad) {
        this(x, y, tipo, pasajeros, capacidad, "", "", "#00A8FF", Estado.EN_RUTA);
    }

    public enum Estado { EN_RUTA, EN_PARADA, DETENIDO, AVERIADO, SIN_RUTA }

    public enum TipoVehiculo {
        BUS, MINIBUS
    }
}
