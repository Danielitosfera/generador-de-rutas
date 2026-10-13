package trafiqueishon.simulacion;

public enum TipoEvento {
    TRANCADERA("Trancadera", 2.5),
    VIA_CERRADA("Vía cerrada", 1.0),
    VENDEDORES_AMBULANTES("Vendedores ambulantes", 1.5);

    private final String etiqueta;
    private final double factor;

    TipoEvento(String etiqueta, double factor) {
        this.etiqueta = etiqueta;
        this.factor = factor;
    }

    public double factor() {
        return factor;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
