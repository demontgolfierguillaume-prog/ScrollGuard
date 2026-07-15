package com.scrollguard.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding

/**
 * Écran de blocage PLEIN ÉCRAN posé par-dessus la fonctionnalité restreinte.
 *
 * - Couvre tout l'écran (y compris derrière les barres système) : aucun geste
 *   ne peut atteindre l'app en dessous.
 * - Prend le focus audio à l'affichage : les lecteurs vidéo (Reels, Shorts,
 *   TikTok…) mettent la lecture en pause derrière l'overlay — sans cela, le
 *   son et la vidéo continueraient.
 * - Deux sorties : « Continuer sur l'appli » (action retour, on reste dans
 *   l'app hôte) et « Ouvrir ScrollGuard ».
 *
 * Utilise TYPE_ACCESSIBILITY_OVERLAY : aucun besoin de SYSTEM_ALERT_WINDOW
 * tant que la fenêtre est posée par le service d'accessibilité. Toutes les
 * méthodes doivent être appelées sur le thread principal.
 */
class BlockOverlay(private val service: AccessibilityService) {

    private val windowManager: WindowManager =
        service.getSystemService(WindowManager::class.java)
    private val audioManager: AudioManager =
        service.getSystemService(AudioManager::class.java)

    private var view: LinearLayout? = null
    private var focusRequest: AudioFocusRequest? = null

    val isShowing: Boolean get() = view != null

    fun show(
        featureLabel: String,
        reasonLabel: String,
        onLeave: () -> Unit,
        onOpenScrollGuard: () -> Unit,
    ) {
        if (view != null) return
        pauseMediaBehind()

        val container = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F2141816"))
            setPadding(dp(32))
            isClickable = true
            isFocusable = false
        }

        container.addView(TextView(service).apply {
            text = service.getString(R.string.block_overlay_title)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
        })
        container.addView(TextView(service).apply {
            text = "$featureLabel — $reasonLabel"
            setTextColor(Color.parseColor("#B0BDB5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(0, dp(12), 0, dp(8))
        })
        container.addView(TextView(service).apply {
            text = service.getString(R.string.block_overlay_hint)
            setTextColor(Color.parseColor("#7A867F"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, dp(32))
        })
        container.addView(Button(service).apply {
            text = service.getString(R.string.block_overlay_button_continue)
            setOnClickListener { onLeave() }
        })
        container.addView(Button(service).apply {
            text = service.getString(R.string.block_overlay_button_open_app)
            setOnClickListener { onOpenScrollGuard() }
        })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )

        windowManager.addView(container, params)
        view = container
    }

    fun hide() {
        view?.let { windowManager.removeView(it) }
        view = null
        releaseMediaFocus()
    }

    /**
     * Prend le focus audio exclusif : le lecteur de l'app derrière l'overlay
     * reçoit une perte de focus et met sa lecture en pause (comportement
     * standard des lecteurs Instagram/YouTube/TikTok).
     */
    private fun pauseMediaBehind() {
        if (focusRequest != null) return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build(),
            )
            .build()
        audioManager.requestAudioFocus(request)
        focusRequest = request
    }

    private fun releaseMediaFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun dp(value: Int): Int =
        (value * service.resources.displayMetrics.density).toInt()
}
