# Consideraciones del Miembro 5 — Foreground Service + GPS + Mapa

Rama: `feature/miembro-5-foreground-service-gps-mapa`. Este documento resume lo que hay que saber antes de integrar con `develop`.

## 1. Setup obligatorio (cada uno en su máquina)

1. **Key de CARTO** (estilo limpio del mapa). Agregar en `local.properties` (raíz, ignorado por git):
   ```
   CARTO_API_KEY=la_key_del_grupo
   ```
   - **Nunca commitearla** ni pegarla en el código. Pasarla por un canal privado.
   - Sin key la app funciona igual, pero el mapa usa el estilo estándar de OpenStreetMap (con íconos de comercios).
   - Hay que recompilar después de agregarla.
2. **Gradle 8.9.** Android Studio puede ofrecer subir el wrapper a 8.14.x al sincronizar: no aceptarlo, para no ensuciar el repo.
3. **Emulador:** usar una imagen **API 35 (Android 15) con Google Play**. Las imágenes preview (Android 17 / 16 KB) no renderizan bien y generan ANRs falsos.
4. **URL del servidor:** el valor por defecto ahora es `http://10.0.2.2:8585/api/v1/` (la PC vista desde el emulador). Con un **celular real** hay que cambiarla en Configuración por la IP de la PC en la misma Wi-Fi.

## 2. Qué hace este módulo

- **`LocationService`** (foreground service, tipo `location`): corre mientras el estado es `EN_SERVICIO`. Lo arranca `LocationServiceController` (observa `AmbulanceStatusStore`) y se detiene solo al salir de servicio o cerrar sesión.
- **Notificación persistente** con el estado, aviso si el GPS está apagado y botón **"Salir de servicio"** (cambia el estado sin abrir la app).
- **Envío de ubicación:** cada lectura va a `ApiRepository.sendLocation()` respetando el intervalo de Configuración; si falla, queda en Room y se reintenta con `sendPendingLocations()`.
- **Permisos en runtime** al tocar "Entrar en Servicio": ubicación precisa → notificaciones (Android 13+) → ubicación en segundo plano (Android 10+). Si se rechaza la ubicación precisa, no se entra en servicio.
- **Aviso de GPS desactivado** en el Dashboard, con acceso a los ajustes de ubicación.
- **Mapa** (`MapScreen`): posición de la ambulancia, destino del despacho aceptado, ruta **por calles**, distancia restante, seguimiento con rotación según el rumbo y un cartel de estado de la ruta (siguiendo / calculando / recalculando / sin ruta).

## 3. Archivos de otros miembros que se tocaron

| Miembro | Archivo | Cambio |
|---|---|---|
| M1 | `DispatchViewModel` | Al aceptar, guarda el despacho en `ActiveDispatchStore` (el mapa toma el destino de ahí) |
| M1 | `DashboardScreen` / `DashboardViewModel` | Flujo de permisos al entrar en servicio, aviso de GPS apagado, limpia el despacho activo al volver |
| M1 | `MapScreen` | Reemplaza el placeholder; conserva el botón temporal "Simular llegada" |
| M2 | `SessionManager` | Timeout de inactividad de **30 s a 5 min** (una sola constante) |
| M4 | `AmbulancePreferences` | `DEFAULT_URL` pasa a `10.0.2.2` |
| Todos | `SisveApplication`, `AndroidManifest`, `strings.xml`, `build.gradle.kts`, `libs.versions.toml` | Canal de notificación, servicio y permiso, strings nuevos, dependencia osmdroid y `CARTO_API_KEY` vía `BuildConfig` |

Posibles conflictos al mergear con `feature/login-photo` (M3): ambos tocan `build.gradle.kts`, `libs.versions.toml`, `AndroidManifest.xml` y `strings.xml`. Se resuelven conservando ambos lados.

## 4. Qué pueden usar los demás

- **M1 / M3 (despacho):** al aceptar un despacho llamar a `ActiveDispatchStore.set(call)`. El mapa geocodifica `call.address` y calcula la ruta solo. Cuando llegue la push (M3), alcanza con abrir la pantalla de despacho; el resto del flujo ya funciona.
- **M4 (configuración):** el intervalo de GPS se lee **al iniciar el servicio**; un cambio en Configuración se aplica la próxima vez que se entre en servicio.
- **M6 (API):** el servicio usa `ApiRepository.sendLocation()` y `sendPendingLocations()`. El botón de la notificación deja un `TODO Miembro 6` para avisar el cambio de estado al servidor cuando `updateStatus` deje de estar mockeado.
- **Cualquiera:** `LocationStore` expone la última posición (`lastFix`) y si el GPS está activo (`gpsEnabled`).

## 5. Decisiones técnicas

- **`LocationManager` (GPS_PROVIDER) en vez de Fused Location:** funciona en cualquier imagen del emulador (también sin Play Services) y con la simulación de ubicación.
- **Frecuencia adaptativa:** con el mapa abierto el GPS lee cada 1 s; fuera del mapa usa el intervalo configurado, para cuidar la batería. Al servidor siempre se envía según el intervalo configurado.
- **Mapa aislado:** solo `ui/map/SisveMap.kt` conoce osmdroid. Cambiar a Google Maps o MapBox implica reescribir ese archivo, la dependencia y la key.
- **Servicios externos** (todos sin cuenta propia del grupo salvo CARTO):
  - Tiles: **CARTO** (con key; plan gratuito para proyectos educativos).
  - Ruta por calles: **OSRM** público de demostración (sin garantía de disponibilidad; si falla, el mapa muestra una línea recta punteada).
  - Dirección → coordenadas: `android.location.Geocoder` (necesita internet y Google Play Services).
- **Atribución obligatoria** en el mapa: "© OpenStreetMap contributors © CARTO". No quitarla.
- **Privacidad:** el pedido a OSRM usa un `OkHttpClient` propio, para que el token de la ambulancia no viaje a servicios de terceros.

## 6. Cómo probar

Guía completa en [`docs/demo/SIMULAR_UBICACION.md`](demo/SIMULAR_UBICACION.md). Resumen:

1. Levantar el mock-server (`cd mock-server && npm install && npm start`).
2. Correr la app en un emulador API 35.
3. En el emulador: **⋯ (Extended controls) → Location → Routes**, cargar `docs/demo/ruta-demo.gpx` (≈5,7 km por calles de CABA, con varias vueltas) y darle Play.
4. Login → **Entrar en Servicio** (aceptar permisos) → generar el llamado con el POST documentado en [`mock-server/README.md`](../mock-server/README.md) → **Aceptar** → mapa.

## 7. Limitaciones conocidas y pendientes

- **Despacho de prueba:** se genera con un POST al mock-server; el push de despacho en segundo plano depende de FCM (M3).
- **Botón temporal** ("Simular llegada") a sacar antes de entregar.
- **Llegada automática:** no hay detección; el paso a PostArrival sigue siendo manual.
- **Destino de traslado:** el mock-server ya permite asignar un hospital público de CABA aleatorio cuando se confirma el traslado. La app todavía debe integrarlo (ver contrato abajo).
- **Estado al servidor:** `updateStatus` sigue mockeado (M6).
- **Geocodificación:** puede fallar sin internet o con direcciones ambiguas; el mapa avisa y muestra solo la posición actual.
- **Celulares reales:** algunas marcas (Xiaomi, Samsung, Huawei) pueden cortar el servicio por la optimización de batería. Desactivarla para SISVE si pasa.
- **Sin tests automáticos** para la lógica de ruta y rumbo todavía.

## 8. Integración pendiente: destino del traslado

El mock-server implementa `POST /api/v1/dispatch/:serviceRequestId/transport-destination`. La app debe llamarlo solo cuando el operador confirme que traslada al paciente, usando el mismo `Authorization: Bearer <token>` que los demás endpoints autenticados. No requiere body.

- Solo se asigna a una solicitud existente, asignada a la ambulancia autenticada y con estado `ACCEPTED`.
- La primera llamada selecciona al azar uno de estos nueve hospitales públicos de CABA y lo persiste en la solicitud. Los reintentos devuelven el mismo destino.
- Respuesta `200`:
  ```json
  {
    "dispatchId": "id-de-la-solicitud",
    "transportDestination": {
      "id": "hospital-durand",
      "name": "Hospital Durand",
      "address": "Av. Díaz Vélez 5044, Caballito, CABA",
      "latitude": -34.6088669,
      "longitude": -58.4380107
    }
  }
  ```
- Errores: `401` sesión inválida, `403` solicitud de otra ambulancia, `404` solicitud inexistente y `409` si todavía no fue aceptada.
- La solicitud también incluye `transportDestination` al consultarse con `GET /api/v1/dispatch/:serviceRequestId`.
- El módulo y el catálogo están en `mock-server/transport-destinations.js`; la persistencia se realiza junto al historial en `mock-server/data/service-requests.json`.
- Para integrar la app: invocar el POST al confirmar traslado y usar la dirección/coordenadas devueltas como destino de la ruta al centro de salud. La selección aleatoria es solo para el mock; no representa disponibilidad, pertinencia clínica ni asignación real.
