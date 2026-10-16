import json
import random

with open("juliaca.geojson", encoding="utf-8") as f:
    data = json.load(f)

puntos = []
tipos_validos = ["ORIGEN", "PARADA", "DESTINO", "MERCADO", "TERMINAL",
                 "AEROPUERTO", "ESTACION", "PARADERO", "UNIVERSIDAD",
                 "COLEGIO", "SALUD", "OVALO", "AVENIDA", "JIRON"]

pid = 1
for feat in data["features"]:
    props = feat["properties"]
    nombre = props.get("name")
    if not nombre:
        continue

    # Clasificar por tipo
    tipo = None
    hw = props.get("highway", "")
    if "avenue" in nombre.lower() or "avenida" in nombre.lower():
        tipo = "AVENIDA"
    elif "jirón" in nombre.lower() or "jiron" in nombre.lower() or "jr" in nombre.lower():
        tipo = "JIRON"
    elif hw in ("trunk", "primary", "secondary"):
        tipo = "PARADA"
    else:
        continue  # descartamos lo que no encaja

    # Tomar el primer punto de la geometría
    geom = feat["geometry"]
    if geom["type"] == "LineString":
        lng, lat = geom["coordinates"][0]
    elif geom["type"] == "Polygon":
        lng, lat = geom["coordinates"][0][0]
    else:
        continue

    puntos.append({
        "id": f"p{pid}",
        "nombre": nombre,
        "x": round(lng, 6),
        "y": round(lat, 6),
        "tipo": tipo
    })
    pid += 1

with open("puntos_corregidos.json", "w", encoding="utf-8") as f:
    json.dump({"puntos": puntos}, f, ensure_ascii=False, indent=2)

print(f"Generados {len(puntos)} puntos reales desde OSM")