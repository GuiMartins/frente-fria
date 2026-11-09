package com.frentefria.trava

import android.os.SystemClock
import com.frentefria.dados.Preferencias
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * PIN próprio do app (não o do celular, que ela pode saber), guardado como
 * SHA-256 com sal. A trava volta depois de 1 minuto com o app em segundo plano.
 */
class Trava(private val preferencias: Preferencias) {

    private val _travado = MutableStateFlow(true)
    val travado: StateFlow<Boolean> = _travado

    private var foiParaFundoEm: Long? = null

    suspend fun iniciar() {
        if (preferencias.pin() == null) _travado.value = false
    }

    suspend fun conferir(pin: String): Boolean {
        val (hash, sal) = preferencias.pin() ?: return true
        val ok = hash(pin, sal) == hash
        if (ok) _travado.value = false
        return ok
    }

    fun liberarPorBiometria() { _travado.value = false }

    suspend fun definir(pin: String) {
        val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }.let { Base64.getEncoder().encodeToString(it) }
        preferencias.salvarPin(hash(pin, sal), sal)
        _travado.value = false
    }

    suspend fun remover() {
        preferencias.removerPin()
        _travado.value = false
    }

    fun foiParaFundo() { foiParaFundoEm = SystemClock.elapsedRealtime() }

    suspend fun voltou() {
        val desde = foiParaFundoEm ?: return
        foiParaFundoEm = null
        if (preferencias.pin() != null && SystemClock.elapsedRealtime() - desde > TOLERANCIA_MS) _travado.value = true
    }

    private fun hash(pin: String, sal: String): String =
        MessageDigest.getInstance("SHA-256").digest("$sal:$pin".toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val TOLERANCIA_MS = 60_000L
    }
}
