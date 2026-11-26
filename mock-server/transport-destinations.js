import { randomInt } from 'node:crypto';

export const publicHospitals = [

  {
    id: 'hospital-argerich',
    name: 'Hospital Argerich',
    address: 'Pi y Margall 750, La Boca, CABA',
    latitude: -34.6281137,
    longitude: -58.3655686,
  },
  {
    id: 'hospital-tornu',
    name: 'Hospital Tornú',
    address: 'Combatientes de Malvinas 3002, Parque Chas, CABA',
    latitude: -34.5860192,
    longitude: -58.4717026,
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
