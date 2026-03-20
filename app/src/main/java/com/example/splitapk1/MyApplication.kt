package com.example.splitapk1

import android.app.Application
import android.util.Log


class MyApplication : Application() {
    companion object {
        private const val LOGTAG = "MyApplication"
    }

    private var mainProcStuff: Any? = null

    fun isMainProcess(): Boolean {
        return !getProcessName().contains(":")
    }

    override fun onCreate() {
        super.onCreate()

        System.setProperty("jna.debug_load", "true");
        System.setProperty("jna.debug_load.jna", "true");

        Log.d(LOGTAG, "MyApplication.onCreate() ${getProcessName()}")

        Log.d(LOGTAG, "classLoader: $classLoader")
        Log.d(LOGTAG, "classLoader parent: ${classLoader.parent}")

        Log.d(LOGTAG, "splitNames: ${applicationInfo.splitNames?.map { i -> i.toString() }}")


        if (isMainProcess()) {
            // Main process only — load feature split and init MainProcStuff
            val splitContext = createContextForSplit("feature_browser")
            val clazz = splitContext.classLoader.loadClass(
                "com.example.splitapk1.feature_browser.MainProcStuff"
            )
            mainProcStuff = clazz.getDeclaredConstructor(Application::class.java).newInstance(this)
        }
    }
}
