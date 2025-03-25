import "package:flutter/material.dart";
import 'watch_data.dart';

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  int heartRate = 0;
  int spO2 = 0;
  int steps = 0;

  @override
  void initState() {
    super.initState();
    initializeWatch();
  }

  Future<void> initializeWatch() async {
    await WatchData.initialize();
    await updateData();
  }

  Future<void> updateData() async {
    final hr = await WatchData.getHeartRate();
    final spo2 = await WatchData.getSpO2();
    final stepCount = await WatchData.getSteps();
    
    setState(() {
      heartRate = hr;
      spO2 = spo2;
      steps = stepCount;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: Text('Fitness Watch')),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text("Heart Rate: $heartRate bpm"),
              Text("SpO2: $spO2%"),
              Text("Steps: $steps"),
              ElevatedButton(
                onPressed: updateData,
                child: Text("Refresh Data"),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
