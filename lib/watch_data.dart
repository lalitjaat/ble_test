import 'package:flutter/services.dart';

class WatchData {
  static const MethodChannel _channel = MethodChannel('watch_sdk');

  static Future<void> initialize() async {
    try {
      await _channel.invokeMethod('initialize');
    } catch (e) {
      throw Exception('Failed to initialize watch: $e');
    }
  }

  static Future<int> getHeartRate() async {
    try {
      final int result = await _channel.invokeMethod('getHeartRate');
      return result;
    } catch (e) {
      throw Exception('Failed to get heart rate: $e');
    }
  }

  static Future<int> getSpO2() async {
    try {
      final int result = await _channel.invokeMethod('getSpO2');
      return result;
    } catch (e) {
      throw Exception('Failed to get SpO2: $e');
    }
  }

  static Future<int> getSteps() async {
    try {
      final int result = await _channel.invokeMethod('getSteps');
      return result;
    } catch (e) {
      throw Exception('Failed to get steps: $e');
    }
  }
}