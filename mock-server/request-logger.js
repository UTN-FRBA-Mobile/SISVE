import { randomUUID } from 'node:crypto';
import { performance } from 'node:perf_hooks';

function redactSensitiveFields(value) {
  if (Array.isArray(value)) {
    return value.map(redactSensitiveFields);
  }

  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value).map(([key, fieldValue]) => [
        key,
        /^(pin|token|fcmToken)$/i.test(key) ? '[REDACTED]' : redactSensitiveFields(fieldValue),
      ]),
    );
  }

  return value;
}

export function requestLogger(req, res, next) {
  const requestId = randomUUID();
  const started = performance.now();
  const request = { requestId, method: req.method, path: req.path };
  let responseBody;

  console.log(JSON.stringify({
    timestamp: new Date().toISOString(),
    event: 'request',
    ...request,
  }));

  const originalJson = res.json;

  res.json = function (body) {
    responseBody = body;

    return originalJson.call(this, body);
  };

  res.on('finish', () => {
    console.log(JSON.stringify({
      timestamp: new Date().toISOString(),
      event: 'response',
      ...request,
      status: res.statusCode,
      durationMs: Math.round((performance.now() - started) * 100) / 100,

      // Se lee después del parser; nunca se registra el JSON crudo ni los headers.
      requestBody: redactSensitiveFields(req.body),
      responseBody: redactSensitiveFields(responseBody),
    }));
  });

  next();
}
