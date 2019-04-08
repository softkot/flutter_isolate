package com.example.isolate.event_channel_isolate

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import io.flutter.plugin.common.JSONUtil
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import org.json.JSONObject

class BackgroundPlugin(val registrar: Registrar) : MethodCallHandler {

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            MethodChannel(registrar.messenger(), "background_plugin.methods").apply {
                setMethodCallHandler(BackgroundPlugin(registrar))
            }
        }
    }

    private val serviceContext = BackgroundService.IsolateContext(registrar)


    private fun runIntent(intent: Intent) {
        //registrar.context().startService(intent) /// реальный вызов через сервис
        serviceContext.processIntent(intent) /// вызов в обход сервиса
    }

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    override fun onMethodCall(call: MethodCall, result: Result) {
        val intent = Intent(registrar.context(), BackgroundService::class.java)
                .apply {
                    putExtra("method", call.method)
                    putExtra("args", JSONUtil.wrap(call.arguments).toString())
                    putExtra("result", object : ResultReceiver(handler) {
                        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                            val res = JSONUtil.unwrap(JSONObject(resultData?.getString("result")))
                            result.success(res)
                        }
                    })
                }
            runIntent(intent)
    }
}
