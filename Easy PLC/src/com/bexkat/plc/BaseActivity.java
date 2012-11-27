package com.bexkat.plc;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.bexkat.plc.USBAccessory.USBAccessoryService;
import com.bexkat.plc.USBAccessory.USBAccessoryService.PLCBinder;

public class BaseActivity extends SherlockFragmentActivity {
	private static final String TAG = "BaseActivity";
	private USBAccessoryService plc;
	private ServiceConnection connection = new PLCServiceConnection();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);
		bindService(new Intent(getApplicationContext(), USBAccessoryService.class), connection,
				Context.BIND_AUTO_CREATE);
		}
	
	@Override
	protected void onDestroy() {
		unbindService(connection);
		super.onDestroy();
	}
	
	private class PLCServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className, IBinder service) {
			PLCBinder binder = (PLCBinder) service;
			plc = binder.getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			plc = null;
		}
	}
	
}
