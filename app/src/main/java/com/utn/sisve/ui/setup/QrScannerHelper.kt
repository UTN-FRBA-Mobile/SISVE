package com.utn.sisve.ui.setup

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

object QrScannerHelper {

    fun scanQrCode(
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(context, options)

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (!rawValue.isNullOrBlank()) {
                    onSuccess(rawValue)
                } else {
                    onError("El código QR está vacío o no se pudo leer.")
                }
            }
            .addOnCanceledListener {
                // El usuario cerró la cámara con el botón atrás; no hace falta mostrar error
            }
            .addOnFailureListener { exception ->
                onError("No se pudo iniciar el escáner: ${exception.localizedMessage ?: "Error desconocido"}")
            }
    }
}