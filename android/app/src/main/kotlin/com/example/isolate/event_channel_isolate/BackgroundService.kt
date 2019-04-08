package com.example.isolate.event_channel_isolate

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.ResultReceiver
import android.util.Log
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.JSONUtil
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugins.GeneratedPluginRegistrant
import io.flutter.view.FlutterMain
import io.flutter.view.FlutterNativeView
import io.flutter.view.FlutterRunArguments
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.log

class BackgroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    private lateinit var bgView: FlutterNativeView
    private lateinit var serviceContex: IsolateContext

    override fun onCreate() {
        super.onCreate()
        FlutterMain.startInitialization(this)
        FlutterMain.ensureInitializationComplete(this, null)
        bgView = FlutterNativeView(this, true)
        GeneratedPluginRegistrant.registerWith(bgView.pluginRegistry)
        serviceContex = IsolateContext(bgView.pluginRegistry.registrarFor(IsolateContext.PLUGIN_KEY))
        bgView.runFromBundle(FlutterRunArguments().apply {
            bundlePath = FlutterMain.findAppBundlePath(this@BackgroundService)
            entrypoint = "bgmain"
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            serviceContex.processIntent(intent)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        bgView.destroy()
    }


    class IsolateContext(registrar: PluginRegistry.Registrar) {
        companion object {
            val PLUGIN_KEY = "background_plugin.service"
        }

        private val intents = LinkedBlockingQueue<Intent>()
        @Volatile
        private var sink: EventChannel.EventSink? = null
        private val methodCounter = AtomicLong()

        private val pendingReplies = ConcurrentHashMap<Long, Intent>()

        init {
            MethodChannel(registrar.messenger(), "background_service").apply {
                setMethodCallHandler(MethodChannel.MethodCallHandler { call, result ->
                    when (call.method) {
                        "reply" -> {
                            val id = call.argument<Number>("id")?.toLong()
                            val res = call.argument<Any>("result")
                            pendingReplies.remove(id)?.getParcelableExtra<ResultReceiver>("result")?.send(
                                    0,
                                    Bundle().apply {
                                        putString("result", JSONUtil.wrap(res).toString())
                                    }
                            )
                        }
                        else -> {
                            result.notImplemented()
                            return@MethodCallHandler
                        }
                    }
                    result.success(null)
                })
            }

            EventChannel(registrar.messenger(), "background_service.tasks").apply {
                setStreamHandler(object : EventChannel.StreamHandler {
                    override fun onCancel(holder: Any?) {
                        sink = null
                        stop()
                    }

                    override fun onListen(holder: Any?, newSink: EventChannel.EventSink?) {
                        sink = newSink
                        sendIntents()
                    }
                })
            }
        }


        private fun sendIntents() {
            sink?.let { sender ->
                while (intents.isNotEmpty()) {
                    intents.remove()?.let {
                        val mid = methodCounter.incrementAndGet()
                        pendingReplies[mid] = it
                        hashMapOf(
                                "id" to mid,
                                "method" to it.getStringExtra("method"),
                                "args" to JSONUtil.unwrap(JSONObject(it.getStringExtra("args")))).also {
                            Log.i("BG", "Put task $it to queue")
                        }
                    }?.let { data ->
                        sender.success(data)
                    }
                }
            }
        }


        fun processIntent(intent: Intent) {
            intents.add(intent)
            sendIntents()
        }

        fun stop() {
            sink?.endOfStream()
            sink = null
            intents.clear()
            pendingReplies.clear()
        }


    }
}