package trafiqueishon.simulacion;

import trafiqueishon.geo.GrafoVial;
import trafiqueishon.geo.Pathfinder;
import trafiqueishon.geo.RutaResult;
import trafiqueishon.geo.Edge;
import trafiqueishon.geo.Nodo;
import trafiqueishon.ruta.Trayecto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SimuladorBuses {
    public static final int MAX_UNIDADES = ServicioMicro.MAX_UNIDADES;
    private static final Logger LOG = Logger.getLogger(SimuladorBuses.class.getName());
    private static final int MAX_DESVIOS_CACHE = 2_048;
    private final List<Vehiculo> vehiculos = new ArrayList<>();
    private final List<DespachoProgramado> despachos = new ArrayList<>();
    private GrafoVial grafo;
    private Pathfinder pathfinder;
    private List<EventoTrafico> eventos = List.of();
    private List<EventoTrafico> gasolineras = List.of();
    private final Map<RutaDesvioKey, List<Edge>> desviosCache = new HashMap<>();
    private long versionRed;
    private double velocidadSimulacion = 1.0;
    private boolean ejecutando;
    private double segundosTranscurridos;
    private int horaInicioMinutos;
    private int ultimoMinutoFactorPunta = -1;
    private int pasajerosTransportados;

    public void iniciar(Trayecto trayecto, int cantidad) {
        Objects.requireNonNull(trayecto, "El trayecto es obligatorio.");
        if (cantidad < 1 || cantidad > MAX_UNIDADES) {
            throw new IllegalArgumentException("La cantidad de unidades debe estar entre 1 y "
                    + MAX_UNIDADES + ".");
        }
        if (trayecto.edges().isEmpty()) {
            throw new IllegalArgumentException("El trayecto no contiene tramos.");
        }
        vehiculos.clear();
        despachos.clear();
        pasajerosTransportados = 0;
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
        long totalUnidades = (long) buses + micros;
        long capacidadFlota = (long) buses * 50 + (long) micros * 18;
        if (buses < 0 || micros < 0 || totalUnidades < 1
                || totalUnidades > MAX_UNIDADES) {
            throw new IllegalArgumentException("Selecciona hasta "
                    + MAX_UNIDADES + " unidades.");
        }
        if (pasajeros < 0 || pasajeros > capacidadFlota) {
            throw new IllegalArgumentException("Los pasajeros superan la capacidad de la flota.");
        }
        if (trayecto.edges().isEmpty()) {
            throw new IllegalArgumentException("El trayecto no contiene tramos.");
        }
        vehiculos.clear();
        despachos.clear();
        pasajerosTransportados = pasajeros;
        int pasajerosPendientes = pasajeros;
        int cantidad = (int) totalUnidades;
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
        long totalUnidades = servicios.stream().mapToLong(ServicioMicro::unidadesDisponibles).sum();
        if (totalUnidades > MAX_UNIDADES) {
            throw new IllegalArgumentException("La flota total no puede superar "
                    + MAX_UNIDADES + " unidades.");
        }
        if (hora < 0 || hora > 23 || minuto < 0 || minuto > 59) {
            throw new IllegalArgumentException("La hora de inicio no es válida.");
        }
        vehiculos.clear();
        despachos.clear();
        segundosTranscurridos = 0;
        pasajerosTransportados = 0;
        horaInicioMinutos = hora * 60 + minuto;
        ultimoMinutoFactorPunta = -1;
        for (ServicioMicro servicio : servicios) {
            DespachoProgramado despacho = new DespachoProgramado(servicio);
            despacho.paradasIda = nodosParadas(servicio.trayecto());
            despacho.paradasVuelta = nodosParadas(servicio.trayectoVuelta());
            despachos.add(despacho);
        }
        ejecutando = true;
        procesarDespachos();
    }

    public void detener() {
        ejecutando = false;
    }

    public void pausar() {
        ejecutando = false;
    }

    public void reanudar() {
        if (!vehiculos.isEmpty() || !despachos.isEmpty()) ejecutando = true;
    }

    public void limpiar() {
        detener();
        vehiculos.clear();
        despachos.clear();
        pasajerosTransportados = 0;
    }

    public void configurarGrafo(GrafoVial grafo) {
        this.grafo = Objects.requireNonNull(grafo);
        this.pathfinder = new Pathfinder(grafo);
        this.pathfinder.setModo(Pathfinder.Modo.A_ESTRELLA);
    }

    public void actualizarServicios(List<ServicioMicro> servicios) {
        Map<String, ServicioMicro> porId = servicios.stream()
                .collect(java.util.stream.Collectors.toMap(ServicioMicro::id, servicio -> servicio,
                        (primero, ultimo) -> ultimo));
        for (DespachoProgramado despacho : despachos) {
            ServicioMicro actualizado = porId.get(despacho.servicio.id());
            if (actualizado != null) {
                despacho.servicio = actualizado;
                despacho.paradasIda = nodosParadas(actualizado.trayecto());
                despacho.paradasVuelta = nodosParadas(actualizado.trayectoVuelta());
            }
        }
        for (Vehiculo vehiculo : vehiculos) {
            ServicioMicro actualizado = porId.get(vehiculo.idRuta);
            if (actualizado == null) continue;
            vehiculo.rutaAlternativa = vehiculo.enIda
                    ? actualizado.trayectoVuelta().edges() : actualizado.trayecto().edges();
            vehiculo.paradasIda = nodosParadas(actualizado.trayecto());
            vehiculo.paradasVuelta = nodosParadas(actualizado.trayectoVuelta());
            vehiculo.paradas = vehiculo.enIda
                    ? vehiculo.paradasIda : vehiculo.paradasVuelta;
        }
        invalidarRutas();
    }

    public void actualizarEventos(List<EventoTrafico> eventos) {
        this.eventos = List.copyOf(eventos);
        this.gasolineras = this.eventos.stream()
                .filter(evento -> evento.tipo() == TipoEvento.GASOLINERA)
                .toList();
        invalidarRutas();
    }

    private void invalidarRutas() {
        versionRed++;
        desviosCache.clear();
    }

    private List<Nodo> nodosParadas(Trayecto trayecto) {
        if (grafo == null) return List.of();
        return trayecto.puntos().stream()
                .map(punto -> grafo.nodoMasCercanoLonLat(punto.lon(), punto.lat()))
                .filter(Objects::nonNull)
                .toList();
    }

    public void avanzar(double deltaSegundos) {
        if (!ejecutando || !Double.isFinite(deltaSegundos) || deltaSegundos <= 0) return;
        double delta = Math.min(deltaSegundos, 1.0) * velocidadSimulacion;
        segundosTranscurridos += delta;
        for (Vehiculo vehiculo : vehiculos) {
            try {
                vehiculo.avanzar(delta, pathfinder, gasolineras, versionRed, desviosCache);
            } catch (RuntimeException ex) {
                vehiculo.estado = PosicionVehiculo.Estado.AVERIADO;
                LOG.log(Level.SEVERE, "Se aisló un error en el vehículo " + vehiculo.idRuta, ex);
            }
        }
        procesarDespachos();
    }

    public List<PosicionVehiculo> posiciones() {
        return vehiculos.stream().map(Vehiculo::posicion).toList();
    }

    public boolean estaEjecutando() {
        return ejecutando;
    }

    public boolean haySimulacion() {
        return !vehiculos.isEmpty() || !despachos.isEmpty();
    }

    public int capacidadTotal() {
        return vehiculos.stream().mapToInt(v -> v.capacidad).sum();
    }

    public int pasajerosAsignados() {
        return vehiculos.stream().mapToInt(v -> v.pasajeros).sum();
    }

    public int pasajerosTransportados() {
        return pasajerosTransportados;
    }

    public double ocupacionPromedio() {
        return vehiculos.isEmpty() ? 0 : vehiculos.stream()
                .mapToDouble(v -> (double) v.pasajeros / v.capacidad).average().orElse(0);
    }

    public int vehiculosConIncidencia() {
        return (int) vehiculos.stream().filter(v -> v.estado == PosicionVehiculo.Estado.SIN_RUTA
                || v.estado == PosicionVehiculo.Estado.AVERIADO).count();
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
            try {
                while (despacho.unidadesDespachadas < servicio.unidadesDisponibles()
                        && despacho.proximoDespacho <= segundosTranscurridos) {
                    int demandaNueva = (int) Math.round(despacho.demandaPorSalida
                            * PerfilHoraPunta.factorDemanda(horaActualMinutos()));
                    despacho.pasajerosPendientes = Math.addExact(
                            despacho.pasajerosPendientes, demandaNueva);
                    int abordados = Math.min(18, despacho.pasajerosPendientes);
                    despacho.pasajerosPendientes -= abordados;
                    boolean saleDelFinal = despacho.unidadesDespachadas % 2 == 1;
                    Vehiculo vehiculo = new Vehiculo(
                            saleDelFinal ? servicio.trayectoVuelta().edges()
                                    : servicio.trayecto().edges(),
                            saleDelFinal ? servicio.trayecto().edges()
                                    : servicio.trayectoVuelta().edges(),
                            PosicionVehiculo.TipoVehiculo.MINIBUS, abordados, servicio.id(),
                            servicio.nombre(), servicio.color(),
                            servicio.frecuenciaMinutos() * 60.0,
                            despacho.paradasIda, despacho.paradasVuelta);
                    vehiculo.enIda = !saleDelFinal;
                    vehiculo.paradas = vehiculo.enIda
                            ? vehiculo.paradasIda : vehiculo.paradasVuelta;
                    vehiculos.add(vehiculo);
                    pasajerosTransportados += abordados;
                    despacho.unidadesDespachadas++;
                    if (despacho.unidadesDespachadas == 1
                            && servicio.unidadesDisponibles() > 1) {
                        continue;
                    }
                    despacho.proximoDespacho += servicio.frecuenciaMinutos() * 60.0;
                }
            } catch (RuntimeException ex) {
                despacho.unidadesDespachadas = servicio.unidadesDisponibles();
                LOG.log(Level.SEVERE, "Se aisló un error en el despacho de " + servicio.nombre(),
                        ex);
            }
        }
        int horaActual = horaActualMinutos();
        if (horaActual == ultimoMinutoFactorPunta) return;
        double factorHoraPunta = PerfilHoraPunta.factor(horaActual);
        if (grafo != null) {
            PerfilHoraPunta.aplicar(grafo, horaActual);
        } else {
            for (DespachoProgramado despacho : despachos) {
                for (var edge : despacho.servicio.trayecto().edges()) {
                    edge.factorHoraPunta = factorHoraPunta;
                }
                for (var edge : despacho.servicio.trayectoVuelta().edges()) {
                    edge.factorHoraPunta = factorHoraPunta;
                }
            }
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
                || velocidadSimulacion < 0.5 || velocidadSimulacion > 30) {
            throw new IllegalArgumentException("La velocidad debe estar entre 0.5x y 30x.");
        }
        this.velocidadSimulacion = velocidadSimulacion;
    }

    private static final class Vehiculo {
        private List<Edge> ruta;
        private List<Edge> rutaAlternativa;
        private List<Nodo> paradasIda = List.of();
        private List<Nodo> paradasVuelta = List.of();
        private List<Nodo> paradas = List.of();
        private final PosicionVehiculo.TipoVehiculo tipo;
        private final int pasajeros;
        private final int capacidad;
        private final String idRuta;
        private final String nombreRuta;
        private final String color;
        private final double esperaTerminalSegundos;
        private int indiceEdge;
        private int siguienteParada = 1;
        private double progresoMetros;
        private double esperaRestante;
        private boolean enIda = true;
        private PosicionVehiculo.Estado estado = PosicionVehiculo.Estado.EN_RUTA;
        private Set<String> gasolinerasVisitadas;
        private long versionRuta = -1;

        private Vehiculo(List<Edge> ruta, List<Edge> rutaAlternativa,
                         PosicionVehiculo.TipoVehiculo tipo, int pasajeros,
                         String idRuta, String nombreRuta, String color,
                         double esperaTerminalSegundos) {
            this(ruta, rutaAlternativa, tipo, pasajeros, idRuta, nombreRuta, color,
                    esperaTerminalSegundos, List.of(), List.of());
        }

        private Vehiculo(List<Edge> ruta, List<Edge> rutaAlternativa,
                         PosicionVehiculo.TipoVehiculo tipo, int pasajeros,
                         String idRuta, String nombreRuta, String color,
                         double esperaTerminalSegundos, List<Nodo> paradasIda,
                         List<Nodo> paradasVuelta) {
            this.ruta = List.copyOf(ruta);
            this.rutaAlternativa = List.copyOf(rutaAlternativa);
            this.paradasIda = List.copyOf(paradasIda);
            this.paradasVuelta = List.copyOf(paradasVuelta);
            this.paradas = this.paradasIda;
            this.tipo = tipo;
            this.pasajeros = pasajeros;
            this.capacidad = capacidad(tipo);
            this.idRuta = idRuta;
            this.nombreRuta = nombreRuta;
            this.color = color;
            this.esperaTerminalSegundos = esperaTerminalSegundos;
        }

        private void avanzar(double segundos, Pathfinder pathfinder,
                             List<EventoTrafico> eventos, long versionRed,
                             Map<RutaDesvioKey, List<Edge>> desviosCache) {
            if (versionRuta != versionRed) {
                recalcularSiCerrada(pathfinder, versionRed, desviosCache);
            }
            if (estado == PosicionVehiculo.Estado.SIN_RUTA
                    || estado == PosicionVehiculo.Estado.AVERIADO) return;
            detectarGasolinera(eventos);
            double restante = segundos;
            if (esperaRestante > 0) {
                double espera = Math.min(esperaRestante, restante);
                esperaRestante -= espera;
                restante -= espera;
            }
            if (esperaRestante <= 0 && estado == PosicionVehiculo.Estado.EN_PARADA) {
                estado = PosicionVehiculo.Estado.EN_RUTA;
            }
            if (esperaRestante <= 0 && estado == PosicionVehiculo.Estado.DETENIDO) {
                estado = PosicionVehiculo.Estado.EN_RUTA;
            }
            while (restante > 0 && indiceEdge < ruta.size()) {
                Edge edge = ruta.get(indiceEdge);
                if (edge.cerrado) {
                    estado = PosicionVehiculo.Estado.SIN_RUTA;
                    recalcularSiCerrada(pathfinder, versionRed, desviosCache);
                    break;
                }
                double velocidad = Math.max(1.0, edge.velocidadLibreMps()
                        / Math.max(1.0, edge.factorTrafico * edge.factorHoraPunta));
                double disponible = Math.max(0, edge.longitudMetros() - progresoMetros);
                double recorrido = Math.min(disponible, restante * velocidad);
                progresoMetros += recorrido;
                restante -= recorrido / velocidad;
                if (progresoMetros >= edge.longitudMetros()) {
                    boolean paradaAlcanzada = siguienteParada < paradas.size()
                            && edge.destino == paradas.get(siguienteParada);
                    if (paradaAlcanzada) {
                        siguienteParada++;
                        estado = PosicionVehiculo.Estado.EN_PARADA;
                        esperaRestante = Math.max(esperaRestante, 15);
                    }
                    indiceEdge++;
                    progresoMetros = 0;
                    if (indiceEdge >= ruta.size()) {
                        ruta = rutaAlternativa;
                        enIda = !enIda;
                        paradas = enIda ? paradasIda : paradasVuelta;
                        siguienteParada = 1;
                        indiceEdge = 0;
                        esperaRestante = esperaTerminalSegundos;
                        estado = PosicionVehiculo.Estado.EN_RUTA;
                        break;
                    }
                    if (paradaAlcanzada) break;
                }
            }
        }

        private void detectarGasolinera(List<EventoTrafico> eventos) {
            if (indiceEdge >= ruta.size()) return;
            Edge edge = ruta.get(indiceEdge);
            for (EventoTrafico evento : eventos) {
                String id = evento.nombre() + ":" + evento.lon() + ":" + evento.lat();
                if ((gasolinerasVisitadas != null && gasolinerasVisitadas.contains(id))
                        || distanciaPuntoTramo(evento.lon(), evento.lat(), edge) > 12) continue;
                if (gasolinerasVisitadas == null) gasolinerasVisitadas = new HashSet<>();
                gasolinerasVisitadas.add(id);
                esperaRestante = Math.max(esperaRestante, 180);
                estado = PosicionVehiculo.Estado.DETENIDO;
            }
        }

        private static double distanciaPuntoTramo(double lon, double lat, Edge edge) {
            double escalaX = 111_320 * Math.cos(Math.toRadians(lat));
            double ax = edge.origen.lon * escalaX;
            double ay = edge.origen.lat * 111_320;
            double bx = edge.destino.lon * escalaX;
            double by = edge.destino.lat * 111_320;
            double px = lon * escalaX;
            double py = lat * 111_320;
            double dx = bx - ax;
            double dy = by - ay;
            double longitud2 = dx * dx + dy * dy;
            double t = longitud2 == 0 ? 0
                    : Math.clamp(((px - ax) * dx + (py - ay) * dy) / longitud2, 0, 1);
            return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
        }

        private void recalcularSiCerrada(Pathfinder pathfinder, long versionRed,
                                         Map<RutaDesvioKey, List<Edge>> desviosCache) {
            this.versionRuta = versionRed;
            if (pathfinder == null || ruta.isEmpty() || indiceEdge >= ruta.size()) return;
            int bloqueada = -1;
            for (int i = indiceEdge; i < ruta.size(); i++) {
                if (ruta.get(i).cerrado) {
                    bloqueada = i;
                    break;
                }
            }
            if (bloqueada < 0) {
                if (estado == PosicionVehiculo.Estado.SIN_RUTA) {
                    estado = PosicionVehiculo.Estado.EN_RUTA;
                }
                return;
            }

            Edge actual = ruta.get(indiceEdge);
            boolean actualCerrada = actual.cerrado;
            Nodo inicio = actualCerrada ? actual.origen : actual.destino;
            Nodo destino = ruta.getLast().destino;
            RutaDesvioKey clave = new RutaDesvioKey(idRuta, inicio.id, destino.id,
                    siguienteParada, enIda);
            List<Edge> desvio = desviosCache.get(clave);
            if (desvio == null) {
                List<Nodo> waypointsRestantes = new ArrayList<>();
                waypointsRestantes.add(inicio);
                for (int i = siguienteParada; i < paradas.size(); i++) {
                    if (paradas.get(i) != inicio) waypointsRestantes.add(paradas.get(i));
                }
                if (waypointsRestantes.getLast() != destino) waypointsRestantes.add(destino);
                RutaResult resultado = pathfinder.ruta(waypointsRestantes);
                if (resultado == null || resultado.getEdges().isEmpty()) {
                    estado = PosicionVehiculo.Estado.SIN_RUTA;
                    return;
                }
                desvio = resultado.getEdges();
                if (desviosCache.size() >= MAX_DESVIOS_CACHE) desviosCache.clear();
                desviosCache.put(clave, desvio);
            }
            List<Edge> nuevaRuta = new ArrayList<>();
            if (!actualCerrada) {
                nuevaRuta.addAll(ruta.subList(0, indiceEdge + 1));
                nuevaRuta.addAll(desvio);
                indiceEdge = nuevaRuta.size() - desvio.size() - 1;
            } else {
                nuevaRuta.addAll(desvio);
                indiceEdge = 0;
                progresoMetros = 0;
            }
            ruta = List.copyOf(nuevaRuta);
            estado = PosicionVehiculo.Estado.EN_RUTA;
            LOG.info(() -> "Ruta recalculada para " + idRuta + " evitando tramo bloqueado");
        }

        private PosicionVehiculo posicion() {
            Edge edge = ruta.get(Math.min(indiceEdge, ruta.size() - 1));
            double fraccion = Math.min(1, progresoMetros / Math.max(0.01, edge.longitudMetros()));
            Nodo origen = edge.origen;
            Nodo destino = edge.destino;
            return new PosicionVehiculo(origen.x + (destino.x - origen.x) * fraccion,
                    origen.y + (destino.y - origen.y) * fraccion, tipo, pasajeros, capacidad,
                    idRuta, nombreRuta, color, estado);
        }

    }

    private static final class DespachoProgramado {
        private ServicioMicro servicio;
        private final int demandaPorSalida;
        private List<Nodo> paradasIda;
        private List<Nodo> paradasVuelta;
        private int pasajerosPendientes;
        private int unidadesDespachadas;
        private double proximoDespacho;

        private DespachoProgramado(ServicioMicro servicio) {
            this.servicio = servicio;
            demandaPorSalida = (int) Math.ceil((double) servicio.pasajerosEsperando()
                    / servicio.unidadesDisponibles());
        }
    }

    private record RutaDesvioKey(String rutaId, int origenId, int destinoId,
                                 int siguienteParada, boolean ida) {}
}
