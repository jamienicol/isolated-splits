package com.example.splitapk1

import android.app.Activity
import android.app.AppComponentFactory
import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentProvider
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import dalvik.system.PathClassLoader

class MyAppComponentFactory: AppComponentFactory() {
    companion object {
        private const val LOGTAG = "MyAppComponentFactory"

        private fun findAbiConfigSplit(aInfo: ApplicationInfo): Pair<String, String>? {
            val splitSourceDirs = aInfo.splitSourceDirs ?: return null
            val abiSplit = splitSourceDirs.find { it.contains("split_config.arm64_v8a") }
                ?: splitSourceDirs.find { it.contains("split_config.armeabi_v7a") }
                ?: splitSourceDirs.find { it.contains("split_config.x86_64") }
                ?: splitSourceDirs.find { it.contains("split_config.x86") }
                ?: return null

            val abiLibDir = when {
                abiSplit.contains("arm64_v8a") -> "arm64-v8a"
                abiSplit.contains("armeabi_v7a") -> "armeabi-v7a"
                abiSplit.contains("x86_64") -> "x86_64"
                abiSplit.contains("x86") -> "x86"
                else -> return null
            }

            return Pair(abiSplit, abiLibDir)
        }
    }

    /**
     * With isolatedSplits=true, the framework creates classloaders for each split
     * but does not include ABI config split native library paths (e.g.
     * split_config.arm64_v8a.apk!/lib/arm64-v8a) in the base module's classloader.
     *
     * The framework also creates the native linker namespace (with library_path)
     * BEFORE calling instantiateClassLoader, so a child classloader doesn't help —
     * classes defined by the parent still use the parent's namespace.
     *
     * Instead, we create a REPLACEMENT classloader with the same DEX path and
     * parent as the original, but with the ABI config split added to the native
     * library search path. Since classes are now defined by our replacement, the
     * native linker creates a new namespace with the correct library paths when
     * the first System.loadLibrary call happens.
     */
    override fun instantiateClassLoader(cl: ClassLoader, aInfo: ApplicationInfo): ClassLoader {
        Log.d(LOGTAG, "instantiateClassLoader() cl=$cl")

        val (abiSplit, abiLibDir) = findAbiConfigSplit(aInfo) ?: return super.instantiateClassLoader(cl, aInfo)

        // Only replace the base module's classloader (the one with base.apk)
        if (!cl.toString().contains("base.apk")) {
            return super.instantiateClassLoader(cl, aInfo)
        }

        // Build library search path: original paths + ABI config split
        val librarySearchPath = listOfNotNull(
            aInfo.nativeLibraryDir,
            "${aInfo.sourceDir}!/lib/$abiLibDir",
            "$abiSplit!/lib/$abiLibDir"
        ).joinToString(":")

        Log.d(LOGTAG, "Creating replacement classloader: dexPath=${aInfo.sourceDir}, librarySearchPath=$librarySearchPath")

        // Create a new classloader that replaces (not wraps) the original.
        // Using cl.parent so classes are defined by our classloader, causing
        // the native linker to create a fresh namespace with our library paths.
        return PathClassLoader(aInfo.sourceDir, librarySearchPath, cl.parent)
    }

    override fun instantiateApplication(cl: ClassLoader, className: String): Application {
        Log.d(LOGTAG, "instantiateApplication() $className: $cl")
        return super.instantiateApplication(cl, className)
    }

    override fun instantiateActivity(
        cl: ClassLoader,
        className: String,
        intent: Intent?
    ): Activity {
        Log.d(LOGTAG, "instantiateActivity() $className: $cl")
        return super.instantiateActivity(cl, className, intent)
    }

    override fun instantiateReceiver(
        cl: ClassLoader,
        className: String,
        intent: Intent?
    ): BroadcastReceiver {
        Log.d(LOGTAG, "instantiateReceiver() $className")
        return super.instantiateReceiver(cl, className, intent)
    }

    override fun instantiateService(cl: ClassLoader, className: String, intent: Intent?): Service {
        Log.d(LOGTAG, "instantiateService() $className")
        return super.instantiateService(cl, className, intent)
    }

    override fun instantiateProvider(cl: ClassLoader, className: String): ContentProvider {
        Log.d(LOGTAG, "instantiateProvider() $className")
        return super.instantiateProvider(cl, className)
    }
}
