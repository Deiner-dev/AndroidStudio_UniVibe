package com.gamevault.univibe

import android.content.Context
import android.media.AudioManager
import android.util.Base64
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import kotlinx.coroutines.*
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class llamadas(private val context: Context) {

    interface Listener {
        fun onConectado()
        fun onRemotoEntro()
        fun onRemotoSalio()
        fun onError(mensaje: String)
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val room: Room = LiveKit.create(context.applicationContext)
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun iniciar(urlLivekit: String, sala: String, identidad: String, listener: Listener) {
        scope.launch {
            try {
                val token = TokenLiveKit.generar(sala, identidad)

                launch {
                    room.events.collect { event ->
                        when (event) {
                            is RoomEvent.ParticipantConnected -> listener.onRemotoEntro()
                            is RoomEvent.ParticipantDisconnected -> listener.onRemotoSalio()
                            else -> {}
                        }
                    }
                }

                room.connect(urlLivekit, token)
                room.localParticipant.setMicrophoneEnabled(true)
                listener.onConectado()

                if (room.remoteParticipants.isNotEmpty()) listener.onRemotoEntro()
            } catch (e: Exception) {
                listener.onError(e.message ?: "Error al conectar la llamada")
            }
        }
    }

    private fun generarToken(sala: String, identidad: String): String {
        val ahora = System.currentTimeMillis() / 1000

        val header = JSONObject().put("alg", "HS256").put("typ", "JWT")
        val payload = JSONObject()
            .put("iss", BuildConfig.LIVEKIT_API_KEY)
            .put("sub", identidad)
            .put("name", identidad)
            .put("nbf", ahora - 10)
            .put("exp", ahora + 3600)
            .put("video", JSONObject()
                .put("room", sala)
                .put("roomJoin", true)
                .put("canPublish", true)
                .put("canSubscribe", true))

        fun b64(b: ByteArray) = Base64.encodeToString(
            b, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

        val datos = b64(header.toString().toByteArray()) + "." +
                b64(payload.toString().toByteArray())

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(BuildConfig.LIVEKIT_API_SECRET.toByteArray(), "HmacSHA256"))
        return datos + "." + b64(mac.doFinal(datos.toByteArray()))
    }

    fun silenciar(silenciado: Boolean) {
        scope.launch { room.localParticipant.setMicrophoneEnabled(!silenciado) }
    }

    @Suppress("DEPRECATION")
    fun altavoz(activo: Boolean) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = activo
    }

    fun finalizar() {
        room.disconnect()
        room.release()
        scope.cancel()
    }
}