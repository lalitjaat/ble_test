package com.example.ble_test;

import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.content.Context;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.plugin.common.MethodChannel;

import com.oudmon.ble.base.bluetooth.BleOperateManager;
import com.oudmon.ble.base.bluetooth.OnGattEventCallback;
import com.oudmon.ble.base.communication.CommandHandle;
import com.oudmon.ble.base.communication.ICommandResponse;
import com.oudmon.ble.base.communication.req.BloodOxygenSettingReq;
import com.oudmon.ble.base.communication.req.ReadDetailSportDataReq;
import com.oudmon.ble.base.communication.req.ReadHeartRateReq;
import com.oudmon.ble.base.communication.rsp.BloodOxygenSettingRsp;
import com.oudmon.ble.base.communication.rsp.ReadDetailSportDataRsp;
import com.oudmon.ble.base.communication.rsp.ReadHeartRateRsp;
import com.oudmon.ble.base.communication.rsp.BatteryRsp;
import com.oudmon.ble.base.communication.entity.BleStepDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "fitness_watch";
    private static final String TAG = "MainActivity";

    private BleOperateManager bleManager;
    private static Context appContext;
    private String deviceMacAddress = "30:35:48:32:8F:04"; // Replace with actual MAC address

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appContext = getApplicationContext();

        Log.d(TAG, "Initializing BLE Manager...");
        initializeBleManager();

        new MethodChannel(Objects.requireNonNull(getFlutterEngine()).getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler((call, result) -> {
                    switch (call.method) {
                        case "scanForDevices":
                            scanForDevices(result);
                            break;
                        case "connectToDevice":
                            String macAddress = call.argument("mac");
                            connectToDevice(macAddress, result);
                            break;
                        case "getFitnessData":
                            getFitnessData(result);
                            break;
                        case "initializeBle":
                            initializeBle(result);
                            break;
                        default:
                            result.notImplemented();
                            break;
                    }
                });

    }
    private void initializeBleManager() {
        try {
            bleManager = BleOperateManager.getInstance((Application) getApplicationContext());
            if (bleManager == null) {
                Log.e(TAG, "BleOperateManager.getInstance() returned null");
                return;
            }
            bleManager.setApplication((Application) getApplicationContext());
            bleManager.setCallback(new OnGattEventCallback() {
                @Override
                public void onReceivedData(String s, byte[] bytes) {}

                public void bleConnectStatus(int status) {
                    Log.d(TAG, "BLE connection status: " + status);
                }

                public void bleDeviceFound(String s, int i) {
                    Log.d(TAG, "BLE device found: " + s + ", RSSI: " + i);
                }
            });
            bleManager.init();
            Log.d(TAG, "BLE Manager initialized successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing BLE Manager: " + e.getMessage());
        }
    }
    private void scanForDevices(MethodChannel.Result result) {
        if (bleManager == null) {
            result.error("BLE_NOT_INITIALIZED", "BLE Manager is not initialized", null);
            return;
        }

        List<Map<String, Object>> devicesList = new ArrayList<>();

        bleManager.setCallback(new OnGattEventCallback() {
            @Override
            public void onReceivedData(String s, byte[] bytes) {}

            public void bleDeviceFound(String macAddress, int rssi) {
                Log.d(TAG, "Device found: " + macAddress + " RSSI: " + rssi);

                Map<String, Object> deviceData = new HashMap<>();
                deviceData.put("mac", macAddress);
                deviceData.put("rssi", rssi);

                devicesList.add(deviceData);
            }

            public void bleConnectStatus(int status) {
                Log.d(TAG, "BLE connection status: " + status);
            }
        });

        bleManager.classicBluetoothStartScan();

        // Return the list to Flutter after scanning for 5 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            bleManager.classicBluetoothStopScan();
            result.success(devicesList);
        }, 5000);
    }

    private void connectToDevice(String macAddress, MethodChannel.Result result) {
        if (bleManager == null) {
            result.error("BLE_NOT_INITIALIZED", "BLE Manager is not initialized", null);
            return;
        }

        Log.d(TAG, "Connecting to device: " + macAddress);

        bleManager.connectDirectly(macAddress);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (bleManager.isConnected()) {
                Log.d(TAG, "Connected successfully to " + macAddress);
                result.success(true);
            } else {
                Log.e(TAG, "Failed to connect");
                result.error("CONNECTION_FAILED", "Could not connect to device", null);
            }
        }, 3000);
    }
    private void initializeBle(MethodChannel.Result result) {
        Log.d(TAG, "Initializing BLE from Flutter...");
        if (bleManager == null) {
            initializeBleManager();
        }
        result.success(true);
    }

    private void getFitnessData(MethodChannel.Result result) {
        Log.d(TAG, "Getting fitness data...");
        if (bleManager == null) {
            Log.e(TAG, "BLE Manager is null");
            result.error("BLE_NOT_INITIALIZED", "BLE Manager is not initialized", null);
            return;
        }
        if (!bleManager.isConnected()) {
            Log.e(TAG, "BLE device not connected, trying to reconnect...");
            bleManager.connectDirectly(deviceMacAddress);

            new Handler(Looper.getMainLooper()).postDelayed(() -> getFitnessData(result), 3000);
            return;
        }

        Log.d(TAG, "BLE connection established. Fetching data...");
        Map<String, Object> fitnessData = new HashMap<>();

        CommandHandle.getInstance().executeReqCmd(new ReadHeartRateReq(0), new ICommandResponse<ReadHeartRateRsp>() {
            @Override
            public void onDataResponse(ReadHeartRateRsp data) {
                int heartRate = (data.getmHeartRateArray() != null && data.getmHeartRateArray().length > 0) ? data.getmHeartRateArray()[0] & 0xFF : 0;
                Log.d(TAG, "Heart Rate from Java: " + data.getStatus());
                fitnessData.put("HeartRate", data.getmHeartRateArray());

            }
        });

        CommandHandle.getInstance().executeReqCmd(BloodOxygenSettingReq.getReadInstance(), new ICommandResponse<BloodOxygenSettingRsp>() {
            @Override
            public void onDataResponse(BloodOxygenSettingRsp data) {
                fitnessData.put("spO2", data.isEnable());
                Log.d(TAG, "SpO2 Enabled: " + data.isEnable());
            }
        });

        CommandHandle.getInstance().executeReqCmd(new ReadDetailSportDataReq(0, 0, 95), new ICommandResponse<ReadDetailSportDataRsp>() {
            @Override
            public void onDataResponse(ReadDetailSportDataRsp data) {
                List<BleStepDetails> stepDetails = data.getBleStepDetailses();
                int totalSteps = stepDetails != null && !stepDetails.isEmpty() ? stepDetails.get(stepDetails.size() - 1).getWalkSteps() : 0;
                fitnessData.put("steps", totalSteps);
                Log.d(TAG, "Steps: " + totalSteps);
            }
        });

        // CommandHandle.getInstance().executeReqCmd(new ReadDetailSportDataReq(0, 0, 95), new ICommandResponse<BatteryRsp>() {
        //     @Override
        //     public void onDataResponse(BatteryRsp data) {
        //         fitnessData.put("battery", data.getStatus());
        //         Log.d(TAG, "Battery Status: " + data.getStatus());
        //     }
        // });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "Sending fitness data to Flutter");
            result.success(fitnessData);
        }, 2000);
    }
}
