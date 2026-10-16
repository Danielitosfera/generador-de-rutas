<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: dominio
 * Archivo: GestorTrayectos.java
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
 * @author Piero Gonzales (PG)
 */
=======
>>>>>>> e8b7abb8ec556353e5a6371a8d51ea852ffcdd32
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