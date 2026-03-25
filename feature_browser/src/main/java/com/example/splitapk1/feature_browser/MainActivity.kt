package com.example.splitapk1.feature_browser

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.splitapk1.CrashHelper
import com.example.splitapk1.feature_browser.ui.theme.SplitApk1Theme
import mozilla.components.support.remotesettings.RemoteSettingsServer
import mozilla.components.support.remotesettings.RemoteSettingsService
import mozilla.components.support.remotesettings.into

class MainActivity : ComponentActivity() {
    companion object {
        private const val LOGTAG = "MainActivity"
    }

    private lateinit var remoteSettingsService: RemoteSettingsService;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(LOGTAG, "MainActivity.onCreate()")
        Log.d(LOGTAG, "MainActivity application class loader: $classLoader")
        Log.d(LOGTAG, "MainActivity::class.java class loader: ${MainActivity::class.java.classLoader}")

        startService(Intent(this, CrashHelper::class.java))

        remoteSettingsService =
            RemoteSettingsService(
                application,
                RemoteSettingsServer.Prod.into(),
                channel = "prototype",
            )

        Log.d(
            LOGTAG,
            "RemoteSettingsService initialized ${remoteSettingsService.remoteSettingsService}",
        )

        setContent {
            SplitApk1Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SplitApk1Theme {
        Greeting("Android")
    }
}
