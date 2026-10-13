package trafiqueishon.simulacion;

import java.util.List;

@FunctionalInterface
public interface EventoListener {
    void eventosActualizados(List<EventoTrafico> eventos);
}
