package com.example.isolate.event_channel_isolate

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import io.flutter.view.FlutterMain
import io.flutter.view.FlutterNativeView
import io.flutter.view.FlutterRunArguments
import java.util.concurrent.atomic.AtomicInteger

class PluginService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    private lateinit var bgView: FlutterNativeView
    override fun onCreate() {
        super.onCreate()
        FlutterMain.startInitialization(this)
        FlutterMain.ensureInitializationComplete(this, null)
        bgView = FlutterNativeView(this, true)
        Plugin(bgView.pluginRegistry, "Service")
        bgView.runFromBundle(FlutterRunArguments().apply {
            bundlePath = FlutterMain.findAppBundlePath(this@PluginService)
            entrypoint = "platformChannels"
        })
    }
}