import { randomInt } from 'node:crypto';

export const publicHospitals = [
  {
    id: 'hospital-pinero',
    name: 'Hospital Piñero',
    address: 'Av. Varela 1301, Flores, CABA',
    latitude: -34.6435413,
    longitude: -58.4543171,
  },
  {
    id: 'hospital-durand',
    name: 'Hospital Durand',
    address: 'Av. Díaz Vélez 5044, Caballito, CABA',
    latitude: -34.6088669,
    longitude: -58.4380107,
  },
  {
    id: 'hospital-rivadavia',
    name: 'Hospital Rivadavia',
    address: 'Av. General Las Heras 2670, Recoleta, CABA',
    latitude: -34.5846894,
    longitude: -58.4016762,
  },
  {
    id: 'hospital-argerich',
    name: 'Hospital Argerich',
    address: 'Pi y Margall 750, La Boca, CABA',
    latitude: -34.6281137,
    longitude: -58.3655686,
  },
  {
    id: 'hospital-santojanni',
    name: 'Hospital Santojanni',
    address: 'Pilar 950, Liniers, CABA',
    latitude: -34.6486894,
    longitude: -58.5150399,
  },
  {
    id: 'hospital-tornu',
    name: 'Hospital Tornú',
    address: 'Combatientes de Malvinas 3002, Parque Chas, CABA',
    latitude: -34.5860192,
    longitude: -58.4717026,
  },
  {
    id: 'hospital-ramos-mejia',
    name: 'Hospital Ramos Mejía',
    address: 'Gral. Urquiza 609, Balvanera, CABA',
    latitude: -34.6180444,
    longitude: -58.4101818,
  },
  {
    id: 'hospital-fernandez',
    name: 'Hospital Fernández',
    address: 'Cerviño 3356, Palermo, CABA',
    latitude: -34.5813693,
    longitude: -58.4069437,
  },
  {
    id: 'hospital-pirovano',
    name: 'Hospital Pirovano',
    address: 'Av. Monroe 3555, Coghlan, CABA',
    latitude: -34.5641411,
    longitude: -58.4718644,
  },
];

export function choosePublicHospital() {
  return publicHospitals[randomInt(publicHospitals.length)];
}

export function registerTransportDestinations(app, store, requireSession) {
  app.post(
    '/api/v1/dispatch/:id/transport-destination',
    requireSession,
    async (req, res) => {
      const result = await store.assignTransportDestination(
        req.params.id,
        res.locals.session.ambulanceId,
        choosePublicHospital,
      );

      if (result.kind === 'not_found') {
        return res.status(404).json({ error: 'Solicitud de servicio inexistente.' });
      }

      if (result.kind === 'wrong_ambulance') {
        return res.status(403).json({ error: 'La solicitud no está asignada a esta ambulancia.' });
      }

      if (result.kind === 'not_accepted') {
        return res.status(409).json({ error: 'Solo se puede asignar un destino a una solicitud aceptada.' });
      }

      return res.status(200).json({
        dispatchId: req.params.id,
        transportDestination: result.serviceRequest.transportDestination,
      });
    },
  );
}
