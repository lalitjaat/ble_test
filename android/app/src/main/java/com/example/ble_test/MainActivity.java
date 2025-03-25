package com.example.ble_test;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.plugin.common.MethodChannel;

import com.oudmon.ble.base.bluetooth.BleOperateManager;
import com.oudmon.ble.base.bluetooth.OnGattEventCallback;
import com.oudmon.ble.base.communication.CommandHandle;
import com.oudmon.ble.base.communication.ICommandResponse;
import com.oudmon.ble.base.communication.req.BpSettingReq;
import com.oudmon.ble.base.communication.req.ReadHeartRateReq;
import com.oudmon.ble.base.communication.req.ReadDetailSportDataReq;
import com.oudmon.ble.base.communication.req.BloodOxygenSettingReq;
import com.oudmon.ble.base.communication.rsp.BpSettingRsp;
import com.oudmon.ble.base.communication.rsp.ReadHeartRateRsp;
import com.oudmon.ble.base.communication.rsp.ReadDetailSportDataRsp;
import com.oudmon.ble.base.communication.rsp.BloodOxygenSettingRsp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.oudmon.ble.base.communication.entity.BleStepDetails;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "fitness_watch";
    private static final String TAG = "MainActivity";



    private BleOperateManager bleManager;
    private static Context appContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Store application context
        appContext = getApplicationContext();


        // Initialize BLE Manager
        Log.d(TAG, "Initializing BLE Manager...");
        try {
            // Initialize BLE Manager
            bleManager = BleOperateManager.getInstance((Application) getApplicationContext());
            if (bleManager == null) {
                Log.e(TAG, "BleOperateManager.getInstance() returned null");
            } else {
                // Set the application instance
                bleManager.setApplication((Application) getApplicationContext());
                
                // Set up BLE callback
                bleManager.setCallback(new OnGattEventCallback() {
                    @Override
                    public void onReceivedData(String s, byte[] bytes) {

                    }

                    public void bleConnectStatus(int status) {
                        Log.d(TAG, "BLE connection status: " + status);
                    }

                    public void bleDeviceFound(String s, int i) {
                        Log.d(TAG, "BLE device found: " + s + ", RSSI: " + i);
                    }
                });
                
                // Initialize BLE Manager
                bleManager.init();
                Log.d(TAG, "BLE Manager initialized successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing BLE Manager: " + e.getMessage());
        }

        new MethodChannel(Objects.requireNonNull(getFlutterEngine()).getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler((call, result) -> {
                    switch (call.method) {
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

    private void initializeBle(MethodChannel.Result result) {
        Log.d(TAG, "Initializing BLE from Flutter...");
        try {
            if (bleManager == null) {
                Log.d(TAG, "BLE Manager is null, creating new instance");
                bleManager = BleOperateManager.getInstance((Application) getApplicationContext());
                if (bleManager == null) {
                    throw new Exception("BleOperateManager.getInstance() returned null");
                }
                
                // Set the application instance
                bleManager.setApplication((Application) getApplicationContext());
                
                // Set up BLE callback
                bleManager.setCallback(new OnGattEventCallback() {
                    @Override
                    public void onReceivedData(String s, byte[] bytes) {

                    }

                    public void bleConnectStatus(int status) {
                        Log.d(TAG, "BLE connection status: " + status);
                    }

                    public void bleDeviceFound(String s, int i) {
                        Log.d(TAG, "BLE device found: " + s + ", RSSI: " + i);
                    }
                });
                
                // Initialize BLE Manager
                bleManager.init();
            }
            Log.d(TAG, "BLE initialization successful");


            result.success(true);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing BLE: " + e.getMessage());
            result.error("BLE_INIT_ERROR", "Error initializing BLE", e.getMessage());
        }
    }

    private void getFitnessData(MethodChannel.Result result) {

        Log.d(TAG, "Getting fitness data...");
        if (bleManager == null) {
            Log.e(TAG, "BLE Manager is null");
            result.error("BLE_NOT_INITIALIZED", "BLE Manager is not initialized", null);
            return;
        }
        if (!bleManager.isConnected()) {


            Log.e(TAG, "BLE device not connected");
            result.error("BLE_NOT_CONNECTED", "BLE device not connected", null);
            bleManager.connectDirectly("30:35:48:32:8F:04");

            return;
        }
        Log.d(TAG, "BLE connection status OK, proceeding to get data");

        Map<String, Object> fitnessData = new HashMap<>();

        try {
            // Get Heart Rate (BPM)
            CommandHandle.getInstance().executeReqCmd(new ReadHeartRateReq(0), new ICommandResponse<ReadHeartRateRsp>() {
                @Override
                public void onDataResponse(ReadHeartRateRsp data) {
                    byte[] heartRateArray = data.getmHeartRateArray();
                    int heartRate = heartRateArray != null && heartRateArray.length > 0 ? heartRateArray[0] & 0xFF : 0;
                    fitnessData.put("bpm", heartRate);
                    Log.d(TAG, "Heart Rate: " + heartRate);
                }
            });

            // Get SpO2
            CommandHandle.getInstance().executeReqCmd(BloodOxygenSettingReq.getReadInstance(), new ICommandResponse<BloodOxygenSettingRsp>() {
                @Override
                public void onDataResponse(BloodOxygenSettingRsp data) {
                    boolean isEnabled = data.isEnable();
                    fitnessData.put("spo2_enabled", isEnabled);
                    Log.d(TAG, "SpO2 Enabled: " + isEnabled);
                }
            });

            // Get Steps
            CommandHandle.getInstance().executeReqCmd(new ReadDetailSportDataReq(0, 0, 95), new ICommandResponse<ReadDetailSportDataRsp>() {
                @Override
                public void onDataResponse(ReadDetailSportDataRsp data) {
                    ArrayList<BleStepDetails> stepDetails = data.getBleStepDetailses();
                    int totalSteps = 0;
                    if (stepDetails != null && !stepDetails.isEmpty()) {
                        for (BleStepDetails detail : stepDetails) {
                            totalSteps += detail.getWalkSteps() + detail.getRunSteps();
                        }
                    }
                    fitnessData.put("steps", totalSteps);
                    Log.d(TAG, "Steps: " + totalSteps);
                    result.success(fitnessData);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error getting fitness data: " + e.getMessage());
            result.error("BLE_ERROR", "Error getting fitness data", e.getMessage());
        }
    }
}
