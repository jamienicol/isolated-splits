package com.example.splitapk1

import android.app.Application
import android.util.Log


class MyApplication : Application() {
    companion object {
        private const val LOGTAG = "MyApplication"
    }

    private lateinit var mainProcStuff: MainProcStuff

    override fun onCreate() {
        super.onCreate()

        Log.d(LOGTAG, "MyApplication.onCreate() ${Application.getProcessName()}")
        mainProcStuff = MainProcStuff()
    }
}
