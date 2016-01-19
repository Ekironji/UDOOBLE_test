package com.aidilab.ekironji.udooble;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.aidilab.ble.interfaces.UDOOBLEActivity;
import com.aidilab.ble.interfaces.UDOOBLEFragment;
import com.aidilab.ble.utils.SensorsValues;

/**
 * Created by Luca on 19/01/2016.
 */
public class UDOOBLEMainFragment extends UDOOBLEFragment {

    private UDOOBLEActivity mActivity;

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    mInstance = this;
	    mActivity = (UDOOBLEActivity) getActivity();

	    View view = inflater.inflate(R.layout.fragment_layout, container, false);

	    /**   GUI initialization - getting gui references */

        Button buttonLED = (Button) view.findViewById(R.id.button_led);
        buttonLED.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MAIN fragment", "on click");
                mActivity.turnLED(100, 1);
            }
        });

        mActivity.onViewInflated(view);
	    return view;
	}

    @Override
    public void onCharacteristicChanged(String uuidStr, byte[] rawValue) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCharacteristicChanged(String uuidStr, SensorsValues sv) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGestureDetected(int gestureId) {

    }

    @Override
    public void onClick(View v) {

    }
}
