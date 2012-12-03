package com.bexkat.plc.USBAccessory;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.bexkat.plc.compiler.MoveResult;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class USBAccessoryService extends Service {
	private static final String TAG = "USBAccessoryService";
	private BroadcastReceiver mUsbReceiver = new UsbReceiver();
	private UsbManager mUSBManager;
	private UsbAccessory mAccessory;
	private final IBinder mBinder = new PLCBinder();
	private static final String ACTION_USB_PERMISSION = "com.bexkat.plc.action.USB_PERMISSION";
	public static final String POSITION_INTENT = "com.bexkat.plc.position";
	private static final BlockingQueue<AccessoryCommand> queue = new LinkedBlockingQueue<AccessoryCommand>();
	protected ListenerTask listener;
	protected WriterTask writer;
	private ParcelFileDescriptor pfd;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "USB service onCreate()");
		mUSBManager = (UsbManager) getSystemService(Context.USB_SERVICE);

		// So we know when it's disconnected
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);
		startMessenger();
	}

	private void startMessenger() {
		FileDescriptor fd;

		if (pfd == null) {
			UsbAccessory[] accessories = mUSBManager.getAccessoryList();
			mAccessory = (accessories == null ? null : accessories[0]);
			if (mAccessory == null)
				return;
			if ((pfd = mUSBManager.openAccessory(mAccessory)) == null) {
				Log.d(TAG, "openAccessory failed!");
				return;
			}
			fd = pfd.getFileDescriptor();
			if (!fd.valid()) {
				Log.d(TAG, "FD for accessory not valid!");
				return;
			}
			if (listener != null)
				listener.cancel(true);
			listener = new ListenerTask();
			listener.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
					new FileInputStream(fd));
			if (writer != null)
				writer.cancel(true);
			writer = new WriterTask();
			writer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
					new FileOutputStream(fd));
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		listener.cancel(true);
		unregisterReceiver(mUsbReceiver);
		Log.d(TAG, "USB service onDestroy()");
	}

	public void init() {
		startMessenger();
		queue.add(new AccessoryCommand(AccessoryCommandType.INIT));
	}

	public void home() {
		startMessenger();
		queue.add(new AccessoryCommand(AccessoryCommandType.HOME));
	}

	public void reset() {
		startMessenger();
		queue.add(new AccessoryCommand(AccessoryCommandType.ALARM_RESET));
	}

	public void relay(byte cmd) {
		startMessenger();
		queue.add(new AccessoryCommand(AccessoryCommandType.RELAY, cmd));
	}

	public void move(List<MoveResult> data) {
		startMessenger();
		queue.add(new AccessoryCommand(AccessoryCommandType.MOVE, data));
	}

	public void download(List<MoveResult> data) {
		startMessenger();
		// TODO
	}

	private class UsbReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = (UsbAccessory) intent
						.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					Log.i(TAG, "closing accessory");
					listener.cancel(true);
					try {
						pfd.close();
						pfd = null;
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class PLCBinder extends Binder {
		public USBAccessoryService getService() {
			return USBAccessoryService.this;
		}
	}

	protected class ListenerTask extends
			AsyncTask<FileInputStream, AccessoryResult, Void> {
		FileInputStream is;

		@Override
		protected Void doInBackground(FileInputStream... params) {
			byte[] buffer = new byte[64];
			int res = 0;
			int index = 0;
			is = params[0];

			Log.d(TAG, "Starting read loop");
			try {
				while (!isCancelled()) {
					res = is.read(buffer, index, buffer.length);
					if (res < 0)
						break;
					index = index + res;
					if (index == 5) {
						publishProgress(new AccessoryResult(buffer));
						index = 0;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(AccessoryResult... values) {
			Bundle b = new Bundle();
			Intent i;

			if (values.length > 0) {
				b.putInt("axis", values[0].getAxis());
				b.putInt("position", values[0].getPosition());
				i = new Intent(POSITION_INTENT);
				i.putExtras(b);
				sendBroadcast(i, null);
			}
		}

		@Override
		protected void onCancelled() {
			Log.i(TAG, "ListenerTask cancelled");
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
				Log.e(TAG, "Close: " + e.getMessage());
			}
			is = null;
		}
	}

	// This thread pulls messages off the the send queue, and based on the
	// type, waits for a response in a synchronous manner.
	protected class WriterTask extends
			AsyncTask<FileOutputStream, AccessoryResult, Void> {
		private FileOutputStream os;

		@Override
		protected Void doInBackground(FileOutputStream... params) {
			int res = 0;
			FileOutputStream os = params[0];
			try {
				Log.d(TAG, "Starting write loop");
				while (!isCancelled()) {
					AccessoryCommand cmd = queue.take();
					switch (cmd.getType()) {
					case INIT:
						os.write('i');
						os.write('0');
						os.write('i');
						os.write('1');
						os.write('i');
						os.write('2');
						break;
					case RELAY:
						os.write('c');
						os.write(cmd.getData());
						break;
					case ALARM_RESET:
						os.write('r');
						break;
					case HOME:
						os.write('h');
						os.write('0');
						os.write('h');
						os.write('1');
						os.write('h');
						os.write('2');
						break;
					case MOVE:
						for (byte s : cmd.getData()) {
							os.write('c');
							os.write(s);
						}
						break;
					default:
						res = 0;
						break;
					}
					if (res < 0) {
						break;
					}
				}
			} catch (IOException e) {
				Log.e(TAG, "WriterTask: " + e.getMessage());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onCancelled() {
			Log.i(TAG, "WriterTask cancelled");
			try {
				if (os != null)
					os.close();
			} catch (IOException e) {
				Log.e(TAG, "Close: " + e.getMessage());
			}
			os = null;
		}
	}
}
