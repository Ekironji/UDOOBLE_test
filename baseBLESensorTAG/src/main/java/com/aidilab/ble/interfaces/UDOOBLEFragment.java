package com.aidilab.ble.interfaces;

import android.app.Fragment;
import android.view.View.OnClickListener;

import com.aidilab.ble.utils.SensorsValues;

import java.text.DecimalFormat;

public abstract class UDOOBLEFragment extends Fragment implements OnClickListener{

	public static UDOOBLEFragment mInstance = null;
	
	// Views elements
	
	// House-keeping
	private DecimalFormat decimal = new DecimalFormat("+0.00;-0.00");
	private FizzlyActivity mActivity;
	
//	@Override
//	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//	    mInstance = this;
//	    mActivity = (FizzlyActivity) getActivity();
//       
//	    View view = inflater.inflate(R.layout.fragment_device, container, false);
//	    	
//	    /**   GUI initialization - getting gui references */
//		
//	    mActivity.onViewInflated(view);
//	
//	    return view;
//	}
		
	public abstract void onCharacteristicChanged(String uuidStr, byte[] rawValue);
	public abstract void onCharacteristicChanged(String uuidStr, SensorsValues sv);
	public abstract void onGestureDetected(int gestureId);

}
