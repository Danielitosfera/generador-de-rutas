package trafiqueishon.simulacion;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GestorEventos {
    private static final Logger LOG = Logger.getLogger(GestorEventos.class.getName());
    private final List<EventoListener> listeners = new CopyOnWriteArrayList<>();

    public void agregarListener(EventoListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public void quitarListener(EventoListener listener) {
        listeners.remove(listener);
    }

    public void publicar(List<EventoTrafico> eventos) {
        List<EventoTrafico> copia = List.copyOf(eventos);
        for (EventoListener listener : listeners) {
            try {
                listener.eventosActualizados(copia);
            } catch (RuntimeException ex) {
                LOG.log(Level.SEVERE, "Un listener no pudo procesar el cambio de eventos", ex);
            }
        }
    }
}
