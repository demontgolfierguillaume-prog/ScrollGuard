package com.scrollguard.data

import android.content.Context
import com.scrollguard.data.db.ScrollGuardDatabase

/**
 * Localisateur de services minimal, partagé entre l'app et le service
 * d'accessibilité. Volontairement sans framework d'injection à ce stade
 * (à réévaluer si le graphe grossit — voir memory/decisions.md).
 */
object ScrollGuardGraph {

    @Volatile
    private var initialized = false

    lateinit var database: ScrollGuardDatabase
        private set
    lateinit var signatureRepository: SignatureRepository
        private set
    lateinit var ruleRepository: RuleRepository
        private set

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appContext = context.applicationContext
            database = ScrollGuardDatabase.get(appContext)
            signatureRepository = SignatureRepository(appContext)
            ruleRepository = RuleRepository(database.ruleDao())
            initialized = true
        }
    }
}
