package com.aidilab.ble.interfaces;

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.aidilab.ble.R;
import com.aidilab.ble.common.BleDeviceInfo;
import com.aidilab.ble.fragment.ScanViewFragment;
import com.aidilab.ble.sensor.BleService;

public abstract class DevicesBLEScanActivity extends FragmentActivity {
	// Log
	private static final String TAG = "MainActivity";
	  
	private static final int NO_DEVICE = -1;
	private boolean mInitialised       = false;

	// permission
	private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
	
	//BLE management
	private boolean mBleSupported                     = true;
	private boolean mScanning                         = false;
	private int mNumDevs                              = 0;
	private int mConnIndex                            = NO_DEVICE;
	private List<BleDeviceInfo> mDeviceInfoList       = null;
	private static BluetoothManager mBluetoothManager = null;
	private BluetoothAdapter mBtAdapter               = null;
	protected BluetoothDevice mBluetoothDevice          = null;
	private BleService mBluetoothLeService      = null;
	private IntentFilter mFilter                      = null;
	private String [] mDeviceFilter                   = null;
	
	// Requests to other activities
	protected static final int REQ_ENABLE_BT  = 0;
	protected static final int REQ_DEVICE_ACT = 1;

	// GUI
	private static DevicesBLEScanActivity mThis = null;
	public ScanViewFragment mScanView;
	protected Intent mDeviceIntent;
	private static final int STATUS_DURATION = 5;
	
	public DevicesBLEScanActivity() {
		  Log.i(TAG, "Construct");
		  mThis = this;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Start the application
	    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_main);
	    
//	    getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.green_fizzly)));
//	    getActionBar().setIcon(android.R.color.transparent);
	    
	    // Use this check to determine whether BLE is supported on the device. Then
	    // you can selectively disable BLE-related features.
	    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
	      Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
	      mBleSupported = false;
	    }

	    // Initializes a Bluetooth adapter. For API level 18 and above, get a
	    // reference to BluetoothAdapter through BluetoothManager.
	    mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
	    mBtAdapter = mBluetoothManager.getAdapter();

	    // Checks if Bluetooth is supported on the device.
	    if (mBtAdapter == null) {
	      Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_LONG).show();
	      mBleSupported = false;
	    }
	    
	    // Initialize device list container and device filter
	    mDeviceInfoList = new ArrayList<BleDeviceInfo>();
	    Resources res = getResources();
	    mDeviceFilter = res.getStringArray(R.array.device_filter);
	    
	    mScanView = new ScanViewFragment();
	    
	    if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, mScanView).commit();
		}
	    
	    // Register the BroadcastReceiver
	    mFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
	    mFilter.addAction(BleService.ACTION_GATT_CONNECTED);
	    mFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			// Android M Permission check
			if (this.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("This app needs location access");
				builder.setMessage("Please grant location access so this app can detect beacons.");
				builder.setPositiveButton(android.R.string.ok, null);
				builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
							requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
						}
					}
				});
				builder.show();
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_COARSE_LOCATION: {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Log.d(TAG, "coarse location permission granted");
				} else {
					final AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle("Functionality limited");
					builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
					builder.setPositiveButton(android.R.string.ok, null);
					builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

						@Override
						public void onDismiss(DialogInterface dialog) {
						}

					});
					builder.show();
				}
				return;
			}
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();

	}
	
	@Override
	protected void onDestroy() {
	    Log.d(TAG, "Destroy");	    
	    if (mBluetoothLeService != null) {
	      scanLeDevice(false);
	      mBluetoothLeService.close();
	      unregisterReceiver(mReceiver);
	      unbindService(mServiceConnection);
	      mBluetoothLeService = null;
	    }
	    super.onDestroy();
	}
	  
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_activity_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		
		int id = item.getItemId();
		
		if(id == android.R.id.home){
		    onBackPressed();
		    return true;
		}
		else if(id == R.id.action_settings){
			return true;
		}
		else if(id == R.id.opt_bt){
			Intent settingsIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
		    startActivity(settingsIntent);
		}
		else if(id == R.id.opt_exit){
			finish();
		}
		else{
		    return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	
	public void onScanViewReady() {
		// Initial state of widgets
		updateGuiState();
		Log.i("BLEfromsensorTag", "mInitialize");
		if (!mInitialised) {
			// Broadcast receiver

		    Log.i("BLEfromsensorTag","DevicesBLEScanActivity() - sto per chiamare registerReceiver(mReceiver, mFilter)");
			registerReceiver(mReceiver, mFilter);
	
		    if (mBtAdapter.isEnabled()) {
		    	// Start straight away
		        startBluetoothLeService();
		    } else {
		        // Request BT adapter to be turned on
		        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		        startActivityForResult(enableIntent, REQ_ENABLE_BT);
		    }
		    mInitialised = true;
		 } else {
		    mScanView.notifyDataSetChanged();
		 }
	}
	
	public void onBtnScan(View view) {
		if (mScanning) {
			stopScan();
		} else {
			startScan();
		}
	}
	
	void onConnect() {
		if (mNumDevs > 0) {
			int connState = mBluetoothManager.getConnectionState(mBluetoothDevice, BluetoothGatt.GATT);
	
			switch (connState) {
		      case BluetoothGatt.STATE_CONNECTED:
		        mBluetoothLeService.disconnect(null);
		        break;
		      case BluetoothGatt.STATE_DISCONNECTED:
		        boolean ok = mBluetoothLeService.connect(mBluetoothDevice.getAddress());
		        if (!ok) {
		          setError("Connect failed");
		        }
		        break;
		      default:
		        setError("Device busy (connecting/disconnecting)");
		        break;
		     }
		}
	}
	
	private void startScan() {
		// Start device discovery
		if (mBleSupported) {
			mNumDevs = 0;
			mDeviceInfoList.clear();
			mScanView.notifyDataSetChanged();
		    scanLeDevice(true);
		    mScanView.updateGui(mScanning);
		    if (!mScanning) {
		    	setError("Device discovery start failed");
		        setBusy(false);
		    }
		} else {
			setError("BLE not supported on this device");
		}
	}
	
	private void stopScan() {
		mScanning = false;
		mScanView.updateGui(false);
		scanLeDevice(false);
	}
	
	/**
	 * mDeviceIntent = new Intent(this, TestFizzlyActivity.class);
	 *  mDeviceIntent.putExtra(TestFizzlyActivity.EXTRA_DEVICE, mBluetoothDevice);
	 *  startActivityForResult(mDeviceIntent, REQ_DEVICE_ACT); 
	 */
	protected abstract void startDeviceActivity();
	  
	private void stopDeviceActivity() {
	    finishActivity(REQ_DEVICE_ACT);
	}

	public void onDeviceClick(int pos) {
	    if (mScanning)
	      stopScan();

	    setBusy(true);
	    mBluetoothDevice = mDeviceInfoList.get(pos).getBluetoothDevice();
	    if (mConnIndex == NO_DEVICE) {
	      mScanView.setStatus("Connecting");
	      mConnIndex = pos;
	      onConnect();
	    } else {
	      mScanView.setStatus("Disconnecting");
	      if (mConnIndex != NO_DEVICE) {
	        mBluetoothLeService.disconnect(mBluetoothDevice.getAddress());
	      }
	    }
	}

	public void onScanTimeout() {
	    runOnUiThread(new Runnable() {
	      public void run() {
	        stopScan();
	      }
	    });
	}

	public void onConnectTimeout() {
	    runOnUiThread(new Runnable() {
	      public void run() {
	        setError("Connection timed out");
	      }
	    });
	    if (mConnIndex != NO_DEVICE) {
	      mBluetoothLeService.disconnect(mBluetoothDevice.getAddress());
	      mConnIndex = NO_DEVICE;
	    }
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////////////////
	  //
	  // GUI methods
	  //
	public void updateGuiState() {
	    boolean mBtEnabled = mBtAdapter.isEnabled();

	    if (mBtEnabled) {
	      if (mScanning) {
	        // BLE Host connected
	        if (mConnIndex != NO_DEVICE) {
	          String txt = mBluetoothDevice.getName() + " connected";
	          mScanView.setStatus(txt);
	        } else {
	          mScanView.setStatus(mNumDevs + " devices");
	        }
	      }
	    } else {
	      mDeviceInfoList.clear();
	      mScanView.notifyDataSetChanged();
	    }
	}

	private void setBusy(boolean f) {
	    mScanView.setBusy(f);
	  }

	void setError(String txt) {
	    mScanView.setError(txt);
	  }

	private BleDeviceInfo createDeviceInfo(BluetoothDevice device, int rssi) {
		BleDeviceInfo deviceInfo = new BleDeviceInfo(device, rssi);
		return deviceInfo;
	}

	private boolean checkDeviceFilter(BluetoothDevice device) {
		int  n = mDeviceFilter.length;
		if (n > 0) {
			boolean found = false;
			for (int i=0; i<n && !found; i++) {
				found = device.getName().equals(mDeviceFilter[i]);
			}
			return found;
		} else
		// Allow all devices if the device filter is empty
		return true;
	}

	  private void addDevice(BleDeviceInfo device) {
	    mNumDevs++;
	    mDeviceInfoList.add(device);
	    mScanView.notifyDataSetChanged();
	    if (mNumDevs > 1)
	      mScanView.setStatus(mNumDevs + " devices");
	    else
	      mScanView.setStatus("1 device");
	  }

	  private boolean deviceInfoExists(String address) {
	    for (int i = 0; i < mDeviceInfoList.size(); i++) {
	      if (mDeviceInfoList.get(i).getBluetoothDevice().getAddress().equals(address)) {
	        return true;
	      }
	    }
	    return false;
	  }

	  private BleDeviceInfo findDeviceInfo(BluetoothDevice device) {
	    for (int i = 0; i < mDeviceInfoList.size(); i++) {
	      if (mDeviceInfoList.get(i).getBluetoothDevice().getAddress().equals(device.getAddress())) {
	        return mDeviceInfoList.get(i);
	      }
	    }
	    return null;
	  }

	  private boolean scanLeDevice(boolean enable) {
	    if (enable) {
	      mScanning = mBtAdapter.startLeScan(mLeScanCallback);
	    } else {
	      mScanning = false;
	      mBtAdapter.stopLeScan(mLeScanCallback);
	    }
	    return mScanning;
	  }

	  public List<BleDeviceInfo> getDeviceInfoList() {
	    return mDeviceInfoList;
	  }

	  private void startBluetoothLeService() {
	    boolean f;

	    Intent bindIntent = new Intent(this, BleService.class);
	    startService(bindIntent);
	    f = bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
	    if (f)
	      Log.d(TAG, "BluetoothLeService - success");
	    else {
	      Toast.makeText(this, "Bind to BluetoothLeService failed", Toast.LENGTH_SHORT).show();
	      finish();
	    }
	  }
	  
	  // Activity result handling
	  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);

	    switch (requestCode) {
	    case REQ_DEVICE_ACT:
	      // When the device activity has finished: disconnect the device
	      if (mConnIndex != NO_DEVICE) {
	        mBluetoothLeService.disconnect(mBluetoothDevice.getAddress());
	      }
	      break;

	    case REQ_ENABLE_BT:
	      // When the request to enable Bluetooth returns
	      if (resultCode == Activity.RESULT_OK) {
	      	
	        Toast.makeText(this, R.string.bt_on, Toast.LENGTH_SHORT).show();
	      } else {
	        // User did not enable Bluetooth or an error occurred
	        Toast.makeText(this, R.string.bt_not_on, Toast.LENGTH_SHORT).show();
	        finish();
	      }
	      break;
	    default:
	      Log.e(TAG, "Unknown request code");
	      break;
	    }
	  }
	  
	  
	  // ////////////////////////////////////////////////////////////////////////////////////////////////
	  //
	  // Broadcasted actions from Bluetooth adapter and BluetoothLeService
	  //
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
	  	public void onReceive(Context context, Intent intent) {
	  		final String action = intent.getAction();
	  		
	  		
	  		if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
	  			// Bluetooth adapter state change
	  			switch (mBtAdapter.getState()) {
	  			case BluetoothAdapter.STATE_ON:
	  				mConnIndex = NO_DEVICE;
	  				startBluetoothLeService();
	  				break;
	  			case BluetoothAdapter.STATE_OFF:
	  				Toast.makeText(context, R.string.app_closing, Toast.LENGTH_LONG).show();
	  				finish();
	  				break;
	  			default:
	  				Log.w(TAG, "Action STATE CHANGED not processed ");
	  				break;
	  			}

	  			updateGuiState();
	  		} else if (BleService.ACTION_GATT_CONNECTED.equals(action)) {
	  			// GATT connect
	  			int status = intent.getIntExtra(BleService.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);
	  			if (status == BluetoothGatt.GATT_SUCCESS) {
	  				setBusy(false);
	  				startDeviceActivity();
	  			} else
	  				setError("Connect failed. Status: " + status);
	  		} else if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
	  			// GATT disconnect
	  			int status = intent.getIntExtra(BleService.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);
	  			stopDeviceActivity();
	  			if (status == BluetoothGatt.GATT_SUCCESS) {
	  				setBusy(false);
	  				mScanView.setStatus(mBluetoothDevice.getName() + " disconnected", STATUS_DURATION);
	  			} else {
	  				setError("Disconnect failed. Status: " + status);  				
	  			}
	  			mConnIndex = NO_DEVICE;
	  			mBluetoothLeService.close();
	  		} else {
	  			Log.w(TAG,"Unknown action: " + action);
	  		}

	  	}
	};

	// Code to manage Service life cycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

	    public void onServiceConnected(ComponentName componentName, IBinder service) {
	      mBluetoothLeService = ((BleService.LocalBinder) service).getService();
	      if (!mBluetoothLeService.initialize()) {
	        Log.e(TAG, "Unable to initialize BluetoothLeService");
	        finish();
	        return;
	      }
	      final int n = mBluetoothLeService.numConnectedDevices();
	      if (n > 0) {
	        runOnUiThread(new Runnable() {
	          public void run() {
	            mThis.setError("Multiple connections!");
	          }
	        });
	      } else {
	        startScan();
	        Log.i(TAG, "BluetoothLeService connected");
	      }
	    }

	    public void onServiceDisconnected(ComponentName componentName) {
	      mBluetoothLeService = null;
	      Log.i(TAG, "BluetoothLeService disconnected");
	    }
	};
	  
	  
	  // Device scan callback.
	  // NB! Nexus 4 and Nexus 7 (2012) only provide one scan result per scan
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

	    public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
	      runOnUiThread(new Runnable() {
	        public void run() {
	        	// Filter devices
//	        	if (checkDeviceFilter(device)) {
				if(true) {
					if (!deviceInfoExists(device.getAddress())) {
						// New device
						BleDeviceInfo deviceInfo = createDeviceInfo(device, rssi);
						addDevice(deviceInfo);
					} else {
						// Already in list, update RSSI info
						BleDeviceInfo deviceInfo = findDeviceInfo(device);
						deviceInfo.updateRssi(rssi);
						mScanView.notifyDataSetChanged();
					}
				}
	        }

	      });
	    }
	};
	  
	  
	 


}
