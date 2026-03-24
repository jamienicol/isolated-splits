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

/**
 * Manages replacement classloaders for splits that include ABI config split
 * native library paths. With isolatedSplits=true, the framework does not
 * include ABI config splits in classloaders' native library search paths.
 *
 * For the base module, the replacement is created in instantiateClassLoader.
 * For feature modules, replacements are created on demand and used in
 * instantiateActivity/Service/etc.
 */
object SplitClassLoaderManager {
    private const val LOGTAG = "SplitCLManager"

    private var appInfo: ApplicationInfo? = null
    private val replacements = mutableMapOf<ClassLoader, ClassLoader>()

    fun init(aInfo: ApplicationInfo) {
        appInfo = aInfo
    }

    /**
     * Get or create a replacement classloader for a feature split classloader.
     * The replacement is a sibling (not child) of the original — same dex path,
     * same parent, but with the feature's ABI config split in the native lib path.
     * This ensures classes are defined by the replacement, so System.loadLibrary
     * uses its namespace.
     */
    fun getReplacementClassLoader(cl: ClassLoader): ClassLoader {
        replacements[cl]?.let { return it }

        val aInfo = appInfo
        if (aInfo == null) {
            Log.d(LOGTAG, "appInfo not initialized yet")
            return cl
        }
        val clStr = cl.toString()

        // Don't replace the base classloader here (handled in instantiateClassLoader)
        if (clStr.contains("base.apk")) return cl

        val splitSourceDirs = aInfo.splitSourceDirs
        if (splitSourceDirs == null) {
            Log.d(LOGTAG, "splitSourceDirs is null")
            return cl
        }

        Log.d(LOGTAG, "splitSourceDirs: ${splitSourceDirs.toList()}")

        // Find which feature split APK this classloader is for
        val featureApk = splitSourceDirs.find { dir ->
            val name = dir.substringAfterLast("/")
            name.startsWith("split_") && !name.contains("config") && clStr.contains(name)
        }
        if (featureApk == null) {
            Log.d(LOGTAG, "No feature APK found matching classloader: $clStr")
            return cl
        }

        // Derive feature name: split_feature_browser.apk -> feature_browser
        val featureName = featureApk.substringAfterLast("/")
            .removePrefix("split_")
            .removeSuffix(".apk")

        // Find the ABI config split for this feature
        val abiSplit = splitSourceDirs.find {
            it.contains(featureName) && it.contains("arm64_v8a")
        } ?: splitSourceDirs.find {
            it.contains(featureName) && it.contains("armeabi_v7a")
        } ?: splitSourceDirs.find {
            it.contains(featureName) && it.contains("x86_64")
        } ?: splitSourceDirs.find {
            it.contains(featureName) && it.contains("x86")
        }

        if (abiSplit == null) {
            Log.d(LOGTAG, "No ABI config split found for $featureName")
            return cl
        }

        val abiLibDir = when {
            abiSplit.contains("arm64_v8a") -> "arm64-v8a"
            abiSplit.contains("armeabi_v7a") -> "armeabi-v7a"
            abiSplit.contains("x86_64") -> "x86_64"
            abiSplit.contains("x86") -> "x86"
            else -> return cl
        }

        val nativeLibPath = "$abiSplit!/lib/$abiLibDir"

        Log.d(LOGTAG, "Creating replacement for $featureName: dexPath=$featureApk, nativeLibPath=$nativeLibPath")

        // Sibling replacement: same parent as original, classes defined by us
        val replacement = PathClassLoader(featureApk, nativeLibPath, cl.parent)
        replacements[cl] = replacement
        return replacement
    }
}

class MyAppComponentFactory: AppComponentFactory() {
    companion object {
        private const val LOGTAG = "MyAppComponentFactory"

        private fun findBaseAbiConfigSplit(aInfo: ApplicationInfo): Pair<String, String>? {
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

    override fun instantiateClassLoader(cl: ClassLoader, aInfo: ApplicationInfo): ClassLoader {
        Log.d(LOGTAG, "instantiateClassLoader() cl=$cl")

        SplitClassLoaderManager.init(aInfo)

        val (abiSplit, abiLibDir) = findBaseAbiConfigSplit(aInfo) ?: return super.instantiateClassLoader(cl, aInfo)

        if (!cl.toString().contains("base.apk")) {
            return super.instantiateClassLoader(cl, aInfo)
        }

        val librarySearchPath = listOfNotNull(
            aInfo.nativeLibraryDir,
            "${aInfo.sourceDir}!/lib/$abiLibDir",
            "$abiSplit!/lib/$abiLibDir"
        ).joinToString(":")

        Log.d(LOGTAG, "Creating replacement base classloader: dexPath=${aInfo.sourceDir}, librarySearchPath=$librarySearchPath")

        return PathClassLoader(aInfo.sourceDir, librarySearchPath, cl.parent)
    }

    override fun instantiateApplication(cl: ClassLoader, className: String): Application {
        Log.d(LOGTAG, "instantiateApplication() $className: $cl")
        return super.instantiateApplication(SplitClassLoaderManager.getReplacementClassLoader(cl), className)
    }

    override fun instantiateActivity(
        cl: ClassLoader,
        className: String,
        intent: Intent?
    ): Activity {
        Log.d(LOGTAG, "instantiateActivity() $className: $cl")
        return super.instantiateActivity(SplitClassLoaderManager.getReplacementClassLoader(cl), className, intent)
    }

    override fun instantiateReceiver(
        cl: ClassLoader,
        className: String,
        intent: Intent?
    ): BroadcastReceiver {
        Log.d(LOGTAG, "instantiateReceiver() $className")
        return super.instantiateReceiver(SplitClassLoaderManager.getReplacementClassLoader(cl), className, intent)
    }

    override fun instantiateService(cl: ClassLoader, className: String, intent: Intent?): Service {
        Log.d(LOGTAG, "instantiateService() $className")
        return super.instantiateService(SplitClassLoaderManager.getReplacementClassLoader(cl), className, intent)
    }

    override fun instantiateProvider(cl: ClassLoader, className: String): ContentProvider {
        Log.d(LOGTAG, "instantiateProvider() $className")
        return super.instantiateProvider(SplitClassLoaderManager.getReplacementClassLoader(cl), className)
    }
}
