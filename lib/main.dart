import 'package:flutter/material.dart';
import 'dart:async';
import 'dart:io';
import 'dart:convert';
import 'package:flutter/services.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key}) : super(key: key);

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  @override
  Widget build(BuildContext context) {
    return Text("");
  }

  @override
  void initState() {
    platformChannels();
  }
}

void platformChannels() {
  WidgetsFlutterBinding.ensureInitialized();
  print("Start platform channels test");
  MethodChannel pluginMethods = const MethodChannel('methods');
  EventChannel pluginEvents = const EventChannel('events');
  pluginEvents.receiveBroadcastStream().listen((data) {
    print(data);
  });
  Timer.periodic(Duration(seconds: 1), (timer) {
    pluginMethods.invokeMethod("tick", {});
  });
}
