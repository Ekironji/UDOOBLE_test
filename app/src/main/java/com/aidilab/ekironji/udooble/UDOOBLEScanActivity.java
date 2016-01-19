package com.aidilab.ekironji.udooble;

import android.content.Intent;
import android.os.Bundle;

import com.aidilab.ble.interfaces.DevicesBLEScanActivity;

/**
 * Created by Luca on 12/01/2016.
 */
public class UDOOBLEScanActivity extends DevicesBLEScanActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.onScanViewReady();
    }

    @Override
    protected void startDeviceActivity() {
        this.mDeviceIntent = new Intent(this, UDOOBLEMainActivity.class);
        mDeviceIntent.putExtra(UDOOBLEMainActivity.EXTRA_DEVICE, this.mBluetoothDevice);
        startActivityForResult(mDeviceIntent, UDOOBLEScanActivity.REQ_DEVICE_ACT);
    }
}
