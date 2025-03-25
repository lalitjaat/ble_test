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
    'bpm': 0,
    'spo2_enabled': false,
    'steps': 0,
  };

  @override
  void initState() {
    super.initState();
    _initBluetooth();
  }

  Future<void> _initBluetooth() async {
    // Check if Bluetooth is supported
    if (await FlutterBluePlus.isSupported == false) {
      print("Bluetooth not supported by this device");
      return;
    }

    // Request permissions
    try {
      await [Permission.bluetoothScan, Permission.bluetoothConnect, Permission.location].request();
    } catch (e) {
      print('Error requesting permissions: $e');
      return;
    }

    // Turn on Bluetooth
    if (await FlutterBluePlus.isOn == false) {
      try {
        await FlutterBluePlus.turnOn();
      } catch (e) {
        print('Error turning on Bluetooth: $e');
        return;
      }
    }

    // Start scanning
    await _startScanning();
  }

  Future<void> _startScanning() async {
    print('Starting BLE scan...');
    setState(() {
      devicesList.clear();
    });

    try {
      // Check Bluetooth state
      final isOn = await FlutterBluePlus.isOn;
      print('Bluetooth is ${isOn ? 'ON' : 'OFF'}');
      if (!isOn) {
        print('Attempting to turn on Bluetooth...');
        await FlutterBluePlus.turnOn();
      }

      // Listen for scan results
      FlutterBluePlus.scanResults.listen((results) {
        print('Scan results received: ${results.length} devices');
        for (ScanResult result in results) {
          print('Found device: ${result.device.platformName} (${result.device.remoteId})');
          if (!devicesList.contains(result.device) &&
              result.device.platformName.isNotEmpty) {
            setState(() {
              devicesList.add(result.device);
            });
          }
        }
      });

      print('Starting scan with 4 second timeout...');
      await FlutterBluePlus.startScan(timeout: const Duration(seconds: 4));
      print('Scan completed');
    } catch (e) {
      print('Error scanning: $e');
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error scanning: $e')),
      );
    }
  }

  Future<void> _connectToDevice(BluetoothDevice device) async {
    print("Connection State: ${device.connectionState}");
    // await device.connect();
    // setState(() {
    //       selectedDevice = device;
    //     });
    // print("Service List: ${device.servicesList}");
    //
    // print("Connection State: ${device.connectionState}");
    try {
      // Initialize BLE SDK
      await platform.invokeMethod('initializeBle');

      // Connect to the device
      //await device.connect();
      setState(() {
        selectedDevice = device;
      });

      // Get initial fitness data
      //await _getFitnessData();
    } catch (e) {
      print('Error connecting: $e');
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error connecting to device: $e')),
      );
    }
  }

  Future<void> _getFitnessData() async {
    try {
      final result = await platform.invokeMethod('getFitnessData');
      setState(() {
        fitnessData = Map<String, dynamic>.from(result);

        print("Fitness Data Value:${fitnessData}");
      });
    } on PlatformException catch (e) {
      print('Error getting fitness data: ${e.message}');
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
          // Fitness Data Display
          if (selectedDevice != null) ...[            
            ListTile(
              title: const Text('Heart Rate'),
              trailing: Text('${fitnessData['bpm']} BPM'),
            ),
            ListTile(
              title: const Text('SpO2 Monitoring'),
              trailing: Text(fitnessData['spo2_enabled'] ? 'Enabled' : 'Disabled'),
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
          // Device List
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
