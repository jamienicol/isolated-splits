package com.example.splitapk1

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class CrashHelper : Service() {
    companion object {
        private const val LOGTAG = "CrashHelper"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOGTAG, "CrashHelper.onStartCommand()")
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}