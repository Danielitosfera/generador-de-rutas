package trafiqueishon.ruta;

import java.util.ArrayList;
import java.util.List;

public final class GestorTrayectos {

    private final List<Trayecto> trayectos = new ArrayList<>();

    public void agregar(Trayecto t) {
        trayectos.removeIf(x -> x.id().equals(t.id()));
        trayectos.add(t);
    }

    public void eliminar(String id) {
        trayectos.removeIf(t -> t.id().equals(id));
    }

    public void limpiar() { trayectos.clear(); }

    public List<Trayecto> getTrayectos() { return List.copyOf(trayectos); }

    public Trayecto buscar(String id) {
        return trayectos.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
    }
}