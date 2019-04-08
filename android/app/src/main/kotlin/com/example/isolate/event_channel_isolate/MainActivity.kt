package com.example.isolate.event_channel_isolate

import android.content.Intent
import android.os.Bundle

import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.EventChannel
import io.flutter.plugins.GeneratedPluginRegistrant

class MainActivity : FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BackgroundPlugin.registerWith(this.registrarFor("background_plugin"))
        GeneratedPluginRegistrant.registerWith(this)
    }
}
