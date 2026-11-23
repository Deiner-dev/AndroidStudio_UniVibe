package com.gamevault.univibe

import android.content.Context
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.*

class videoLLamada(context: Context) {

    interface Listener {
        fun onConectado()
        fun onRemotoEntro()
        fun onRemotoSalio()
        fun onError(mensaje: String)
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val room: Room = LiveKit.create(context.applicationContext)

    private var rendererRemoto: TextureViewRenderer? = null
    private var rendererLocal: TextureViewRenderer? = null
    private var trackRemoto: VideoTrack? = null
    private var trackLocal: LocalVideoTrack? = null

    fun iniciar(
        urlLivekit: String,
        sala: String,
        identidad: String,
        remoto: TextureViewRenderer,
        local: TextureViewRenderer,
        listener: Listener
    ) {
        rendererRemoto = remoto
        rendererLocal = local

        scope.launch {
            try {
                room.initVideoRenderer(remoto)
                room.initVideoRenderer(local)

                val token = TokenLiveKit.generar(sala, identidad)

                launch {
                    room.events.collect { event ->
                        when (event) {
                            is RoomEvent.TrackSubscribed -> {
                                val t = event.track
                                if (t is VideoTrack) mostrarRemoto(t)
                            }
                            is RoomEvent.ParticipantConnected -> listener.onRemotoEntro()
                            is RoomEvent.ParticipantDisconnected -> listener.onRemotoSalio()
                            else -> {}
                        }
                    }
                }

                room.connect(urlLivekit, token)
                room.localParticipant.setMicrophoneEnabled(true)
                room.localParticipant.setCameraEnabled(true)

                // Mi propia cámara en la vista pequeña
                val pub = room.localParticipant.getTrackPublication(Track.Source.CAMERA)
                (pub?.track as? LocalVideoTrack)?.let {
                    trackLocal = it
                    it.addRenderer(local)
                }

                listener.onConectado()

                // Si el otro ya estaba en la sala cuando entré
                for (p in room.remoteParticipants.values) {
                    listener.onRemotoEntro()
                    val v = p.getTrackPublication(Track.Source.CAMERA)?.track as? VideoTrack
                    if (v != null) mostrarRemoto(v)
                }
            } catch (e: Exception) {
                listener.onError(e.message ?: "Error al conectar la videollamada")
            }
        }
    }

    private fun mostrarRemoto(track: VideoTrack) {
        trackRemoto?.let { old -> rendererRemoto?.let { old.removeRenderer(it) } }
        trackRemoto = track
        rendererRemoto?.let { track.addRenderer(it) }
    }

    fun silenciar(silenciado: Boolean) {
        scope.launch { room.localParticipant.setMicrophoneEnabled(!silenciado) }
    }

    fun camaraActiva(activa: Boolean) {
        scope.launch { room.localParticipant.setCameraEnabled(activa) }
    }

    fun cambiarCamara() {
        trackLocal?.switchCamera()
    }

    fun finalizar() {
        try {
            rendererRemoto?.let { trackRemoto?.removeRenderer(it) }
            rendererLocal?.let { trackLocal?.removeRenderer(it) }
        } catch (_: Exception) {}
        room.disconnect()
        room.release()
        scope.cancel()
    }
}