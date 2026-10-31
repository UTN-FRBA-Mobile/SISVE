package com.utn.sisve.session

// Reloj monotónico: no depende de la fecha/hora y cuenta el tiempo en suspensión.
fun interface SessionClock {
    fun nowMillis(): Long
}
