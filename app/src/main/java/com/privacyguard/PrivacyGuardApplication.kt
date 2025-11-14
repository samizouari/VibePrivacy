package com.privacyguard

import android.app.Application
// import dagger.hilt.android.HiltAndroidApp // TODO: Réactiver au Jour 2
import timber.log.Timber

// @HiltAndroidApp // TODO: Réactiver au Jour 2 quand on implémente DI
class PrivacyGuardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
