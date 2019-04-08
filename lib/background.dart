import 'dart:async';

import 'package:flutter/services.dart';

/// Публичный интерфейс плагина
class BackgroundPlugin {
  static const _pluginMethods =
      const MethodChannel('background_plugin.methods');

  static Future<Map> method1(String param1, String param2) {
    return _schedule("method1", {
      "param1": param1,
      "param2": param1,
    });
  }

  ///  передача данных о вызова в нативную часть и ожидание результата
  ///  в качестве входных и выходных параметров дупустимо передавать только Map
  ///  поля которог могут быть сериализованы в соответствии с
  ///  https://flutter.dev/docs/development/platform-integration/platform-channels
  static Future<Map> _schedule(String method, Map args) {
    initBackground();
    var res = new Completer<Map>();
    _pluginMethods.invokeMethod(method, args).then((value) {
      res.complete(value as Map);
    }).catchError((err) {
      res.completeError(err);
    });
    return res.future;
  }
}

/// Синглтон работающий в изоляте
final _isolate = new _IsolateHolder();

/// Инкапсюлация всей логики а данных которые потенциально выполняются в фоновом изоляте
class _IsolateHolder {
  /// Доступ к вызову методов на стороне фонового сервиса
  MethodChannel _serviceMethods = const MethodChannel('background_service');

  /// Асинхронные события со стороны фонового сервиса (передаются данные о поступивших задачах)
  EventChannel _serviceEvents = const EventChannel('background_service.tasks');

  /// признак что изолят готовится к завершению
  var _terminated = false;

  ///создание синглтона и ожидание поступления команд
  _IsolateHolder() {
    _serviceEvents.receiveBroadcastStream().listen(
        (task) {
          if (task != null) {
            () async {
              var methodName = task.remove("method") as String;
              var args = task.remove("args") as Map;
              task["result"] = await _handleTask(methodName, args);
              _serviceMethods.invokeMethod("reply", task);
            }();
          }
        },
        cancelOnError: false,
        onError: (err) {
          print("Service events error " + err.toString());
        },
        onDone: () {
          _terminated = true;
        });
  }

  ///фактический обработчик команд поступивщих на обработку в фоновый изолят
  Future<Map> _handleTask(String methodName, Map args) async {
    switch (methodName) {
      case "method1":
        var p1 = args["param1"];
        var p2 = args["param2"];
        return {"r1": p1, "r2": p2};
      default:
        return {};
    }
  }

  void _ensureInitialized() {
    print("Isolate started");
  }
}

/// Метод вызывается для гарантированного создания синглтона управления звонками
/// плюс он же является обязательной точкой входа при работе внутри изолята (фонового сервиса)
void initBackground() {
  _isolate._ensureInitialized();
}
