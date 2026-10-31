import { randomUUID } from 'node:crypto';
import { performance } from 'node:perf_hooks';

const sensitiveFields = new Set([
  'pin', 'token', 'accesstoken', 'refreshtoken', 'password',
  'authorization', 'cookie', 'set-cookie',
]);

function redact(value) {
  if (Array.isArray(value)) {
    return value.map(redact);
  }

  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([key, field]) => [
      key,
      sensitiveFields.has(key.toLowerCase().replaceAll('_', '')) ? '[REDACTED]' : redact(field),
    ]));
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
    responseBody = redact(body);

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
      requestBody: redact(req.body),
      responseBody,
    }));
  });

  next();
}
