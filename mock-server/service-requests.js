import { randomInt, randomUUID } from 'node:crypto';
import { mkdir, readFile, rename, rm, writeFile } from 'node:fs/promises';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

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

export function registerServiceRequests(app, store, sessions) {
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
        serviceRequest.status !== 'PENDING' &&
        serviceRequest.respondedByOperatorId === operatorId &&
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

      return res.status(200).json({ status: result.serviceRequest.status });
    },
  );

  app.post(
    '/internal/v1/ambulances/:ambulanceId/service-requests',
    validatePathAmbulanceId,
    async (req, res) => {
      const template = serviceRequestTemplates[randomInt(serviceRequestTemplates.length)];
      const serviceRequest = {
        id: randomUUID(),
        ambulanceId: req.params.ambulanceId,
        ...template,
        createdAt: new Date().toISOString(),
        status: 'PENDING',
      };

      await store.append(serviceRequest);

      return res.status(201).json(serviceRequest);
    },
  );
}
