package com.aidilab.ble.interfaces;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.aidilab.ble.R;
import com.aidilab.ble.common.GattInfo;
import com.aidilab.ble.fragment.DrumSimpleFragment;
import com.aidilab.ble.sensor.BleService;
import com.aidilab.ble.sensor.UDOOBLE;
import com.aidilab.ble.sensor.UDOOBLESensor;
import com.aidilab.ble.utils.Point3D;
import com.aidilab.ble.utils.SensorsValues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.aidilab.ble.sensor.UDOOBLE.UUID_ACC_DATA;
import static com.aidilab.ble.sensor.UDOOBLE.UUID_GYR_DATA;
import static com.aidilab.ble.sensor.UDOOBLE.UUID_KEY_DATA;
import static com.aidilab.ble.sensor.UDOOBLE.UUID_MAG_DATA;


public abstract class UDOOBLEActivity extends FragmentActivity{
	
	private static String TAG = "UDOOBLEActivity";

	final public static byte LED_ON		= 0x01;
	final public static byte LED_OFF		= 0x00;
	final public static byte BLINK_ON		= 0x02;
	final byte BLINK_OFF	= 0x00;

	final public static int GREEN_LED	= 1;
	final public static int YELLOW_LED = 2;
	final public static int RED_LED	= 3;


	// Activity
	public static final String EXTRA_DEVICE = "EXTRA_DEVICE";

	//private DeviceViewFragment mDeviceView         = null;
	private DrumSimpleFragment mDeviceView = null;

	// BLE
	private BleService mBtLeService     = null;
	private BluetoothDevice            mBtDevice        = null;
	private BluetoothGatt              mBtGatt          = null;
	private List<BluetoothGattService> mServiceList     = null;
	private static final int           GATT_TIMEOUT     = 300; // milliseconds
	private boolean                    mServicesRdy     = false;
	private boolean                    mIsReceiving     = false;

	// SensorTag
	private List<UDOOBLESensor>   mEnabledSensors     = new ArrayList<UDOOBLESensor>();
	private BluetoothGattService mOadService         = null;
	private BluetoothGattService mConnControlService = null;
	
	// Gesture Recognizer
	protected GestureDetector mGestureDetector = null;
	
	// Sensors parameter
	private int NOTIFICATIONS_PERIOD = 10;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	    
	    Intent intent = getIntent();
	    
	    // BLE
	    mBtLeService = BleService.getInstance();
	    mBtDevice = intent.getParcelableExtra(EXTRA_DEVICE);
	    mServiceList = new ArrayList<BluetoothGattService>();
	
	    // GATT database
	    Resources res = getResources();
	    XmlResourceParser xpp = res.getXml(R.xml.gatt_uuid);
	    new GattInfo(xpp);
	    
	    // clear sensor list
	    mEnabledSensors.clear();
	}
	

	@Override
	public void onDestroy() {
	    super.onDestroy();
	}

	@Override 
	protected void onResume() {
	  	Log.d(TAG,"onResume");
	    super.onResume();
	    if (!mIsReceiving) {
	    	registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	    	mIsReceiving = true;
	    }
	}

	@Override
	protected void onPause() {
		Log.d(TAG,"onPause");
		super.onPause();
		if (mIsReceiving) {
			unregisterReceiver(mGattUpdateReceiver);
			mIsReceiving = false;
		}
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
	  	final IntentFilter fi = new IntentFilter();
	  	fi.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
	  	fi.addAction(BleService.ACTION_DATA_NOTIFY);
	  	fi.addAction(BleService.ACTION_DATA_WRITE);
	  	fi.addAction(BleService.ACTION_DATA_READ);
	  	return fi;
	}

	public void onViewInflated(View view) {
	    Log.d(TAG, "Gatt view ready");
	
	    // Create GATT object
	    mBtGatt = BleService.getBtGatt();
	
	    // Start service discovery
	    if (!mServicesRdy && mBtGatt != null) {
	      if (mBtLeService.getNumServices() == 0)
	        discoverServices();
	      else
	        displayServices();
	    }
	}


	// Application implementation
	//
	BluetoothGattService getOadService() {
		return mOadService;
	}

	BluetoothGattService getConnControlService() {
		return mConnControlService;
	}

	private void discoverServices() {
	    if (mBtGatt.discoverServices()) {
	      Log.i(TAG, "START SERVICE DISCOVERY");
	      mServiceList.clear();
	      setStatus("Service discovery started");
	    } else {
	      setError("Service discovery start failed");
	    }
	}

	private void displayServices() {
	    mServicesRdy = true;
	
	    try {
	      mServiceList = mBtLeService.getSupportedGattServices();
	    } catch (Exception e) {
	      e.printStackTrace();
	      mServicesRdy = false;
	    }

	    // Characteristics descriptor readout done
	    if (mServicesRdy) {
	      setStatus("Service discovery complete");
	      enableSensors(true);
	      enableNotifications(true);
	    } else {
	      setError("Failed to read services");
	    }
	}

	
	private void setError(String txt) {}
	private void setStatus(String txt) {}
	
	
	private void enableSensors(boolean enable) {
	  	for (UDOOBLESensor sensor : mEnabledSensors) {
	  		UUID servUuid = sensor.getService();
	  		UUID confUuid = sensor.getConfig();
	  		
	  		// Skip keys 
	  		if (confUuid == null)
	  			break;
	
	  		BluetoothGattService serv = null;
	  		BluetoothGattCharacteristic charac = null;
	  		
	  		byte value;
	  		try {
				serv = mBtGatt.getService(servUuid);
				charac = serv.getCharacteristic(confUuid);
				value = enable ? sensor.getEnableSensorCode()
						: UDOOBLESensor.DISABLE_SENSOR_CODE;
				mBtLeService.writeCharacteristic(charac, value);
				mBtLeService.waitIdle(GATT_TIMEOUT);
			} catch (Exception e) {
				Log.e("DeviceActivity","enableSensor(), service uuid: " + servUuid.toString());
			}
	  		
	  		// FIZZLY: se e' tutti i sensori ne setto il periodo dopo averlo attivato
			/*if (confUuid.equals(Fizzly.UUID_ALL_CONF) && enable) {
				charac = serv.getCharacteristic(Fizzly.UUID_ALL_PERI);
		  		value = (byte) NOTIFICATIONS_PERIOD;
		  		mBtLeService.writeCharacteristic(charac, value);
		  		Log.i("DeviceActivity","Scrtitta la caratteristica del periodo di tutti i sensori : " + value);
				mBtLeService.waitIdle(GATT_TIMEOUT);
			}*/
	  		
			// FIZZLY: se e' accelerometro ne setto il periodo dopo averlo attivato
			if (confUuid.equals(UDOOBLE.UUID_ACC_CONF) && enable) {
				charac = serv.getCharacteristic(UDOOBLE.UUID_ACC_PERI);
		  		value = (byte) NOTIFICATIONS_PERIOD;
		  		mBtLeService.writeCharacteristic(charac, value);
		  		Log.i("DeviceActivity","Scrtitta la caratteristica del periodo dell accelererometro : " + value);
				mBtLeService.waitIdle(GATT_TIMEOUT);
			}	
			
			// FIZZLY: se e' magnetometro ne setto il periodo dopo averlo attivato
			if (confUuid.equals(UDOOBLE.UUID_MAG_CONF) && enable) {
				charac = serv.getCharacteristic(UDOOBLE.UUID_MAG_PERI);
		  		value = (byte) NOTIFICATIONS_PERIOD;
		  		mBtLeService.writeCharacteristic(charac, value);
		  		Log.i("DeviceActivity","Scrtitta la caratteristica del periodo dell magnetometro : " + value);
				mBtLeService.waitIdle(GATT_TIMEOUT);
			}	
			
			// FIZZLY: se e' GIRO ne setto il periodo dopo averlo attivato
			if (confUuid.equals(UDOOBLE.UUID_GYR_CONF) && enable) {
				charac = serv.getCharacteristic(UDOOBLE.UUID_GYR_PERI);
		  		value = (byte) NOTIFICATIONS_PERIOD;
		  		mBtLeService.writeCharacteristic(charac, value);
		  		Log.i("DeviceActivity","Scrtitta la caratteristica del periodo dell GIROSCOPIO : " + value);
				mBtLeService.waitIdle(GATT_TIMEOUT);
			}

			// FIZZLY: se e' GIRO ne setto il periodo dopo averlo attivato
			if (confUuid.equals(UDOOBLE.UUID_TEM_CONF) && enable) {
				charac = serv.getCharacteristic(UDOOBLE.UUID_TEM_PERI);
				value = (byte) NOTIFICATIONS_PERIOD;
				mBtLeService.writeCharacteristic(charac, value);
				Log.i("DeviceActivity","Scrtitta la caratteristica del periodo dell GIROSCOPIO : " + value);
				mBtLeService.waitIdle(GATT_TIMEOUT);
			}
			
	  	}
	}

	private void enableNotifications(boolean enable) {
	  	for (UDOOBLESensor sensor : mEnabledSensors) {
	  		UUID servUuid = sensor.getService();
	  		UUID dataUuid = sensor.getData();
	  		BluetoothGattService serv = mBtGatt.getService(servUuid);
	  		
	  		Log.i(TAG, "enableNotifications service "+ servUuid.toString() + " is null: " + (serv == null) );
	  		
	  		BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
	  		
	  		mBtLeService.setCharacteristicNotification(charac,enable);
			mBtLeService.waitIdle(GATT_TIMEOUT);
	  	}
	} 	
	
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
	  	@Override
	  	public void onReceive(Context context, Intent intent) {
	  		final String action = intent.getAction();
	  		int status = intent.getIntExtra(BleService.EXTRA_STATUS, BluetoothGatt.GATT_SUCCESS);
	
	  		if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
	  			if (status == BluetoothGatt.GATT_SUCCESS) {
	  				displayServices();
	  			} else {
	  				Toast.makeText(getApplication(), "Service discovery failed", Toast.LENGTH_LONG).show();
	  				return;
	  			}
	  		} else if (BleService.ACTION_DATA_NOTIFY.equals(action)) {
	  			// Notification
	  			byte[] value = intent.getByteArrayExtra(BleService.EXTRA_DATA);
	  			String uuidStr = intent.getStringExtra(BleService.EXTRA_UUID);
	  			onCharacteristicChanged(uuidStr, value);
	  		} else if (BleService.ACTION_DATA_WRITE.equals(action)) {
	  			// Data written
	  			String uuidStr = intent.getStringExtra(BleService.EXTRA_UUID);
	  			onCharacteristicWrite(uuidStr,status);
	  		} else if (BleService.ACTION_DATA_READ.equals(action)) {
	  			// Data read
	  			String uuidStr = intent.getStringExtra(BleService.EXTRA_UUID);
	  			byte [] value = intent.getByteArrayExtra(BleService.EXTRA_DATA);
	  			onCharacteristicsRead(uuidStr,value,status);
	  		}
	
	  		if (status != BluetoothGatt.GATT_SUCCESS) {
	  			setError("GATT error code: " + status);
	  		}
	  	}
	};

	private void onCharacteristicWrite(String uuidStr, int status) {
	  Log.d(TAG,"onCharacteristicWrite: " + uuidStr);
	}

	// Arrivano i dati dal sensore le notifiche
	public void onCharacteristicChanged(String uuidStr, byte[] rawValue) {

		Log.d(TAG, "onCharacteristicChanged: " + uuidStr);
		Point3D           v = null;
		SensorsValues    sv = null;
	  	Integer buttonState = null;
		
	  	// Process sensor packet
	  	/*if (uuidStr.equals(UUID_ALL_DATA.toString())) {
	  		
		  	sv = UDOOBLESensor.ACC_MAG_BUTT_BATT.unpack(rawValue);
		  	
		  	// Send data to gesture Recognizer
		  	this.detectSequence(sv);
	  	}*/
				
	  	if (uuidStr.equals(UUID_ACC_DATA.toString())) {
	  		v = UDOOBLESensor.ACCELEROMETER.convert(rawValue);
			Log.i(TAG, "ACCELEROMETER - x: "+ v.x + " - y: "+ v.y + " - z: "+v.z);
	  	} 
	  
	  	if (uuidStr.equals(UUID_MAG_DATA.toString())) {
	  		v = UDOOBLESensor.MAGNETOMETER.convert(rawValue);
			Log.i(TAG, "MAGNETOMETER - x: "+ v.x + " - y: "+ v.y + " - z: "+v.z);
	  	} 
	
	  	if (uuidStr.equals(UUID_GYR_DATA.toString())) {
	  		v = UDOOBLESensor.GYROSCOPE.convert(rawValue);
			Log.i(TAG, "GYROSCOPE - x: "+ v.x + " - y: "+ v.y + " - z: "+v.z);
	  	} 

	  	if (uuidStr.equals(UUID_KEY_DATA.toString())) {
	  		buttonState = UDOOBLESensor.CAPACITIVE_BUTTON.convertKeys(rawValue);
			Log.i(TAG, "buttonState: " + buttonState);
	  		switch (buttonState) {
	  		case 0:
	  			Log.i(TAG, "Button released");
	  			break;
	  		case 1:
	  			Log.i(TAG, "Button pressed");
	  			break;
	  		default:
	  			throw new UnsupportedOperationException();
	  		}
	  	} 
	}

	private void onCharacteristicsRead(String uuidStr, byte [] value, int status) {
		Log.i(TAG, "onCharacteristicsRead: " + uuidStr + " - value: " + Arrays.toString(value) + " - status: " + status);
	}

	public void turnLED(int color, byte func, int millis){
		BluetoothGattService serv = null;
		BluetoothGattCharacteristic charac = null;

		serv   = mBtGatt.getService(UDOOBLE.UUID_LED_SERV);
		switch(color){
			case GREEN_LED:
				charac = serv.getCharacteristic(UDOOBLE.UUID_LED_GREEN);
				break;
			case YELLOW_LED:
				charac = serv.getCharacteristic(UDOOBLE.UUID_LED_YELLOW);
				break;
			case RED_LED:
				charac = serv.getCharacteristic(UDOOBLE.UUID_LED_RED);
				break;
		}
		byte[] msg = new byte[2];
		msg[0] = func;
		msg[1] = (byte)0x03;
		mBtLeService.writeCharacteristic(charac, msg);
		Log.i("DeviceActivity","Scrtitta la caratteristica dei led : " + msg.toString());
		mBtLeService.waitIdle(GATT_TIMEOUT);
	}

	public void enableTemperature() {
		BluetoothGattService serv = null;
		BluetoothGattCharacteristic charac = null;

		serv   = mBtGatt.getService(UDOOBLE.UUID_TEM_SERV);
		charac = serv.getCharacteristic(UDOOBLE.UUID_TEM_CONF);

		mBtLeService.writeCharacteristic(charac, (byte)1);
		mBtLeService.waitIdle(GATT_TIMEOUT);
		Log.i(TAG, "enable temp");
	}

	public void readTemperature() {
		BluetoothGattService serv = null;
		BluetoothGattCharacteristic charac = null;

		serv   = mBtGatt.getService(UDOOBLE.UUID_TEM_SERV);
		charac = serv.getCharacteristic(UDOOBLE.UUID_TEM_DATA);

		mBtLeService.readCharacteristic(charac);
		Log.i(TAG, "read temp");
	}

	public void setTempPrecision(int resolution) {
		Log.i(TAG, "Temp resolution: " + resolution);
		if(resolution < 9  || resolution > 12) {
			setError("TEMPERATURE: resolution error");
		} else {
			BluetoothGattService serv = null;
			BluetoothGattCharacteristic charac = null;

			byte msg = (byte) resolution;

			serv   = mBtGatt.getService(UDOOBLE.UUID_TEM_SERV);
			charac = serv.getCharacteristic(UDOOBLE.UUID_TEM_RESO);
			mBtLeService.writeCharacteristic(charac, msg);
			mBtLeService.waitIdle(GATT_TIMEOUT);
		}
	}

	public void setSensorPeriod(int millis){
		NOTIFICATIONS_PERIOD = millis / 10;
	}
	
	public void detectSequence(SensorsValues sv){
		if (sv != null && mGestureDetector != null) {
			mGestureDetector.detectGesture(sv);
		}
	}
	
	// Settings methods	
	protected void setGestureDetector(GestureDetector mGestureDetector){
		this.mGestureDetector = mGestureDetector;
	}
	
	protected void enableSensors(UDOOBLESensor... sensors){
	    mEnabledSensors.clear();
	    
	    for(UDOOBLESensor fs : sensors){
	    	mEnabledSensors.add(fs);
	    }
	}
	
	// Abstracts Methods
	public abstract void onGestureDetected(int gestureId);
	

}
