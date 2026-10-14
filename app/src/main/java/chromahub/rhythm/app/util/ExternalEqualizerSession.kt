/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.SystemClock
import android.util.Log

/**
 * Avisa a los ecualizadores externos (Poweramp Equalizer, Wavelet, el ecualizador del sistema...) de la sesion de
 * audio que usa Rhythm, con los avisos estandar de Android OPEN/CLOSE_AUDIO_EFFECT_CONTROL_SESSION. Sin este aviso
 * esas apps no saben a que sesion aplicar su ecualizacion.
 */
object ExternalEqualizerSession {
    private const val TAG = "ExternalEqSession"
    private const val MIN_RESEND_MS = 1500L

    private var openedSessionId = 0
    private var lastSentAt = 0L

    @Synchronized
    fun open(context: Context, audioSessionId: Int) {
        if (audioSessionId <= 0) return
        val app = context.applicationContext
        if (openedSessionId != 0 && openedSessionId != audioSessionId) {
            send(app, AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION, openedSessionId)
        } else if (openedSessionId == audioSessionId && SystemClock.elapsedRealtime() - lastSentAt < MIN_RESEND_MS) {
            return
        }
        send(app, AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION, audioSessionId)
        openedSessionId = audioSessionId
        lastSentAt = SystemClock.elapsedRealtime()
    }

    @Synchronized
    fun close(context: Context) {
        val id = openedSessionId
        if (id == 0) return
        send(context.applicationContext, AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION, id)
        openedSessionId = 0
    }

    private fun send(context: Context, action: String, audioSessionId: Int) {
        try {
            val intent = Intent(action).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            context.sendBroadcast(intent)
            Log.d(TAG, "$action session=$audioSessionId")
        } catch (e: Exception) {
            Log.w(TAG, "Could not send $action", e)
        }
    }
}
