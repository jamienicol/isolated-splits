package com.example.splitapk1.feature_browser

import android.app.Application
import android.util.Log
import com.example.splitapk1.MyApplication
import mozilla.components.support.remotesettings.RemoteSettingsServer
import mozilla.components.support.remotesettings.RemoteSettingsService
import mozilla.components.support.remotesettings.into

class MainProcStuff(application: Application) {
    companion object {
        private const val LOGTAG = "MainProcStuff"
    }

    private val remoteSettingsService =
        RemoteSettingsService(
            application,
            RemoteSettingsServer.Prod.into(),
            channel = "prototype",
        )

    init {
        Log.d(LOGTAG, "MainProcStuff constructed ${Application.getProcessName()}")
        Log.d(LOGTAG, "MainProcStuff application class loader: ${application.classLoader}")
        Log.d(LOGTAG, "MainProcStuff::class.java class loader: ${MainProcStuff::class.java.classLoader}")
        Log.d(
            LOGTAG,
            "RemoteSettingsService initialized ${remoteSettingsService.remoteSettingsService}",
        )
    }
}
