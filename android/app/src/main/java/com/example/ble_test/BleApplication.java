package com.example.ble_test;

import android.app.Application;
import android.util.Log;

import com.oudmon.ble.base.bluetooth.BleOperateManager;
import com.oudmon.ble.base.bluetooth.OnGattEventCallback;
import com.oudmon.ble.base.communication.CommandHandle;

public class BleApplication extends Application {
    private static final String TAG = "BleApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize BLE Manager
        try {
            // Initialize BLE Manager with the application context
            BleOperateManager bleManager = BleOperateManager.getInstance(this);
            if (bleManager != null) {
                // Set the application instance
                bleManager.setApplication(this);
                
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
            } else {
                Log.e(TAG, "BleOperateManager.getInstance() returned null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing BLE Manager: " + e.getMessage());
        }
    }
}
