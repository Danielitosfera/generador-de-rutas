# Trafiqueishon — generador y simulador de rutas

Aplicación de escritorio JavaFX para visualizar las vías de Juliaca, editar
ubicaciones, definir rutas de micros y simular sus salidas programadas.

## Ejecutar

Requiere Java 21 y Maven:

```sh
mvn test
mvn javafx:run
```

La ejecución JavaFX queda limitada a 768 MiB de heap (`-Xmx768m`) para dejar
memoria disponible al sistema en equipos con 2 GiB de RAM. El mapa GeoJSON se
lee por feature y las coordenadas gráficas se almacenan como `float`, reduciendo
el uso de memoria de la geometría proyectada. La ventana usa un tamaño inicial
de 1200×760 y el render se limita a 24 FPS.

## Uso

- El panel lateral está organizado en pestañas: **Puntos**, **Rutas**,
  **Pasajeros**, **Simular** y **Eventos**. La barra superior incluye Archivo,
  Simulación, Vistas y Ayuda, además de los controles de marcha y velocidad.
- Pulsa **Tab** para ocultar/mostrar el panel y **F11** para entrar en modo
  presentación; **Esc** sale de la presentación. La simulación continúa al
  ocultar los paneles.
- En **Ubicaciones** se pueden agregar puntos haciendo clic en el mapa o
  escribiendo coordenadas, editar/eliminar puntos e importar/exportar JSON.
  El archivo JSON usa las claves `id`, `nombre`, `x` (longitud), `y` (latitud)
  y `tipo`; también se permite importar Excel.
- En **Trayectos** se calculan las rutas de partida y retorno con las paradas
  actuales, y se exportan a GeoJSON, GPX o CSV.
- En **Simular** se ajusta la hora inicial y la velocidad del reloj (0.5x,
  1x, 2x, 5x, 10x, 20x o 30x). Los sprites se dibujan únicamente desde zoom 3;
  cambian de color si están detenidos, averiados o sin ruta.
- **Pasajeros** configura la demanda base de cada servicio. El modelo actual
  ajusta la generación por salida según la hora (mañana 2.5x, mediodía 1.8x,
  tarde 2.8x y noche 0.2x), limita el abordaje a 18 pasajeros por micro y deja
  una espera aproximada de 15 segundos en las paradas configuradas.
- Las rutas de **ida** y **vuelta** se dibujan encima de la red con colores
  exclusivos (azul neón y rojo neón), con borde oscuro y casillas separadas
  para mostrar/ocultar cada una. Los botones de enfoque encuadran cada ruta.
- En **Rutas y micros disponibles** se crean servicios con nombre, paraderos
  seleccionados y ordenables, frecuencia de salida y cantidad de unidades. El
  sistema fija como terminales los dos paraderos geográficamente más alejados
  y ordena las paradas intermedias para minimizar el tiempo total con Dijkstra.
  Los servicios se guardan en `~/.trafiqueishon/rutas-micros.json` y se
  reconstruyen contra la red al iniciar.
  Cada ruta se puede editar después: nombre, paraderos y orden, frecuencia,
  cantidad de micros, demanda y color. Los colores fluorescentes se asignan
  sin repetir y se puede seleccionar uno distinto desde el editor.
  La simulación despacha unidades desde ambos terminales a la hora indicada y
  continúa hasta lanzar la flota completa, aunque la demanda de pasajeros se
  agote. Al llegar al otro terminal, cada unidad espera un intervalo antes de
  recorrer la ruta inversa. Se asignan hasta 18 pasajeros por salida.
  Cada servicio se dibuja con su color propio; al activar las casillas de ida o
  vuelta, se ocultan esos colores de ruta y se muestran los trazados generales
  azul y rojo neón.
- El reloj simulado incrementa el tráfico entre 07:00–09:00, 12:00–14:00 y
  17:00–20:00. Los nombres de calles se dibujan en blanco con contorno oscuro
  al acercar el mapa, limitando su densidad para mantener legibilidad. La hora
  configurada y el reloj en marcha también aparecen en la barra inferior.
- En **Eventos** se agregan trancaderas, cierres, vendedores ambulantes,
  lluvia, accidentes, venteos/manifestaciones, gasolineras y semáforos rotos.
  El gestor notifica cambios a sus listeners; los cierres recalculan desvíos
  con A* sin detener la simulación. La lluvia, las trancaderas y los semáforos
  alteran el factor de velocidad; una gasolinera detiene la unidad durante
  tres minutos simulados. Los cierres y accidentes se mantienen hasta eliminar
  el evento.
- Al detener la simulación aparece un resumen con abordajes, ocupación media
  de la flota, unidades con incidencias y tiempo simulado.
- El mapa admite zoom con rueda y botones, paneo con arrastre y vista completa
  con doble clic.

## Alcance actual

Los eventos y su estado solo duran mientras la aplicación está abierta. Las
rutas de micros sí se guardan localmente. El mapa vial es un recurso GeoJSON
estático incluido en el proyecto. El modelo de pasajeros todavía no asigna
origen/destino por persona ni simula ascensos y descensos en cada parada; el
resumen no calcula tiempos promedio individuales ni cuellos de botella.
Accidentes y manifestaciones no expiran automáticamente: se retiran desde la
pestaña **Eventos**. La generación de demanda es una aproximación configurable,
no usa mediciones de tráfico en vivo. El nombre del proyecto Maven es
`generador-rutas`.
