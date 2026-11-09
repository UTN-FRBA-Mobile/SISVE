# 🚑 SISVE - Mock Server

Backend de la app SISVE

## Simular emergencia

Hay que generar desde consola la solicitud de emergencia para que se notifique a la app :

curl -X POST http://`[IP]`:`[PUERTO]`/internal/v1/ambulances/`[AmbulanceID]`/service-requests

Ejemlo:
`curl -X POST http://127.0.0.1:8585/internal/v1/ambulances/AMB-001/service-requests`




