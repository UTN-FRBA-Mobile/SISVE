import { createApp } from './app.js';

const host = process.env.HOST ?? '127.0.0.1';
const port = Number(process.env.PORT ?? 8080);

if (!Number.isInteger(port) || port < 1 || port > 65535) {
  throw new Error('PORT debe ser un entero entre 1 y 65535.');
}

const app = createApp();

const server = app.listen(port, host, () => {
  console.log(`Mock SISVE disponible en http://${host}:${port} (solo desarrollo/demo)`);
});

server.on('error', (error) => {
  console.error(`No se pudo iniciar el mock: ${error.code ?? 'error desconocido'}`);

  process.exitCode = 1;
});
