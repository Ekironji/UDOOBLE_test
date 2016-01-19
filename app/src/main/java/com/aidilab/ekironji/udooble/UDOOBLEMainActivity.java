package com.aidilab.ekironji.udooble;

import android.os.Bundle;

import com.aidilab.ble.interfaces.UDOOBLEActivity;
import com.aidilab.ble.sensor.UDOOBLESensor;

/**
 * Created by Ekironji on 12/01/2016.
 */
public class UDOOBLEMainActivity extends UDOOBLEActivity {

    private UDOOBLEMainFragment mFragmentView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity_layout);

        mFragmentView = new UDOOBLEMainFragment();

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, mFragmentView).commit();
        }

        enableSensors(UDOOBLESensor.ACCELEROMETER);
    }

    @Override
    public void onGestureDetected(int gestureId) {

    }

}

