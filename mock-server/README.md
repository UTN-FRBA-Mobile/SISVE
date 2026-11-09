# SISVE Mock Server

Servidor de prueba para la app SISVE.

## Iniciar

```sh
npm ci
npm start
```

Por defecto escucha en `http://127.0.0.1:8585`. Se puede cambiar con las variables de entorno `HOST` y `PORT`.

Desde el emulador Android, usar `http://10.0.2.2:8585` para acceder al servidor que corre en la PC.


## Crear una solicitud de prueba

Primero, iniciá sesión, poné la ambulancia en servicio y registrá el token FCM del dispositivo. Luego ejecutá:


curl -X POST http://127.0.0.1:8585/internal/v1/ambulances/[ambulanceID]/service-requests

```sh
curl -X POST http://127.0.0.1:8585/internal/v1/ambulances/AMB-001/service-requests
```

La respuesta incluye el campo `id` de la solicitud; la push incluye ese mismo valor como `dispatchId`. Usalo para responder al despacho, consultarlo o solicitar el destino de traslado.

## Traslado al centro de salud

Método: `POST`

End point: `/api/v1/dispatch/:serviceRequestId/transport-destination` 

Asigna al azar uno de los nueve hospitales públicos de CABA a una solicitud aceptada. No requiere body. Persiste el resultado: repetir la llamada devuelve el mismo hospital.

## Autenticación

Las rutas protegidas requieren el header `Authorization`. En su valor, enviá el esquema `Bearer`, un espacio y el token recibido al iniciar sesión.

Ejemplo conceptual: `Authorization` → `Bearer` + `<token-de-sesión>`.

Obtené el token con `POST /api/v1/auth/login`. Credenciales ficticias de demo:

| Ambulancia | PIN | Operador |
|---|---|---|
| `AMB-001` | `123456` | Ana López |
| `AMB-001` | `654321` | Luis Pérez |
| `AMB-002` | `123456` | María Ruiz |

## Endpoints

### Autenticación y estado de la ambulancia

| Método | Ruta | Autenticación | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | No | Inicia sesión. JSON: `{"ambulanceId":"AMB-001","pin":"123456"}`. Devuelve token, ambulancia y operador. |
| `POST` | `/api/v1/status` | Sesión | Cambia el estado. JSON: `{"status":"EN_SERVICIO"}` o `{"status":"FUERA_DE_SERVICIO"}`. |
| `POST` | `/api/v1/notifications/fcm-token` | Sesión | Registra el token FCM. JSON: `{"token":"<token-fcm>"}`. |
| `POST` | `/api/v1/access/photo` | Sesión | Sube una foto JPEG de acceso como `multipart/form-data`: `photo` (archivo), `accessId` (UUID) y `capturedAt` (milisegundos Unix). |

### Solicitudes de servicio y despachos

| Método | Ruta | Autenticación | Descripción |
|---|---|---|---|
| `POST` | `/internal/v1/ambulances/:ambulanceId/service-requests` | No (uso interno/demo) | Crea una solicitud aleatoria y envía una push. Requiere que la ambulancia esté en servicio, que haya un token FCM registrado y que Firebase Admin esté configurado. |
| `POST` | `/api/v1/dispatch/:serviceRequestId/respond?accepted=true` | Sesión | Acepta la solicitud identificada por `serviceRequestId`. Usá `accepted=false` para rechazarla. |
| `GET` | `/api/v1/dispatch/:serviceRequestId` | Sesión | Obtiene la solicitud asignada a la ambulancia autenticada, incluyendo el destino de traslado si ya fue asignado. |
| `POST` | `/api/v1/dispatch/:serviceRequestId/transport-destination` | Sesión | Asigna al azar uno de los nueve hospitales públicos de CABA a una solicitud aceptada. No requiere body. Persiste el resultado: repetir la llamada devuelve el mismo hospital. |
| `GET` | `/internal/v1/service-requests/history?operator_id=:operatorId&ambulance_id=:ambulanceId` | No (uso interno/demo) | Consulta las solicitudes respondidas por el operador. `ambulance_id` es opcional; `operator_id` es obligatorio. |

En las rutas de despacho,  `:serviceRequestId` es el UUID de la solicitud, no el ID de la ambulancia. La ruta interna de creación usa `:ambulanceId`.


## Respuestas y restricciones relevantes

- Los endpoints protegidos responden `401` si falta el token o no es válido.
- El destino de traslado solo se asigna a una solicitud aceptada que corresponda a la ambulancia autenticada (`404` si no existe, `403` si pertenece a otra ambulancia, `409` si aún no fue aceptada).