package trafiqueishon.simulacion;

import org.junit.jupiter.api.Test;
import trafiqueishon.geo.Edge;
import trafiqueishon.geo.Nodo;
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
    void rechazaVelocidadFueraDelRangoPermitido() {
        SimuladorBuses simulador = new SimuladorBuses();
        assertThrows(IllegalArgumentException.class,
                () -> simulador.setVelocidadSimulacion(21));
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
