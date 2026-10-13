package trafiqueishon.simulacion;

public enum TipoEvento {
    TRANCADERA("Trancadera", 2.5),
    VIA_CERRADA("Vía cerrada", 1.0),
    VENDEDORES_AMBULANTES("Vendedores ambulantes", 1.5),
    LLUVIA("Lluvia", 1.67),
    ACCIDENTE("Accidente", 3.0),
    GASOLINERA("Gasolinera", 1.0),
    SEMAFORO_ROTO("Semáforo roto", 2.5),
    MANIFESTACION("Venteo / manifestación", 1.0);

    private final String etiqueta;
    private final double factor;

    TipoEvento(String etiqueta, double factor) {
        this.etiqueta = etiqueta;
        this.factor = factor;
    }

    public double factor() {
        return factor;
    }

    public boolean bloqueaVia() {
        return this == VIA_CERRADA || this == ACCIDENTE || this == MANIFESTACION;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
