package com.aidilab.ekironji.udooble;

import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.aidilab.ble.interfaces.UDOOBLEActivity;
import com.aidilab.ble.interfaces.UDOOBLEFragment;
import com.aidilab.ble.utils.SensorsValues;

import java.util.ArrayList;
import java.util.List;

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

        Button greenLEDBlink = (Button) view.findViewById(R.id.but_green_start_blink);
        greenLEDBlink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MAIN fragment", "on click");
                mActivity.turnLED(UDOOBLEActivity.GREEN_LED, UDOOBLEActivity.BLINK_ON, 100);
            }
        });
        Button greenLEDSTOPBLink = (Button) view.findViewById(R.id.but_green_stop_blink);
        greenLEDSTOPBLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MAIN fragment", "on click");
                mActivity.turnLED(UDOOBLEActivity.GREEN_LED, UDOOBLEActivity.LED_OFF, 100);
            }
        });
        Button greenLEDTurnOn = (Button) view.findViewById(R.id.but_green_turnon);
        greenLEDTurnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MAIN fragment", "on click");
                mActivity.turnLED(UDOOBLEActivity.GREEN_LED, UDOOBLEActivity.LED_ON, 100);
            }
        });
        Button greenLEDTurnOff = (Button) view.findViewById(R.id.but_green_turnoff);
        greenLEDTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MAIN fragment", "on click");
                mActivity.turnLED(UDOOBLEActivity.GREEN_LED, UDOOBLEActivity.LED_OFF, 100);
            }
        });

        Button yellowLEDBLink = (Button) view.findViewById(R.id.but_yellow_start_blink);
        yellowLEDBLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MAIN fragment", "on click");
                mActivity.turnLED(UDOOBLEActivity.YELLOW_LED, UDOOBLEActivity.BLINK_ON, 100);
            }
        });
        Button yellowLEDSTOPBLink = (Button) view.findViewById(R.id.but_yellow_led_stop_blink);
        yellowLEDSTOPBLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MAIN fragment", "on click");
                mActivity.turnLED(UDOOBLEActivity.YELLOW_LED, UDOOBLEActivity.LED_OFF, 100);
            }
        });
        Button yellowLEDTurnOn = (Button) view.findViewById(R.id.but_yellow_led_turnon);
        yellowLEDTurnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MAIN fragment", "on click");
                mActivity.turnLED(UDOOBLEActivity.YELLOW_LED, UDOOBLEActivity.LED_ON, 100);
            }
        });
        Button yellowLEDTurnOff = (Button) view.findViewById(R.id.but_yellow_led_turnoff);
        yellowLEDTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MAIN fragment", "on click");
                mActivity.turnLED(UDOOBLEActivity.YELLOW_LED, UDOOBLEActivity.LED_OFF, 100);
            }
        });

        Button redLEDBLink = (Button) view.findViewById(R.id.but_red_led_start_blink);
        redLEDBLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MAIN fragment", "on click");
                mActivity.turnLED(UDOOBLEActivity.RED_LED, UDOOBLEActivity.BLINK_ON, 100);
            }
        });
        Button redLEDSTOPBLink = (Button) view.findViewById(R.id.but_red_led_stop_blink);
        redLEDSTOPBLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MAIN fragment", "on click");
                mActivity.turnLED(UDOOBLEActivity.RED_LED, UDOOBLEActivity.LED_OFF, 100);
            }
        });
        Button redLEDTurnOn = (Button) view.findViewById(R.id.but_red_led_turnon);
        redLEDTurnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MAIN fragment", "on click");
                mActivity.turnLED(UDOOBLEActivity.RED_LED, UDOOBLEActivity.LED_ON, 100);
            }
        });
        Button redLEDTurnOff = (Button) view.findViewById(R.id.but_red_led_turnoff);
        redLEDTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MAIN fragment", "on click");
                mActivity.turnLED(UDOOBLEActivity.RED_LED, UDOOBLEActivity.LED_OFF, 100);
            }
        });

        Button readTemp = (Button) view.findViewById(R.id.but_temp_read);
        readTemp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.enableTemperature();
                mActivity.readTemperature();
            }
        });

        final Spinner spinner_temp = (Spinner) view.findViewById(R.id.spinner_temp_precision);
        List<String> list = new ArrayList<String>();
        list.add("9");
        list.add("10");
        list.add("11");
        list.add("12");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_temp.setAdapter(dataAdapter);

        Button temp_set_precision = (Button) view.findViewById(R.id.but_set_precision);
        temp_set_precision.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.setTempPrecision(Integer.parseInt(spinner_temp.getSelectedItem().toString()));
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
