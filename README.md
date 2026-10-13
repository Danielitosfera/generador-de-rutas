# Trafiqueishon — generador y simulador de rutas

Aplicación de escritorio JavaFX para visualizar las vías de Juliaca, editar
ubicaciones, definir rutas de micros y simular sus salidas programadas.

## Ejecutar

Requiere Java 21 y Maven:

```sh
mvn test
mvn javafx:run
```

## Uso

- El menú lateral agrupa ubicaciones, trayectos, simulación y eventos en un
  acordeón. Solo una sección permanece abierta a la vez.
- En **Ubicaciones** se pueden agregar puntos haciendo clic en el mapa o
  escribiendo coordenadas, editar/eliminar puntos e importar/exportar JSON.
  El archivo JSON usa las claves `id`, `nombre`, `x` (longitud), `y` (latitud)
  y `tipo`; también se permite importar Excel.
- En **Trayectos** se calculan las rutas de partida y retorno con las paradas
  actuales, y se exportan a GeoJSON, GPX o CSV.
- En **Simulación de buses** se ajusta la hora inicial y la velocidad del reloj.
  Los sprites se dibujan únicamente desde zoom 3 para reducir el trabajo de
  renderizado al alejarse.
- **Pasajeros** configura la demanda de cada servicio: se asignan hasta 18
  pasajeros por micro despachada y el excedente espera el siguiente horario.
- Las rutas de **ida** y **vuelta** se dibujan encima de la red con colores
  exclusivos (azul neón y rojo neón), con borde oscuro y casillas separadas
  para mostrar/ocultar cada una. Los botones de enfoque encuadran cada ruta.
- En **Rutas y micros disponibles** se crean servicios con nombre, paraderos
  seleccionados y ordenables, frecuencia de salida y cantidad de unidades. El
  sistema fija como terminales los dos paraderos geográficamente más alejados
  y ordena las paradas intermedias para minimizar el tiempo total con Dijkstra.
  Los servicios se guardan en `~/.trafiqueishon/rutas-micros.json` y se
  reconstruyen contra la red al iniciar.
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
- En **Eventos y mapa de calor** se agregan trancaderas, vías cerradas y
  vendedores ambulantes. La proximidad se calcula contra el segmento vial;
  las vías cerradas se remarcan en rojo y se excluyen al recalcular rutas. Si
  se agrega un evento durante una simulación, esta se pausa para que pueda
  reiniciarse con las rutas recalculadas.
- El mapa admite zoom con rueda y botones, paneo con arrastre y vista completa
  con doble clic.

## Alcance actual

Los eventos y su estado solo duran mientras la aplicación está abierta. Las
rutas de micros sí se guardan localmente. El mapa vial es un recurso GeoJSON
estático incluido en el proyecto.
La ocupación de pasajeros se representa de forma determinista para probar la
capacidad; no modela conteos observados ni ascensos/bajadas en cada paradero.
El planificador usa Dijkstra con los costos de tiempo actuales; la calidad de
las estimaciones de hora punta y eventos depende de los factores configurados,
no de mediciones de tráfico en vivo.