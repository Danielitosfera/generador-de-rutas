<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: simulación
 * Archivo: GestorEventos.java
 *
 * Autores de la obra:
 *   Piero Gonzales     (PG) - Módulo dominio
 *   Beto Hanco         (BH) - Módulo rutas
 *   Eberth Mamani      (EM) - Módulo simulación
 *   Daniel Taza        (DT) - Módulo datos
 *   Luis Yujra         (LY) - Módulo interfaz
 *
 * Copyright (c) 2026 Piero Gonzales, Beto Hanco, Eberth Mamani,
 * Daniel Taza, Luis Yujra. Todos los derechos reservados.
 *
 * Obra en colaboración. Versión 1.0.
 *
 * @author Eberth Mamani (EM)
 */
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
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
