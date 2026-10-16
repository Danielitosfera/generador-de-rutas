/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: simulación
 * Archivo: DemandaPasajeros.java
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
package trafiqueishon.simulacion;

/**
 * Genera la demanda de pasajeros de un servicio de micro y decide cuántos
 * abordan en cada salida. Aplica el perfil horario de {@link PerfilHoraPunta}.
 */
public final class DemandaPasajeros {

    /** Máximo de pasajeros que abordan por salida. */
    public static final int ABORDAJE_MAXIMO = 18;

    private final int demandaPorSalida;
    private int pendientes;

    /**
     * @param pasajerosEsperando demanda base total del servicio
     * @param unidades           unidades del servicio entre las que se reparte
     */
    public DemandaPasajeros(int pasajerosEsperando, int unidades) {
        if (pasajerosEsperando < 0 || unidades < 1) {
            throw new IllegalArgumentException("Demanda o cantidad de unidades inválida.");
        }
        this.demandaPorSalida = (int) Math.ceil((double) pasajerosEsperando / unidades);
    }

    /**
     * Suma la demanda nueva de la franja horaria y devuelve cuántos pasajeros
     * abordan en esta salida.
     *
     * @throws ArithmeticException si los pendientes desbordan un entero
     */
    public int abordarEnSalida(int minutoDelDia) {
        int demandaNueva = (int) Math.round(demandaPorSalida
                * PerfilHoraPunta.factorDemanda(minutoDelDia));
        pendientes = Math.addExact(pendientes, demandaNueva);
        int abordados = Math.min(ABORDAJE_MAXIMO, pendientes);
        pendientes -= abordados;
        return abordados;
    }

    public int demandaPorSalida() { return demandaPorSalida; }

    public int pendientes() { return pendientes; }
}
