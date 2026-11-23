package com.gamevault.univibe

import android.util.Base64
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TokenLiveKit {

    fun generar(sala: String, identidad: String): String {
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
}