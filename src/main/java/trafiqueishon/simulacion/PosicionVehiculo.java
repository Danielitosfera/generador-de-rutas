package trafiqueishon.simulacion;

public record PosicionVehiculo(double x, double y, TipoVehiculo tipo,
                               int pasajeros, int capacidad, String idRuta, String nombreRuta,
                               String color) {
    public PosicionVehiculo(double x, double y, TipoVehiculo tipo) {
        this(x, y, tipo, 0, tipo == TipoVehiculo.BUS ? 50 : 18, "", "", "#00A8FF");
    }

    public PosicionVehiculo(double x, double y, TipoVehiculo tipo, int pasajeros, int capacidad) {
        this(x, y, tipo, pasajeros, capacidad, "", "", "#00A8FF");
    }

    public enum TipoVehiculo {
        BUS, MINIBUS
    }
}
