package com.example.isolate.event_channel_isolate

import android.util.Log
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import java.util.concurrent.atomic.AtomicInteger

class Plugin(registry: PluginRegistry, val label: String) {
    @Volatile
    private var sink: EventChannel.EventSink? = null
    private val counter = AtomicInteger()

    init {
        registry.registrarFor("isolate").apply {
            MethodChannel(messenger(), "methods").setMethodCallHandler { methodCall, result ->
                sink?.success(hashMapOf(
                        "label" to label,
                        "counter" to counter.incrementAndGet()
                ))
                Log.d("PLUGIN", "Call ${methodCall.method} from $label")
                result.success(null)
            }
            EventChannel(messenger(), "events").setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(p0: Any?, s: EventChannel.EventSink?) {
                    sink?.endOfStream()
                    sink = s
                }

                override fun onCancel(p0: Any?) {
                    sink?.endOfStream()
                    sink = null
                }
            })
        }

    }
}