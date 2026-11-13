import { constants, mkdirSync } from 'node:fs';
import { copyFile, rm } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';
import multer from 'multer';

export function registerLoginPhotos(app, sessions) {
  const directory = fileURLToPath(new URL('./uploads/', import.meta.url));
  mkdirSync(directory, { recursive: true });
  const route = '/api/v1/access/photo';
  const upload = multer({
    dest: directory,
    limits: { fileSize: 10 * 1024 * 1024, files: 1, fields: 2, parts: 3, fieldSize: 128 },
    fileFilter: (req, file, callback) => callback(null, file.mimetype === 'image/jpeg'),
  });

  console.log(`[${new Date().toISOString()}] Fotos de acceso del mock: ${directory}`);

  app.post(
    route,
    (req, res, next) => {
      res.set('Cache-Control', 'no-store');

      const bearer = /^Bearer (\S+)$/i.exec(req.get('Authorization') ?? '');
      const session = bearer ? sessions.get(bearer[1]) : null;

      if (!session) {
        return res.status(401).json({ error: 'Token ausente o inválido.' });
      }

      res.locals.session = session;
      next();
    },
    upload.single('photo'),
    async (req, res) => {
      let status;
      let response;

      try {
        const { accessId, capturedAt } = req.body ?? {};
        const timestamp = Number(capturedAt);

        if (
          !req.file || typeof accessId !== 'string' ||
          !/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(accessId) ||
          typeof capturedAt !== 'string' || !/^[0-9]+$/.test(capturedAt) ||
          !Number.isSafeInteger(timestamp) || timestamp <= 0 ||
          !Number.isFinite(new Date(timestamp).getTime())
        ) {
          status = 400;
          response = { error: 'Se requiere photo JPEG, accessId UUID y capturedAt en milisegundos Unix.' };
        } else {
          // La identidad sale del token, no de campos que pueda enviar el cliente.
          const { ambulanceId, operatorId } = res.locals.session;
          const safe = value => value.replace(/[^a-zA-Z0-9_-]/g, '_');
          const date = new Date(timestamp).toISOString().replace(/[:.]/g, '-');
          const fileName = `${date}_${safe(ambulanceId)}_${safe(operatorId)}_${accessId}.jpg`;

          status = 201;

          try {
            // Un reintento del mismo acceso no crea otra copia ni sobrescribe la anterior.
            await copyFile(req.file.path, join(directory, fileName), constants.COPYFILE_EXCL);
          } catch (error) {
            if (error.code !== 'EEXIST') {
              throw error;
            }

            status = 200;
          }

          response = { fileName };
        }
      } finally {
        if (req.file) {
          await rm(req.file.path, { force: true });
        }
      }

      return res.status(status).json(response);
    },
  );

  app.use(route, (error, req, res, next) => {
    // Multer elimina sus archivos parciales cuando rechaza una solicitud.
    if (error instanceof multer.MulterError) {
      const tooLarge = error.code === 'LIMIT_FILE_SIZE';

      return res.status(tooLarge ? 413 : 400).json({
        error: tooLarge ? 'La foto supera los 10 MB.' : 'Solicitud multipart inválida.',
      });
    }

    next(error);
  });
}
