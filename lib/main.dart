import 'package:flutter/material.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Fitness Watch',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const platform = MethodChannel('fitness_watch');

  BluetoothDevice? selectedDevice;
  List<BluetoothDevice> devicesList = [];
  Map<String, dynamic> fitnessData = {
    'HeartRate': 0,
    'spO2': false,
    'steps': 0,
  };

  @override
  void initState() {
    super.initState();
    _initBluetooth();
  }

  Future<void> _initBluetooth() async {
    if (await FlutterBluePlus.isSupported == false) {
      print("Bluetooth not supported");
      return;
    }

    await [Permission.bluetoothScan, Permission.bluetoothConnect, Permission.location].request();

    if (await FlutterBluePlus.isOn == false) {
      await FlutterBluePlus.turnOn();
    }

    _startScanning();
  }

  Future<void> _startScanning() async {
    devicesList.clear();
    FlutterBluePlus.scanResults.listen((results) {
      for (ScanResult result in results) {
        if (!devicesList.contains(result.device) && result.device.platformName.isNotEmpty) {
          setState(() {
            devicesList.add(result.device);
          });
        }
      }
    });
    await FlutterBluePlus.startScan(timeout: const Duration(seconds: 6));
  }

  Future<void> _connectToDevice(BluetoothDevice device) async {
    try {
      await platform.invokeMethod('initializeBle');

      if (selectedDevice != null && selectedDevice!.remoteId != device.remoteId) {
        await selectedDevice!.disconnect();
      }

      await device.connect();
      setState(() => selectedDevice = device);

      _getFitnessData();
    } catch (e) {
      print('Connection Error: $e');
      setState(() => selectedDevice = null);
    }
  }

  Future<void> _getFitnessData() async {
    try {
      final result = await platform.invokeMethod('getFitnessData');
      setState(() {
        fitnessData = Map<String, dynamic>.from(result);
      });
    } catch (e) {
      print('Error fetching data: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Fitness Watch'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _startScanning,
          ),
        ],
      ),
      body: Column(
        children: [
          if (selectedDevice != null) ...[
            ListTile(
              title: const Text('Heart Rate'),
              trailing: Text('${fitnessData['HeartRate']} BPM'),
            ),
            ListTile(
              title: const Text('SpO2 Monitoring'),
              trailing: Text(fitnessData['spO2'] ? 'Enabled' : 'Disabled'),
            ),
            ListTile(
              title: const Text('Steps'),
              trailing: Text('${fitnessData['steps']} steps'),
            ),
            ElevatedButton(
              onPressed: _getFitnessData,
              child: const Text('Refresh Data'),
            ),
          ],
          Expanded(
            child: ListView.builder(
              itemCount: devicesList.length,
              itemBuilder: (context, index) {
                final device = devicesList[index];
                return ListTile(
                  title: Text(device.platformName),
                  subtitle: Text(device.remoteId.toString()),
                  trailing: ElevatedButton(
                    onPressed: () => _connectToDevice(device),
                    child: Text(
                      selectedDevice?.remoteId == device.remoteId
                          ? 'Connected'
                          : 'Connect',
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    FlutterBluePlus.stopScan();
    super.dispose();
  }
}
