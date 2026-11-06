# Simular la ubicación en el emulador (demo)

La app usa `LocationManager` con el proveedor GPS, así que funciona con cualquier imagen del emulador.

## Ubicación fija
1. Con el emulador abierto, clic en `⋯` (Extended controls) → **Location**.
2. Buscá un lugar en el mapa o escribí latitud/longitud y presioná **Set location**.

## Ruta en movimiento (recomendado para la demo)
1. Extended controls → Location → pestaña **Routes** (o **GPX/KML data** → **Load GPX/KML**).
2. Cargá `docs/demo/ruta-demo.gpx`: recorrido por calles de CABA (~5,7 km, unos 9 min a 1x) desde Plaza Italia hasta Av. Corrientes ~1234, con varias vueltas (Santa Fe, Azcuénaga, Viamonte, Paraguay, Riobamba, Uruguay).
3. Elegí la velocidad y presioná **Play route**. Se puede activar *Repeat playback*.

## Secuencia de la demo
1. Iniciá sesión y presioná **Entrar en Servicio**: aceptá los permisos de ubicación y notificaciones.
2. Aparece la notificación persistente "SISVE activo" con el botón **Salir de servicio**.
3. Reproducí la ruta GPX: la ubicación se envía cada N segundos (configurable en Configuración).
4. Simulá un llamado y aceptalo: el mapa muestra la ambulancia, el destino, la línea y la distancia.

## Notas
- El recorrido por calles lo calcula OSRM (servidor público de demostración) y los tiles los sirve CARTO; si no hay internet, el mapa muestra una línea recta punteada.
- El mapa (tiles) y la geocodificación de la dirección necesitan internet en el emulador.
- La geocodificación necesita Google Play Services en la imagen del emulador; si falla, el mapa avisa y muestra solo la posición actual.
- Para ver el GPS desactivado: Quick settings del emulador → apagar *Location*.
