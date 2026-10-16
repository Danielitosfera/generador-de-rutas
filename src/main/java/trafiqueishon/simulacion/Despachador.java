/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: simulación
 * Archivo: Despachador.java
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

import java.util.Optional;

/**
 * Programa las salidas de un servicio de micro: cuándo sale cada unidad, desde
 * qué terminal y con cuántos pasajeros. No crea vehículos ni dibuja nada.
 *
 * <p>Reglas: las dos primeras unidades salen juntas (una de cada terminal si hay
 * más de una); después sale una unidad cada {@code frecuenciaMinutos}, alternando
 * terminal de partida y terminal final.</p>
 */
public final class Despachador {

    /** Una salida programada. */
    public record Salida(int numeroUnidad, boolean desdeTerminalFinal, int pasajeros) {}

    private ServicioMicro servicio;
    private final DemandaPasajeros demanda;
    private int unidadesDespachadas;
    private double proximoDespacho;

    public Despachador(ServicioMicro servicio) {
        this.servicio = java.util.Objects.requireNonNull(servicio);
        this.demanda = new DemandaPasajeros(servicio.pasajerosEsperando(),
                servicio.unidadesDisponibles());
    }

    /**
     * Entrega la siguiente salida vencida, o vacío si aún no toca o ya salieron
     * todas las unidades. Se llama en bucle hasta que devuelva vacío.
     *
     * @throws ArithmeticException si la demanda acumulada desborda un entero
     */
    public Optional<Salida> siguienteSalida(double segundosTranscurridos, int minutoDelDia) {
        if (terminado() || proximoDespacho > segundosTranscurridos) return Optional.empty();
        int abordados = demanda.abordarEnSalida(minutoDelDia);
        boolean saleDelFinal = unidadesDespachadas % 2 == 1;
        Salida salida = new Salida(unidadesDespachadas, saleDelFinal, abordados);
        unidadesDespachadas++;
        boolean primeraConMas = unidadesDespachadas == 1 && servicio.unidadesDisponibles() > 1;
        if (!primeraConMas) proximoDespacho += servicio.frecuenciaMinutos() * 60.0;
        return Optional.of(salida);
    }

    /** Marca el servicio como terminado (p. ej. tras un error aislado). */
    public void finalizar() {
        unidadesDespachadas = servicio.unidadesDisponibles();
    }

    public boolean terminado() {
        return unidadesDespachadas >= servicio.unidadesDisponibles();
    }

    public int unidadesDespachadas() { return unidadesDespachadas; }

    public double proximoDespacho() { return proximoDespacho; }

    public ServicioMicro servicio() { return servicio; }

    /** Reemplaza la configuración del servicio conservando la demanda acumulada. */
    public void actualizarServicio(ServicioMicro actualizado) {
        this.servicio = java.util.Objects.requireNonNull(actualizado);
    }

    public int minutosHastaProximoDespacho(double segundosTranscurridos) {
        return (int) Math.ceil(Math.max(0, proximoDespacho - segundosTranscurridos) / 60);
    }
}
