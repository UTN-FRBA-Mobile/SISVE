import { randomBytes } from 'node:crypto';
import express from 'express';
import { demoUsers } from './demo-users.js';
import { requestLogger } from './request-logger.js';

export function createApp({ users = demoUsers } = {}) {
  const usersByAmbulance = new Map();

  for (const user of users) {
    if (typeof user.ambulanceId !== 'string' || !user.ambulanceId.trim()) {
      throw new Error('ambulanceId debe ser un texto no vacío.');
    }

    if (typeof user.pin !== 'string' || !/^[0-9]{4,6}$/.test(user.pin)) {
      throw new Error('El PIN debe contener entre 4 y 6 dígitos.');
    }

    if (
      typeof user.operator?.id !== 'string' || !user.operator.id.trim() ||
      typeof user.operator?.name !== 'string' || !user.operator.name.trim()
    ) {
      throw new Error('El operador debe tener id y nombre.');
    }

    if (!usersByAmbulance.has(user.ambulanceId)) {
      usersByAmbulance.set(user.ambulanceId, new Map());
    }

    const operatorsByPin = usersByAmbulance.get(user.ambulanceId);

    if (operatorsByPin.has(user.pin)) {
      throw new Error('El PIN debe ser único dentro de cada ambulancia.');
    }

    operatorsByPin.set(user.pin, {
      id: user.operator.id,
      name: user.operator.name,
    });
  }

  const sessions = new Map();
  const app = express();

  app.disable('x-powered-by');
  app.use(requestLogger);
  app.use(express.json({ limit: '4kb' }));

  app.post('/api/v1/auth/login', (req, res) => {
    res.set('Cache-Control', 'no-store');

    const body = req.body;

    if (
      !body || Array.isArray(body) ||
      typeof body.ambulanceId !== 'string' || !body.ambulanceId.trim() ||
      typeof body.pin !== 'string' || !/^[0-9]{4,6}$/.test(body.pin)
    ) {
      return res.status(400).json({
        error: 'Se requiere ambulanceId y un PIN de entre 4 y 6 dígitos enviado como texto.',
      });
    }

    const operator = usersByAmbulance.get(body.ambulanceId)?.get(body.pin);

    if (!operator) {
      return res.status(401).json({ error: 'Credenciales inválidas.' });
    }

    const token = randomBytes(32).toString('hex');

    sessions.set(token, {
      ambulanceId: body.ambulanceId,
      operatorId: operator.id,
    });

    return res.json({
      token,
      ambulanceId: body.ambulanceId,
      operator,
    });
  });

  app.use((req, res) => {
    res.status(404).json({ error: 'Endpoint no Implementado.' });
  });

  app.use((error, req, res, next) => {
    res.set('Cache-Control', 'no-store');

    if (error.type === 'entity.parse.failed') {
      return res.status(400).json({ error: 'JSON inválido.' });
    }

    if (error.type === 'entity.too.large') {
      return res.status(413).json({ error: 'Solicitud demasiado grande.' });
    }

    return res.status(500).json({ error: 'Error interno del mock.' });
  });

  return app;
}
