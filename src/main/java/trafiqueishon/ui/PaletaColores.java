/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: dominio
 * Archivo: PaletaColores.java
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
package trafiqueishon.ui;

/**
 * Única fuente de los colores de recorrido. El mapa, la leyenda, el panel de
 * paradas, los exportadores y el simulador leen estos valores; ningún otro
 * archivo debe repetir los códigos hexadecimales.
 *
 * <p>Se expresan como texto {@code #RRGGBB} para que los módulos sin JavaFX
 * (rutas, simulación, datos) puedan usarlos sin depender de la interfaz.</p>
 */
public final class PaletaColores {

    /** Recorrido de partida (ida) de una micro individual: azul neón. */
    public static final String COLOR_RUTA_IDA = "#00A8FF";

    /** Recorrido de retorno (vuelta) de una micro individual: rojo neón. */
    public static final String COLOR_RUTA_VUELTA = "#FF1744";

    /** Unidad en movimiento sobre la ruta: verde. */
    public static final String COLOR_RUTA_ACTIVA = "#2E9E44";

    private PaletaColores() {}
}
