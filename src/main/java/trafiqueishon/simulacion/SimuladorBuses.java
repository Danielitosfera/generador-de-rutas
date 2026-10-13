package trafiqueishon.simulacion;

import trafiqueishon.geo.Edge;
import trafiqueishon.geo.Nodo;
import trafiqueishon.ruta.Trayecto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SimuladorBuses {
    private final List<Vehiculo> vehiculos = new ArrayList<>();
    private final List<DespachoProgramado> despachos = new ArrayList<>();
    private double velocidadSimulacion = 1.0;
    private boolean ejecutando;
    private double segundosTranscurridos;
    private int horaInicioMinutos;
    private int ultimoMinutoFactorPunta = -1;

    public void iniciar(Trayecto trayecto, int cantidad) {
        Objects.requireNonNull(trayecto, "El trayecto es obligatorio.");
        if (cantidad < 1 || cantidad > 100) {
            throw new IllegalArgumentException("La cantidad de buses debe estar entre 1 y 100.");
        }
        if (trayecto.edges().isEmpty()) {
            throw new IllegalArgumentException("El trayecto no contiene tramos.");
        }
        vehiculos.clear();
        despachos.clear();
        for (int i = 0; i < cantidad; i++) {
            PosicionVehiculo.TipoVehiculo tipo = i % 3 == 0
                    ? PosicionVehiculo.TipoVehiculo.MINIBUS
                    : PosicionVehiculo.TipoVehiculo.BUS;
            vehiculos.add(new Vehiculo(trayecto.edges(), trayecto.edges(), tipo,
                    0, "", "Flota", "#00A8FF", 0));
        }
        ejecutando = true;
    }

    public void iniciar(Trayecto trayecto, int buses, int micros, int pasajeros) {
        Objects.requireNonNull(trayecto, "El trayecto es obligatorio.");
        if (buses < 0 || micros < 0 || buses + micros < 1 || buses + micros > 100) {
            throw new IllegalArgumentException("Selecciona entre 1 y 100 unidades.");
        }
        if (pasajeros < 0 || pasajeros > buses * 50 + micros * 18) {
            throw new IllegalArgumentException("Los pasajeros superan la capacidad de la flota.");
        }
        if (trayecto.edges().isEmpty()) {
            throw new IllegalArgumentException("El trayecto no contiene tramos.");
        }
        vehiculos.clear();
        despachos.clear();
        int pasajerosPendientes = pasajeros;
        int cantidad = buses + micros;
        for (int i = 0; i < cantidad; i++) {
            PosicionVehiculo.TipoVehiculo tipo = i < buses
                    ? PosicionVehiculo.TipoVehiculo.BUS : PosicionVehiculo.TipoVehiculo.MINIBUS;
            int asignados = Math.min(capacidad(tipo), pasajerosPendientes);
            pasajerosPendientes -= asignados;
            vehiculos.add(new Vehiculo(trayecto.edges(), trayecto.edges(), tipo, asignados,
                    "", "Flota", "#00A8FF", 0));
        }
        ejecutando = true;
    }

    public void iniciar(List<ServicioMicro> servicios, int hora, int minuto) {
        Objects.requireNonNull(servicios, "La lista de servicios es obligatoria.");
        if (servicios.isEmpty()) throw new IllegalArgumentException("No hay rutas de micros activas.");
        if (hora < 0 || hora > 23 || minuto < 0 || minuto > 59) {
            throw new IllegalArgumentException("La hora de inicio no es válida.");
        }
        vehiculos.clear();
        despachos.clear();
        segundosTranscurridos = 0;
        horaInicioMinutos = hora * 60 + minuto;
        ultimoMinutoFactorPunta = -1;
        for (ServicioMicro servicio : servicios) {
            despachos.add(new DespachoProgramado(servicio));
        }
        ejecutando = true;
        procesarDespachos();
    }

    public void detener() {
        ejecutando = false;
    }

    public void limpiar() {
        detener();
        vehiculos.clear();
        despachos.clear();
    }

    public void avanzar(double deltaSegundos) {
        if (!ejecutando || !Double.isFinite(deltaSegundos) || deltaSegundos <= 0) return;
        double delta = Math.min(deltaSegundos, 1.0) * velocidadSimulacion;
        segundosTranscurridos += delta;
        for (Vehiculo vehiculo : vehiculos) vehiculo.avanzar(delta);
        procesarDespachos();
    }

    public List<PosicionVehiculo> posiciones() {
        return vehiculos.stream().map(Vehiculo::posicion).toList();
    }

    public boolean estaEjecutando() {
        return ejecutando;
    }

    public int capacidadTotal() {
        return vehiculos.stream().mapToInt(v -> v.capacidad).sum();
    }

    public int pasajerosAsignados() {
        return vehiculos.stream().mapToInt(v -> v.pasajeros).sum();
    }

    public double segundosTranscurridos() {
        return segundosTranscurridos;
    }

    public int horaActualMinutos() {
        return Math.floorMod(horaInicioMinutos + (int) (segundosTranscurridos / 60),
                24 * 60);
    }

    public int minutosHastaProximoDespacho() {
        return despachos.stream()
                .filter(d -> d.unidadesDespachadas < d.servicio.unidadesDisponibles())
                .mapToInt(d -> (int) Math.ceil(
                Math.max(0, d.proximoDespacho - segundosTranscurridos) / 60)).min().orElse(-1);
    }

    private void procesarDespachos() {
        for (DespachoProgramado despacho : despachos) {
            ServicioMicro servicio = despacho.servicio;
            while (despacho.unidadesDespachadas < servicio.unidadesDisponibles()
                    && despacho.proximoDespacho <= segundosTranscurridos) {
                int abordados = Math.min(18, despacho.pasajerosPendientes);
                despacho.pasajerosPendientes -= abordados;
                boolean saleDelFinal = despacho.unidadesDespachadas % 2 == 1;
                vehiculos.add(new Vehiculo(
                        saleDelFinal ? servicio.trayectoVuelta().edges()
                                : servicio.trayecto().edges(),
                        saleDelFinal ? servicio.trayecto().edges()
                                : servicio.trayectoVuelta().edges(),
                        PosicionVehiculo.TipoVehiculo.MINIBUS, abordados, servicio.id(),
                        servicio.nombre(), servicio.color(),
                        servicio.frecuenciaMinutos() * 60.0));
                despacho.unidadesDespachadas++;
                if (despacho.unidadesDespachadas == 1
                        && servicio.unidadesDisponibles() > 1) {
                    continue;
                }
                despacho.proximoDespacho += servicio.frecuenciaMinutos() * 60.0;
            }
        }
        int horaActual = horaActualMinutos();
        if (horaActual == ultimoMinutoFactorPunta) return;
        double factorHoraPunta = PerfilHoraPunta.factor(horaActual);
        for (DespachoProgramado despacho : despachos) {
            for (var edge : despacho.servicio.trayecto().edges()) edge.factorHoraPunta = factorHoraPunta;
            for (var edge : despacho.servicio.trayectoVuelta().edges()) {
                edge.factorHoraPunta = factorHoraPunta;
            }
        }
        for (Vehiculo vehiculo : vehiculos) {
            for (var edge : vehiculo.ruta) edge.factorHoraPunta = factorHoraPunta;
            for (var edge : vehiculo.rutaAlternativa) edge.factorHoraPunta = factorHoraPunta;
        }
        ultimoMinutoFactorPunta = horaActual;
    }

    private static int capacidad(PosicionVehiculo.TipoVehiculo tipo) {
        return tipo == PosicionVehiculo.TipoVehiculo.BUS ? 50 : 18;
    }

    public double getVelocidadSimulacion() {
        return velocidadSimulacion;
    }

    public void setVelocidadSimulacion(double velocidadSimulacion) {
        if (!Double.isFinite(velocidadSimulacion)
                || velocidadSimulacion < 0.25 || velocidadSimulacion > 20) {
            throw new IllegalArgumentException("La velocidad debe estar entre 0.25x y 20x.");
        }
        this.velocidadSimulacion = velocidadSimulacion;
    }

    private static final class Vehiculo {
        private List<Edge> ruta;
        private final List<Edge> rutaAlternativa;
        private final PosicionVehiculo.TipoVehiculo tipo;
        private final int pasajeros;
        private final int capacidad;
        private final String idRuta;
        private final String nombreRuta;
        private final String color;
        private final double esperaTerminalSegundos;
        private int indiceEdge;
        private double progresoMetros;
        private double esperaRestante;

        private Vehiculo(List<Edge> ruta, List<Edge> rutaAlternativa,
                         PosicionVehiculo.TipoVehiculo tipo, int pasajeros,
                         String idRuta, String nombreRuta, String color,
                         double esperaTerminalSegundos) {
            this.ruta = List.copyOf(ruta);
            this.rutaAlternativa = List.copyOf(rutaAlternativa);
            this.tipo = tipo;
            this.pasajeros = pasajeros;
            this.capacidad = capacidad(tipo);
            this.idRuta = idRuta;
            this.nombreRuta = nombreRuta;
            this.color = color;
            this.esperaTerminalSegundos = esperaTerminalSegundos;
        }

        private void avanzar(double segundos) {
            double restante = segundos;
            if (esperaRestante > 0) {
                double espera = Math.min(esperaRestante, restante);
                esperaRestante -= espera;
                restante -= espera;
            }
            while (restante > 0 && indiceEdge < ruta.size()) {
                Edge edge = ruta.get(indiceEdge);
                if (edge.cerrado) break;
                double velocidad = Math.max(1.0, edge.velocidadLibreMps()
                        / Math.max(1.0, edge.factorTrafico * edge.factorHoraPunta));
                double disponible = Math.max(0, edge.longitudMetros() - progresoMetros);
                double recorrido = Math.min(disponible, restante * velocidad);
                progresoMetros += recorrido;
                restante -= recorrido / velocidad;
                if (progresoMetros >= edge.longitudMetros()) {
                    indiceEdge++;
                    progresoMetros = 0;
                    if (indiceEdge >= ruta.size()) {
                        ruta = rutaAlternativa;
                        indiceEdge = 0;
                        esperaRestante = esperaTerminalSegundos;
                        break;
                    }
                }
            }
        }

        private PosicionVehiculo posicion() {
            Edge edge = ruta.get(Math.min(indiceEdge, ruta.size() - 1));
            double fraccion = Math.min(1, progresoMetros / Math.max(0.01, edge.longitudMetros()));
            Nodo origen = edge.origen;
            Nodo destino = edge.destino;
            return new PosicionVehiculo(origen.x + (destino.x - origen.x) * fraccion,
                    origen.y + (destino.y - origen.y) * fraccion, tipo, pasajeros, capacidad,
                    idRuta, nombreRuta, color);
        }

    }

    private static final class DespachoProgramado {
        private final ServicioMicro servicio;
        private int pasajerosPendientes;
        private int unidadesDespachadas;
        private double proximoDespacho;

        private DespachoProgramado(ServicioMicro servicio) {
            this.servicio = servicio;
            pasajerosPendientes = servicio.pasajerosEsperando();
        }
    }
}
