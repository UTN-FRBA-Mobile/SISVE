package com.utn.sisve.repository

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import com.utn.sisve.data.remote.ApiService
import com.utn.sisve.data.local.Entity.LoginAccessEntity
import com.utn.sisve.data.local.db.LoginAccessDao
import com.utn.sisve.domain.model.SessionData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class LoginPhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accessDao: LoginAccessDao,
    private val apiService: ApiService
) {
    private val callbackExecutor = Dispatchers.IO.asExecutor()

    suspend fun captureAndSave(imageCapture: ImageCapture, session: SessionData): LoginAccessEntity {
        return withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "login_photos")

            if (!directory.isDirectory && !directory.mkdirs()) {
                throw IOException("No se pudo crear el directorio privado de fotos.")
            }

            val photo = File.createTempFile("access_", ".jpg", directory)
            val capturedAt = System.currentTimeMillis()
            var committed = false

            try {
                withTimeout(CAPTURE_TIMEOUT_MILLIS) {
                    withContext(Dispatchers.Main.immediate) {
                        takePhoto(imageCapture, photo)
                    }
                }

                if (!photo.isFile || photo.length() == 0L) {
                    throw IOException("La foto no se guardó correctamente.")
                }

                // Una vez escrita la foto, completar el registro sin dejar una fila sin imagen
                // por una cancelación justo después de que Room confirme el INSERT.
                val access = LoginAccessEntity(
                    id = UUID.randomUUID().toString(),
                    ambulanceId = session.ambulanceId,
                    operatorId = session.operatorId,
                    operatorName = session.operatorName,
                    capturedAt = capturedAt,
                    photoFileName = photo.name
                )

                withContext(NonCancellable) {
                    accessDao.insert(access)
                    committed = true
                }

                access
            } finally {
                if (!committed) {
                    photo.delete()
                }
            }
        }
    }

    // Distinguir fallos de transporte de los errores de archivo o de Room.
    class UploadException(cause: IOException) : IOException("No se pudo enviar la foto.", cause)

    suspend fun upload(access: LoginAccessEntity) {
        withContext(Dispatchers.IO) {
            val photo = File(File(context.filesDir, "login_photos"), access.photoFileName)

            if (!photo.isFile || photo.length() == 0L) {
                throw IOException("La foto local no está disponible.")
            }

            val textType = "text/plain".toMediaType()
            val part = MultipartBody.Part.createFormData(
                "photo",
                photo.name,
                photo.asRequestBody("image/jpeg".toMediaType())
            )

            val response = try {
                apiService.uploadLoginPhoto(
                    access.id.toRequestBody(textType),
                    access.capturedAt.toString().toRequestBody(textType),
                    part
                )
            } catch (error: IOException) {
                throw UploadException(error)
            }

            if (!response.isSuccessful) {
                throw HttpException(response)
            }

            accessDao.markUploaded(access.id)
        }
    }

    private suspend fun takePhoto(imageCapture: ImageCapture, photo: File) {
        suspendCancellableCoroutine { continuation ->
            val options = ImageCapture.OutputFileOptions.Builder(photo).build()

            imageCapture.takePicture(
                options,
                callbackExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        } else {
                            // CameraX puede terminar después de cancelar o abandonar la pantalla.
                            photo.delete()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        photo.delete()

                        if (continuation.isActive) {
                            continuation.resumeWithException(exception)
                        }
                    }
                }
            )
        }
    }

    companion object {
        private const val CAPTURE_TIMEOUT_MILLIS = 10_000L
    }
}
