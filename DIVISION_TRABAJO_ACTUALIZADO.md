# División de trabajo - SISVE

## Cómo vamos a trabajar

Cada uno tiene su rama y su módulo separado. Nadie pushea directo a `main`. Todo va a `develop` primero y cuando algo está terminado se hace PR.

### Ramas base

```
main        → solo código que funciona y fue revisado
develop     → acá integramos todo, es la rama "en progreso"
```

Cada feature tiene su propia rama que sale de `develop`:

```
feature/setup-base
feature/login-pin
feature/facial-auth
feature/ambulance-identity
feature/foreground-service
feature/api-client
```

Cuando terminás tu feature, hacés PR a `develop`. Si hay conflicto, lo resolvés vos.

---

## APIs del sistema operativo que usamos (el TP pide mínimo 2)

| API | Quién | Dónde |
|-----|-------|-------|
| Cámara | Miembro 3 | Foto al login |
| Geolocalización | Miembro 5 | GPS continuo |
| Push Notifications | Miembro 3 | Recibir despachos de emergencia |

---

## Quién hace qué

### Miembro 1 — Setup del proyecto + navegación + pantalla principal + despacho
**Rama:** `feature/setup-base`

- Crear el proyecto Android en Android Studio
- Definir la estructura de carpetas (`ui`, `data`, `domain`, `service`, etc.)
- Agregar las dependencias en `build.gradle` (Retrofit, Room, Coroutines, Navigation, etc.)
- Configurar el `AndroidManifest.xml` con los permisos necesarios
- Armar el grafo de navegación (`NavGraph`) con todas las pantallas aunque estén vacías al principio
- Pantalla principal / dashboard: mostrar estado actual de la ambulancia (libre, ocupado, en camino) y botón para cambiar estado
- **Pantalla de despacho / llamado de emergencia**: cuando llega un llamado, mostrar la dirección y los botones "Aceptar" / "Rechazar" → este es el caso de uso central de la app
- Tema visual (colores, tipografía)

> Esto hay que hacerlo primero. Cuando esté listo avisás al grupo y todos hacemos `git pull` de develop.

---

### Miembro 2 — Login con PIN + gestión de sesión
**Rama:** `feature/login-pin`

- Pantalla de ingreso de PIN (4 o 6 dígitos, números grandes)
- Pantalla de creación de PIN para el primer uso
- Lógica de guardar/verificar el PIN (encriptado en SharedPreferences)
- Manejo del estado de sesión global: si ya está logueado no volver a pedir PIN
- Pantalla de "cerrar sesión" / bloquear app
- Timeout de sesión: si la app estuvo inactiva X minutos, pedir PIN de nuevo

> El resto de la app va a depender de saber si hay sesión activa. Definir bien cómo exponer eso (un `SessionManager` o similar).

---

### Miembro 3 — Foto al iniciar sesión + historial de accesos + Push Notifications
**Rama:** `feature/facial-auth`

- Abrir la cámara frontal al momento del login y capturar una foto automáticamente
- Guardar la foto localmente junto con timestamp y qué ambulancia era
- Pantalla de historial: lista de los últimos accesos con foto, fecha y hora
- Pedir permiso de cámara en runtime
- **Push Notifications**: configurar Firebase Cloud Messaging (FCM) para recibir despachos de emergencia cuando la app está en segundo plano
- Cuando llega una push, abrir la pantalla de despacho (Miembro 1)

> La cámara y las push notifications son dos de las APIs del SO que exige el TP.

---

### Miembro 4 — Identidad de la ambulancia + pantalla de configuración
**Rama:** `feature/ambulance-identity`

- Flujo de primer uso: "¿A qué ambulancia pertenece este dispositivo?"
- Opción de ingresar el ID manualmente o escanear un QR
- Guardar el ID localmente (SharedPreferences)
- Clase `AmbulanceSession` accesible desde toda la app con el ID actual
- **Pantalla de configuración**: URL del servidor (no hardcodeada, el TP lo prohíbe), intervalo de envío de GPS, opción de resetear ambulancia asignada
- Validar que no se pueda usar la app sin tener una ambulancia asignada

> El ID de ambulancia lo van a necesitar Miembro 5 y 6 para mandar datos al servidor.

---

### Miembro 5 — Foreground Service + GPS + pantalla de mapa
**Rama:** `feature/foreground-service`

- Implementar un `ForegroundService` que corre mientras la app está activa
- Obtener la ubicación GPS periódicamente (intervalo configurable, ver Miembro 4)
- Mostrar la notificación persistente requerida por Android con el estado actual
- Llamar al Miembro 6 para enviar la ubicación al servidor
- Pedir permisos de ubicación en runtime (incluyendo `ACCESS_BACKGROUND_LOCATION` para Android 10+)
- Manejar el caso de GPS desactivado: avisar al usuario
- **Pantalla de mapa**: mostrar la posición actual de la ambulancia en un mapa (Google Maps o MapBox) y, cuando hay un despacho activo, mostrar también la dirección del llamado
- Botón en la notificación para cambiar estado sin abrir la app

> Este es el módulo más crítico. Revisar bien el ciclo de vida del Service y el consumo de batería.

---

### Miembro 6 — API Client + base de datos local
**Rama:** `feature/api-client`

- Configurar Retrofit con los endpoints del servidor
- Endpoints mínimos: login, registrar ambulancia, enviar ubicación, actualizar estado, recibir/responder despachos
- Crear los modelos de datos (data classes en Kotlin) para requests y responses
- Configurar Room para guardar datos localmente (ubicaciones pendientes de enviar si no hay red)
- Manejo de errores de red: sin internet → guardar localmente y reintentar cuando vuelva la conexión
- Interceptor de autenticación (agregar token a los headers automáticamente)

> Si no hay backend todavía, mockeá las respuestas con un archivo JSON local. Definir bien los modelos porque los demás los van a usar. La URL del servidor tiene que venir de la configuración (Miembro 4), nunca hardcodeada.

---

## Flujo principal de la app (caso de uso completo)

```
Primer uso:
  Abrir app → asignar ambulancia (M4) → crear PIN (M2) → foto (M3) → dashboard (M1)

Uso normal:
  Abrir app → ingresar PIN (M2) → foto (M3) → dashboard (M1)
  Foreground service activo en segundo plano enviando GPS (M5 → M6)

Recibir un llamado:
  Push notification (M3) → pantalla de despacho (M1) → aceptar/rechazar (M1 → M6)
  Si acepta → mapa con la dirección (M5)
```

---

## Estimación de esfuerzo

| Miembro | Módulo | Complejidad |
|---------|--------|-------------|
| 1 | Setup + Nav + Dashboard + Despacho | Media-Alta |
| 2 | Login PIN + Sesión | Media |
| 3 | Foto + Historial + Push | Media-Alta |
| 4 | Identidad ambulancia + Config | Media |
| 5 | Foreground Service + GPS + Mapa | Alta |
| 6 | API Client + Room | Alta |

---

## Orden recomendado para arrancar

1. **Miembro 1** sube el proyecto base con el NavGraph → todos clonan
2. **Miembro 4** define `AmbulanceSession` → lo necesitan M5 y M6
3. **Miembro 6** define los modelos de datos → los necesita M5 y M1
4. A partir de acá el resto puede avanzar en paralelo

---

## Dependencias entre módulos

```
Setup + NavGraph (M1)
    ↓
Identidad ambulancia / AmbulanceSession (M4) ←── M5 y M6 necesitan el ID
SessionManager (M2) ←── M3 necesita saber cuándo mostrar la cámara
Modelos de datos (M6) ←── M5 y M1 los necesitan
FCM / Push (M3) → abre pantalla de despacho (M1)
Foreground Service (M5) → llama a M6 para enviar ubicación
```

---

## Reglas básicas

- Antes de empezar a codear: `git pull origin develop`
- Commit seguido, no acumules días de cambios en uno solo
- Si rompés algo en develop, avisás inmediatamente
- La URL del servidor NUNCA va hardcodeada en el código (lo prohíbe el TP)
- Cada uno commitea con su usuario de GitHub (lo pide el TP)
