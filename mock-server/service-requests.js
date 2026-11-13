import { randomInt, randomUUID } from 'node:crypto';
import { mkdir, readFile, rename, rm, writeFile } from 'node:fs/promises';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { registerTransportDestinations } from './transport-destinations.js';

const serviceRequestsFile = fileURLToPath(new URL('./data/service-requests.json', import.meta.url));

const serviceRequestTemplates = [
  {
    address: 'Av. Medrano 421, CABA',
    emergencyType: 'Accidente vial',
    priority: 'ALTA',
    description: 'Colisión entre dos vehículos. Se solicita asistencia médica.',
    latitude: -34.5981,
    longitude: -58.4202,
  },
  {
    address: 'Av. Rivadavia 5200, CABA',
    emergencyType: 'Emergencia médica',
    priority: 'MEDIA',
    description: 'Persona con malestar general. Se solicita evaluación médica.',
    latitude: -34.6167,
    longitude: -58.4231,
  },
  {
    address: 'Parque Centenario, CABA',
    emergencyType: 'Asistencia en vía pública',
    priority: 'BAJA',
    description: 'Se solicita asistencia para una persona caída, consciente.',
    latitude: -34.6064,
    longitude: -58.4356,
  },
  {
    address: 'Av. Santa Fe 4200, Palermo, CABA',
    emergencyType: 'Emergencia médica',
    priority: 'MEDIA',
    description: 'Persona con dificultad respiratoria en un comercio. Se solicita evaluación médica.',
    latitude: -34.5832,
    longitude: -58.4244,
  },
  {
    address: 'Av. Pueyrredón 400, Balvanera (Once), CABA',
    emergencyType: 'Accidente vial',
    priority: 'ALTA',
    description: 'Motociclista involucrado en una colisión. Se solicita asistencia médica.',
    latitude: -34.6096,
    longitude: -58.4075,
  },
  {
    address: 'Florida 500, San Nicolás, CABA',
    emergencyType: 'Asistencia en vía pública',
    priority: 'BAJA',
    description: 'Persona mayor con mareos en la vía pública. Se solicita evaluación.',
    latitude: -34.6006,
    longitude: -58.3765,
  },
  {
    address: 'Av. Callao 1200, Recoleta, CABA',
    emergencyType: 'Emergencia médica',
    priority: 'ALTA',
    description: 'Paciente con dolor intenso en el pecho. Se solicita asistencia urgente.',
    latitude: -34.5895,
    longitude: -58.3949,
  },
  {
    address: 'Av. Nazca 58, Flores, CABA',
    emergencyType: 'Accidente vial',
    priority: 'MEDIA',
    description: 'Choque leve entre dos vehículos con una persona lesionada.',
    latitude: -34.6299940,
    longitude: -58.4701188,
  },
  {
    address: 'Av. Cruz 6400, Villa Lugano, CABA',
    emergencyType: 'Emergencia médica',
    priority: 'ALTA',
    description: 'Se solicita traslado y atención para una persona con una lesión en una pierna.',
    latitude: -34.6835,
    longitude: -58.4730,
  },
  {
    address: 'Av. del Libertador 500, Retiro, CABA',
    emergencyType: 'Asistencia en vía pública',
    priority: 'MEDIA',
    description: 'Persona descompensada en las inmediaciones de una estación de transporte.',
    latitude: -34.5874,
    longitude: -58.3767,
  },
];

export class ServiceRequestStore {
  #pendingWrite = Promise.resolve();

  constructor(filePath = serviceRequestsFile) {
    this.filePath = filePath;
  }

  async list() {
    await this.#pendingWrite;
    return this.#read();
  }

  async append(serviceRequest) {
    await this.#mutate(history => {
      history.push(serviceRequest);
      return { changed: true };
    });
  }

  async respond(id, { accepted, ambulanceId, operatorId }) {
    return this.#mutate(history => {
      const serviceRequest = history.find(request => request.id === id);

      if (!serviceRequest) {
        return { result: { kind: 'not_found' } };
      }

      if (serviceRequest.ambulanceId !== ambulanceId) {
        return { result: { kind: 'wrong_ambulance' } };
      }

      if (serviceRequest.status !== 'PENDING') {
        return { result: { kind: 'already_responded' } };
      }

      serviceRequest.status = accepted ? 'ACCEPTED' : 'REJECTED';
      serviceRequest.respondedAt = new Date().toISOString();
      serviceRequest.respondedByOperatorId = operatorId;

      return { changed: true, result: { kind: 'updated', serviceRequest } };
    });
  }

  async assignTransportDestination(id, ambulanceId, chooseDestination) {
    return this.#mutate(history => {
      const serviceRequest = history.find(request => request.id === id);

      if (!serviceRequest) {
        return { result: { kind: 'not_found' } };
      }

      if (serviceRequest.ambulanceId !== ambulanceId) {
        return { result: { kind: 'wrong_ambulance' } };
      }

      if (serviceRequest.status !== 'ACCEPTED') {
        return { result: { kind: 'not_accepted' } };
      }

      if (serviceRequest.transportDestination) {
        return { result: { kind: 'already_assigned', serviceRequest } };
      }

      serviceRequest.transportDestination = chooseDestination();
      return { changed: true, result: { kind: 'assigned', serviceRequest } };
    });
  }

  async finalizeActiveByAmbulance(ambulanceId) {
    return this.#mutate(history => {
      let updatedCount = 0;
      for (const serviceRequest of history) {
        if (serviceRequest.ambulanceId === ambulanceId && (serviceRequest.status === 'ACCEPTED' || serviceRequest.status === 'PENDING')) {
          serviceRequest.status = 'COMPLETED';
          serviceRequest.completedAt = new Date().toISOString();
          updatedCount++;
        }
      }
      return { changed: updatedCount > 0, result: { updatedCount } };
    });
  }

  async #mutate(mutator) {
    const operation = this.#pendingWrite.then(async () => {
      const history = await this.#read();
      const outcome = mutator(history);

      if (!outcome.changed) {
        return outcome.result;
      }

      await mkdir(dirname(this.filePath), { recursive: true });

      const temporaryPath = `${this.filePath}.${randomUUID()}.tmp`;
      try {
        await writeFile(temporaryPath, `${JSON.stringify(history, null, 2)}\n`, {
          encoding: 'utf8',
          flag: 'wx',
        });
        await rename(temporaryPath, this.filePath);
      } catch (error) {
        await rm(temporaryPath, { force: true });
        throw error;
      }

      return outcome.result;
    });

    this.#pendingWrite = operation.catch(() => {});
    return operation;
  }

  async #read() {
    let contents;

    try {
      contents = await readFile(this.filePath, 'utf8');
    } catch (error) {
      if (error.code === 'ENOENT') {
        return [];
      }

      throw error;
    }

    const history = JSON.parse(contents);

    if (!Array.isArray(history)) {
      throw new Error('El historial de solicitudes debe ser un arreglo JSON.');
    }

    return history;
  }
}

export function registerServiceRequests(app, store, sessions, { fcmTokensByAmbulance, sendPush }) {
  const idPattern = /^[A-Za-z0-9][A-Za-z0-9_-]{0,31}$/;
  const validateAmbulanceId = (ambulanceId, res) => {
    if (!idPattern.test(ambulanceId)) {
      res.status(400).json({
        error: 'ambulanceId debe tener entre 1 y 32 caracteres alfanuméricos, guiones o guiones bajos.',
      });
      return false;
    }

    return true;
  };
  const validatePathAmbulanceId = (req, res, next) => {
    if (validateAmbulanceId(req.params.ambulanceId, res)) {
      next();
    }
  };
  const requireSession = (req, res, next) => {
    res.set('Cache-Control', 'no-store');

    const bearer = /^Bearer\s+([a-f0-9]{64})$/i.exec(req.get('Authorization') ?? '');
    const session = bearer ? sessions.get(bearer[1]) : null;

    if (!session) {
      return res.status(401).json({ error: 'Token ausente o inválido.' });
    }

    res.locals.session = session;
    next();
  };

  registerTransportDestinations(app, store, requireSession);

  const ambulanceStatuses = new Map();

  app.post('/api/v1/status', requireSession, (req, res) => {
    const { status } = req.body ?? {};
    const ambulanceId = res.locals.session.ambulanceId;

    if (status !== 'EN_SERVICIO' && status !== 'FUERA_DE_SERVICIO') {
      return res.status(400).json({ error: 'Estado inválido. Debe ser EN_SERVICIO o FUERA_DE_SERVICIO.' });
    }

    ambulanceStatuses.set(ambulanceId, status);
    return res.status(200).json({ ambulanceId, status });
  });

  app.post('/api/v1/notifications/fcm-token', requireSession, (req, res) => {
    const token = req.body?.token;

    if (
      !req.body || Array.isArray(req.body) ||
      typeof token !== 'string' || token.length === 0 || token.length > 4096 ||
      /\s/.test(token)
    ) {
      return res.status(400).json({ error: 'Se requiere un token FCM válido.' });
    }

    fcmTokensByAmbulance.set(res.locals.session.ambulanceId, token);

    return res.status(204).end();
  });

  app.get('/internal/v1/service-requests/history', async (req, res) => {
    const { operator_id: operatorId, ambulance_id: ambulanceId } = req.query;

    if (
      typeof operatorId !== 'string' || !idPattern.test(operatorId) ||
      (ambulanceId !== undefined &&
        (typeof ambulanceId !== 'string' || !idPattern.test(ambulanceId)))
    ) {
      return res.status(400).json({
        error: 'Se requiere operator_id válido y, opcionalmente, ambulance_id válido.',
      });
    }

    const history = await store.list();
    const serviceRequests = history.filter(
      serviceRequest =>
        (ambulanceId === undefined || serviceRequest.ambulanceId === ambulanceId),
    );

    return res.json({ serviceRequests });
  });

  app.post(
    '/api/v1/dispatch/:id/respond',
    requireSession,
    async (req, res) => {
      const { accepted } = req.query;

      if (accepted !== 'true' && accepted !== 'false') {
        return res.status(400).json({ error: 'accepted debe ser true o false.' });
      }

      const result = await store.respond(req.params.id, {
        accepted: accepted === 'true',
        ...res.locals.session,
      });

      if (result.kind === 'not_found') {
        return res.status(404).json({ error: 'Solicitud de servicio inexistente.' });
      }

      if (result.kind === 'wrong_ambulance') {
        return res.status(403).json({ error: 'La solicitud no está asignada a esta ambulancia.' });
      }

      if (result.kind === 'already_responded') {
        return res.status(409).json({ error: 'La solicitud ya fue respondida.' });
      }

      return res.status(204).end();
    },
  );

  app.get('/api/v1/dispatch/:id', requireSession, async (req, res) => {
    const history = await store.list();
    const serviceRequest = history.find(request => request.id === req.params.id);

    if (!serviceRequest) {
      return res.status(404).json({ error: 'Solicitud de servicio inexistente.' });
    }

    if (serviceRequest.ambulanceId !== res.locals.session.ambulanceId) {
      return res.status(403).json({ error: 'La solicitud no está asignada a esta ambulancia.' });
    }

    return res.json(serviceRequest);
  });

  app.post('/api/v1/dispatch/finalize', requireSession, async (req, res) => {
    const ambulanceId = res.locals.session.ambulanceId;
    await store.finalizeActiveByAmbulance(ambulanceId);
    return res.status(200).json({ status: 'COMPLETED' });
  });

  app.post(
    '/internal/v1/ambulances/:ambulanceId/service-requests',
    validatePathAmbulanceId,
    async (req, res) => {
      const { ambulanceId } = req.params;
      const status = ambulanceStatuses.get(ambulanceId) ?? 'FUERA_DE_SERVICIO';

      if (status === 'FUERA_DE_SERVICIO') {
        return res.status(409).json({
          error: 'La ambulancia se encuentra fuera de servicio. Entrá en servicio en la app para recibir solicitudes.',
        });
      }

      const history = await store.list();
      const pendingRequest = history.find(
        request => request.ambulanceId === ambulanceId && request.status === 'PENDING'
      );
      if (pendingRequest) {
        return res.status(409).json({
          error: 'La ambulancia tiene una solicitud de emergencia pendiente de respuesta.',
        });
      }

      const activeEmergency = history.find(
        request => request.ambulanceId === ambulanceId && request.status === 'ACCEPTED'
      );
      if (activeEmergency) {
        return res.status(409).json({
          error: 'La ambulancia ya se encuentra atendiendo una emergencia en curso.',
        });
      }

      const deviceToken = fcmTokensByAmbulance.get(ambulanceId);

      if (!deviceToken) {
        return res.status(409).json({
          error: 'No hay una app registrada para esta ambulancia. Iniciá sesión en la app e intentá nuevamente.',
        });
      }

      if (typeof sendPush !== 'function') {
        return res.status(503).json({
          error: 'Firebase Admin no está configurado; no se creó la solicitud.',
        });
      }

      const template = serviceRequestTemplates[randomInt(serviceRequestTemplates.length)];
      const serviceRequest = {
        id: randomUUID(),
        ambulanceId,
        ...template,
        createdAt: new Date().toISOString(),
        status: 'PENDING',
      };

      await store.append(serviceRequest);

      try {
        const messageId = await sendPush({
          token: deviceToken,
          data: {
            dispatchId: serviceRequest.id,
            ambulanceId: serviceRequest.ambulanceId,
            title: `Nuevo llamado: ${serviceRequest.emergencyType}`,
            body: `${serviceRequest.address} — ${serviceRequest.description}`,
            address: serviceRequest.address,
            emergencyType: serviceRequest.emergencyType,
            description: serviceRequest.description,
            priority: serviceRequest.priority,
            latitude: String(serviceRequest.latitude),
            longitude: String(serviceRequest.longitude),
            createdAt: serviceRequest.createdAt,
          },
          android: { priority: 'high' },
          apns: { payload: { aps: { contentAvailable: true } } },
        });

        return res.status(201).json({ ...serviceRequest, notification: { messageId } });
      } catch (error) {
        if (
          error.code === 'messaging/registration-token-not-registered' ||
          error.code === 'messaging/invalid-registration-token'
        ) {
          fcmTokensByAmbulance.delete(ambulanceId);
        }

        console.error(`[Firebase] No se pudo enviar el despacho ${serviceRequest.id}:`, error.code ?? error.message);
        return res.status(502).json({
          error: 'La solicitud quedó guardada, pero Firebase no pudo enviar la notificación.',
          serviceRequestId: serviceRequest.id,
        });
      }
    },
  );
}
