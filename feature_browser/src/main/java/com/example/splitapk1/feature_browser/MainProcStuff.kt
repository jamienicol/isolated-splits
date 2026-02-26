package com.example.splitapk1.feature_browser

import android.app.Application
import android.util.Log

class MainProcStuff {
    companion object {
        private const val LOGTAG = "MainProcStuff"
    }

    init {
        Log.d(LOGTAG, "MainProcStuff constructed ${Application.getProcessName()}")
    }
}
