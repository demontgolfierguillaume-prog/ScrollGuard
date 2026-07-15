package com.scrollguard.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding

/**
 * Écran de blocage plein écran affiché par-dessus la fonctionnalité restreinte.
 *
 * Utilise TYPE_ACCESSIBILITY_OVERLAY : aucun besoin de SYSTEM_ALERT_WINDOW tant
 * que la fenêtre est posée par le service d'accessibilité. Toutes les méthodes
 * doivent être appelées sur le thread principal.
 */
class BlockOverlay(private val service: AccessibilityService) {

    private val windowManager: WindowManager =
        service.getSystemService(WindowManager::class.java)

    private var view: LinearLayout? = null

    val isShowing: Boolean get() = view != null

    fun show(featureLabel: String, reasonLabel: String, onLeave: () -> Unit) {
        if (view != null) return

        val container = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F2141816"))
            setPadding(dp(32))
            // Consomme tous les touchers : la fonctionnalité derrière l'overlay
            // devient inutilisable, sans fermer l'application hôte.
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
            text = service.getString(R.string.block_overlay_button_back)
            setOnClickListener { onLeave() }
        })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        )

        windowManager.addView(container, params)
        view = container
    }

    fun hide() {
        view?.let { windowManager.removeView(it) }
        view = null
    }

    private fun dp(value: Int): Int =
        (value * service.resources.displayMetrics.density).toInt()
}
