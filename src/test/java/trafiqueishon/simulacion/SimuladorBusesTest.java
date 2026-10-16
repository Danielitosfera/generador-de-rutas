<<<<<<< HEAD
/*
 * Trafiqueishon - Sistema de rutas y simulación de transporte
 * Módulo: simulación
 * Archivo: SimuladorBusesTest.java
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

import org.junit.jupiter.api.Test;
import trafiqueishon.geo.Edge;
import trafiqueishon.geo.GrafoVial;
import trafiqueishon.geo.Nodo;
import trafiqueishon.geo.Proyector;
import trafiqueishon.geo.Via;
import trafiqueishon.ruta.PuntoImportante;
import trafiqueishon.ruta.Trayecto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimuladorBusesTest {

    @Test
    void iniciaYAvanzaLosVehiculosSobreElTrayecto() {
        Trayecto trayecto = trayecto();
        SimuladorBuses simulador = new SimuladorBuses();

        simulador.iniciar(trayecto, 2);
        double posicionInicial = simulador.posiciones().getFirst().x();
        simulador.avanzar(0.2);

        assertTrue(simulador.estaEjecutando());
        assertTrue(simulador.posiciones().getFirst().x() > posicionInicial);
        assertEquals(2, simulador.posiciones().size());
    }

    @Test
    void aceptaElLimiteDeSeisMilUnidadesEnElModoDirecto() {
        SimuladorBuses simulador = new SimuladorBuses();

        simulador.iniciar(trayecto(), 6_000);

        assertEquals(6_000, simulador.posiciones().size());
        assertThrows(IllegalArgumentException.class,
                () -> simulador.iniciar(trayecto(), 6_001));
    }

    @Test
    void rechazaVelocidadFueraDelRangoPermitido() {
        SimuladorBuses simulador = new SimuladorBuses();
        assertThrows(IllegalArgumentException.class,
                () -> simulador.setVelocidadSimulacion(31));
        assertDoesNotThrow(() -> simulador.setVelocidadSimulacion(30));
    }

    @Test
    void asignaPasajerosYValidaLaCapacidadDeLaFlota() {
        SimuladorBuses simulador = new SimuladorBuses();

        simulador.iniciar(trayecto(), 1, 1, 60);

        assertEquals(60, simulador.pasajerosAsignados());
        assertEquals(68, simulador.capacidadTotal());
        assertEquals(60, simulador.posiciones().stream()
                .mapToInt(PosicionVehiculo::pasajeros).sum());
        assertThrows(IllegalArgumentException.class,
                () -> simulador.iniciar(trayecto(), 1, 1, 69));
    }

    @Test
    void despachaDesdeElInicioDeRutaYRespetaLaFrecuencia() {
        Trayecto trayecto = trayectoLargo();
        Trayecto vuelta = trayectoLargoVuelta();
        ServicioMicro servicio = new ServicioMicro("m1", "Micro Centro", trayecto, vuelta,
                1, 4, 72, "#39FF14");
        SimuladorBuses simulador = new SimuladorBuses();

        simulador.iniciar(List.of(servicio), 6, 59);

        assertEquals(2, simulador.posiciones().size());
        assertEquals(0, simulador.posiciones().getFirst().x());
        assertEquals(1, simulador.posiciones().get(1).x());
        assertEquals(6 * 60 + 59, simulador.horaActualMinutos());
        for (int i = 0; i < 59; i++) simulador.avanzar(1);
        assertEquals(2, simulador.posiciones().size());
        simulador.avanzar(1);

        assertEquals(3, simulador.posiciones().size());
        assertEquals(7 * 60, simulador.horaActualMinutos());
        assertEquals(2.2, PerfilHoraPunta.factor(simulador.horaActualMinutos()));
    }

    @Test
    void limitaElTotalDeUnidadesEntreServiciosYDespachaGradualmente() {
        Trayecto ida = trayectoLargo();
        Trayecto vuelta = trayectoLargoVuelta();
        ServicioMicro servicio = new ServicioMicro("max", "Línea máxima", ida, vuelta,
                1, SimuladorBuses.MAX_UNIDADES, 0, "#39FF14");
        SimuladorBuses simulador = new SimuladorBuses();

        simulador.iniciar(List.of(servicio), 10, 0);

        assertEquals(2, simulador.posiciones().size());
        assertThrows(IllegalArgumentException.class,
                () -> simulador.iniciar(List.of(servicio, servicio), 10, 0));
        assertEquals(2, simulador.posiciones().size());
    }

    @Test
    void microEsperaEnElTerminalAntesDeRegresar() {
        Trayecto ida = trayecto();
        Nodo fin = ida.edges().getFirst().destino;
        Nodo inicio = ida.edges().getFirst().origen;
        Edge edgeVuelta = new Edge(1, fin, inicio, 100, "residential",
                "Avenida", 30, false, 1, null);
        Trayecto vuelta = new Trayecto("vuelta", "Regreso", "Empresa",
                List.of(new PuntoImportante("p2", "Fin", -69.999, -15),
                        new PuntoImportante("p1", "Inicio", -70, -15)),
                List.of(edgeVuelta), 100, edgeVuelta.costoBase, "SERVICIO_VUELTA");
        ServicioMicro servicio = new ServicioMicro("espera", "Micro", ida, vuelta,
                1, 1, 18, "#39FF14");
        SimuladorBuses simulador = new SimuladorBuses();
        simulador.iniciar(List.of(servicio), 6, 0);

        avanzar(simulador, 13);
        assertEquals(1, simulador.posiciones().getFirst().x(), 0.001);
        avanzar(simulador, 59);
        assertEquals(1, simulador.posiciones().getFirst().x(), 0.001);
        avanzar(simulador, 2);

        assertTrue(simulador.posiciones().getFirst().x() < 1);
    }

    @Test
    void despachaTodasLasUnidadesAunqueLaDemandaDePasajerosSeAgote() {
        Trayecto ida = trayectoLargo();
        Trayecto vuelta = trayectoLargoVuelta();
        ServicioMicro servicio = new ServicioMicro("flota", "Línea", ida, vuelta,
                1, 4, 0, "#39FF14");
        SimuladorBuses simulador = new SimuladorBuses();
        simulador.iniciar(List.of(servicio), 10, 0);

        assertEquals(2, simulador.posiciones().size());
        avanzar(simulador, 60);
        assertEquals(3, simulador.posiciones().size());
        avanzar(simulador, 60);

        assertEquals(4, simulador.posiciones().size());
        assertTrue(simulador.posiciones().stream()
                .allMatch(posicion -> "flota".equals(posicion.idRuta())));
    }

    @Test
    void recalcúlaDesvioYContinuaCuandoSeCierraElTramoActual() {
        Via directa = via("directa", "directa",
                new double[]{-70.0, -15.0}, new double[]{-69.999, -15.0});
        Via alternativa = via("alternativa", "alternativa",
                new double[]{-70.0, -15.0}, new double[]{-69.9995, -15.0005},
                new double[]{-69.999, -15.0});
        List<Via> vias = List.of(directa, alternativa);
        Proyector proyector = new Proyector();
        proyector.preProyectar(vias);
        GrafoVial grafo = new GrafoVial();
        grafo.construir(vias, proyector);

        List<Edge> ida = grafo.getEdges().stream()
                .filter(edge -> "directa".equals(edge.name)
                        && edge.origen.lon < edge.destino.lon)
                .toList();
        List<Edge> vuelta = grafo.getEdges().stream()
                .filter(edge -> "directa".equals(edge.name)
                        && edge.origen.lon > edge.destino.lon)
                .toList();
        Trayecto trayectoIda = new Trayecto("ida", "Ida", "Empresa",
                List.of(new PuntoImportante("origen", "Inicio", -70, -15),
                        new PuntoImportante("destino", "Fin", -69.999, -15)),
                ida, ida.stream().mapToDouble(Edge::longitudMetros).sum(), 100, "SERVICIO");
        Trayecto trayectoVuelta = new Trayecto("vuelta", "Vuelta", "Empresa",
                List.of(new PuntoImportante("destino", "Fin", -69.999, -15),
                        new PuntoImportante("origen", "Inicio", -70, -15)),
                vuelta, vuelta.stream().mapToDouble(Edge::longitudMetros).sum(), 100,
                "SERVICIO_VUELTA");
        ServicioMicro servicio = new ServicioMicro("m1", "Micro", trayectoIda,
                trayectoVuelta, 10, 1, 18, "#39FF14");
        SimuladorBuses simulador = new SimuladorBuses();
        simulador.configurarGrafo(grafo);
        simulador.iniciar(List.of(servicio), 10, 0);
        double xInicial = simulador.posiciones().getFirst().x();
        grafo.getEdges().stream().filter(edge -> "directa".equals(edge.name))
                .forEach(edge -> edge.cerrado = true);

        simulador.avanzar(1);

        assertEquals(PosicionVehiculo.Estado.EN_RUTA, simulador.posiciones().getFirst().estado());
        assertNotEquals(xInicial, simulador.posiciones().getFirst().x());
        assertTrue(simulador.estaEjecutando());
    }

    @Test
    void unListenerFallidoNoImpideNotificarLosSiguientes() {
        GestorEventos gestor = new GestorEventos();
        boolean[] notificado = {false};
        gestor.agregarListener(eventos -> {
            throw new IllegalStateException("falla aislada");
        });
        gestor.agregarListener(eventos -> notificado[0] = true);

        gestor.publicar(List.of(new EventoTrafico("Cierre", TipoEvento.VIA_CERRADA,
                -70, -15)));

        assertTrue(notificado[0]);
    }

    @Test
    void ajustaLaDemandaPorFranjaHoraria() {
        assertEquals(2.5, PerfilHoraPunta.factorDemanda(7 * 60));
        assertEquals(1.8, PerfilHoraPunta.factorDemanda(12 * 60));
        assertEquals(2.8, PerfilHoraPunta.factorDemanda(18 * 60));
        assertEquals(0.2, PerfilHoraPunta.factorDemanda(23 * 60));
        assertEquals(1.0, PerfilHoraPunta.factorDemanda(10 * 60));
    }

    @Test
    void gasolineraDetieneElVehiculoPorTresMinutosSimulados() {
        SimuladorBuses simulador = new SimuladorBuses();
        simulador.iniciar(trayecto(), 1);
        simulador.actualizarEventos(List.of(new EventoTrafico("Gasolinera",
                TipoEvento.GASOLINERA, -70, -15)));

        simulador.avanzar(1);
        double detenidoEn = simulador.posiciones().getFirst().x();
        assertEquals(PosicionVehiculo.Estado.DETENIDO,
                simulador.posiciones().getFirst().estado());
        avanzar(simulador, 178);
        assertEquals(detenidoEn, simulador.posiciones().getFirst().x(), 0.000001);
        assertEquals(PosicionVehiculo.Estado.DETENIDO,
                simulador.posiciones().getFirst().estado());

        simulador.avanzar(1);
        assertEquals(PosicionVehiculo.Estado.EN_RUTA,
                simulador.posiciones().getFirst().estado());
    }

    @Test
    void haceUnaPausaDeQuinceSegundosEnLasParadasIntermedias() {
        Via via = via("linea", "Avenida",
                new double[]{-70.0, -15.0}, new double[]{-69.999, -15.0},
                new double[]{-69.998, -15.0});
        Proyector proyector = new Proyector();
        proyector.preProyectar(List.of(via));
        GrafoVial grafo = new GrafoVial();
        grafo.construir(List.of(via), proyector);
        List<Edge> ida = grafo.getEdges().stream()
                .filter(edge -> edge.origen.lon < edge.destino.lon)
                .toList();
        List<Edge> vuelta = grafo.getEdges().stream()
                .filter(edge -> edge.origen.lon > edge.destino.lon)
                .toList();
        List<PuntoImportante> puntosIda = List.of(
                new PuntoImportante("a", "Inicio", -70, -15),
                new PuntoImportante("b", "Parada", -69.999, -15),
                new PuntoImportante("c", "Final", -69.998, -15));
        List<PuntoImportante> puntosVuelta = List.of(
                puntosIda.get(2), puntosIda.get(1), puntosIda.get(0));
        Trayecto trayectoIda = new Trayecto("ida", "Ida", "Empresa", puntosIda,
                ida, ida.stream().mapToDouble(Edge::longitudMetros).sum(), 100, "SERVICIO");
        Trayecto trayectoVuelta = new Trayecto("vuelta", "Vuelta", "Empresa", puntosVuelta,
                vuelta, vuelta.stream().mapToDouble(Edge::longitudMetros).sum(), 100,
                "SERVICIO_VUELTA");
        ServicioMicro servicio = new ServicioMicro("paradas", "Micro", trayectoIda,
                trayectoVuelta, 10, 1, 0, "#39FF14");
        SimuladorBuses simulador = new SimuladorBuses();
        simulador.configurarGrafo(grafo);
        simulador.iniciar(List.of(servicio), 10, 0);

        for (int i = 0; i < 20
                && simulador.posiciones().getFirst().estado() != PosicionVehiculo.Estado.EN_PARADA;
             i++) {
            simulador.avanzar(1);
        }
        assertEquals(PosicionVehiculo.Estado.EN_PARADA,
                simulador.posiciones().getFirst().estado());
        double enParada = simulador.posiciones().getFirst().x();
        avanzar(simulador, 14);
        assertEquals(enParada, simulador.posiciones().getFirst().x(), 0.000001);
        assertEquals(PosicionVehiculo.Estado.EN_PARADA,
                simulador.posiciones().getFirst().estado());
        simulador.avanzar(1);
        assertEquals(PosicionVehiculo.Estado.EN_RUTA,
                simulador.posiciones().getFirst().estado());
    }

    private Via via(String id, String nombre, double[]... coordenadas) {
        Via via = new Via();
        via.id = id;
        via.name = nombre;
        via.highway = "residential";
        via.coords = java.util.Arrays.stream(coordenadas).toList();
        return via;
    }

    private void avanzar(SimuladorBuses simulador, int segundos) {
        for (int i = 0; i < segundos; i++) simulador.avanzar(1);
    }

    private Trayecto trayecto() {
        Nodo inicio = new Nodo(0, 0, 0, -70, -15);
        Nodo fin = new Nodo(1, 1, 0, -69.999, -15);
        Edge edge = new Edge(0, inicio, fin, 100, "residential",
                "Avenida", 30, false, 1, null);
        Trayecto trayecto = new Trayecto("t1", "Ruta", "Empresa",
                List.of(new PuntoImportante("p1", "Inicio", -70, -15),
                        new PuntoImportante("p2", "Fin", -69.999, -15)),
                List.of(edge), 100, edge.costoBase, "PARTIDA");
        return trayecto;
    }

    private Trayecto trayectoLargo() {
        Nodo inicio = new Nodo(0, 0, 0, -70, -15);
        Nodo fin = new Nodo(1, 1, 0, -69.9, -15);
        Edge edge = new Edge(0, inicio, fin, 10_000, "residential",
                "Avenida", 30, false, 1, null);
        return new Trayecto("largo", "Ruta larga", "Empresa",
                List.of(new PuntoImportante("inicio", "Paradero", -70, -15),
                        new PuntoImportante("fin", "Destino", -69.9, -15)),
                List.of(edge), 10_000, edge.costoBase, "SERVICIO");
    }

    private Trayecto trayectoLargoVuelta() {
        Nodo fin = new Nodo(0, 1, 0, -69.9, -15);
        Nodo inicio = new Nodo(1, 0, 0, -70, -15);
        Edge edge = new Edge(0, fin, inicio, 10_000, "residential",
                "Avenida", 30, false, 1, null);
        return new Trayecto("vuelta", "Ruta vuelta", "Empresa",
                List.of(new PuntoImportante("fin", "Destino", -69.9, -15),
                        new PuntoImportante("inicio", "Paradero", -70, -15)),
                List.of(edge), 10_000, edge.costoBase, "SERVICIO_VUELTA");
    }
}
