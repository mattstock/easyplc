package com.bexkat.plc.USBAccessory;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
	private static final BlockingQueue<AccessoryCommand> queue = new LinkedBlockingQueue<AccessoryCommand>();
	protected MessagingTask messenger;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "USB service onCreate()");
		mUSBManager = (UsbManager) getSystemService(Context.USB_SERVICE);

		// So we know when it's disconnected
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);
		messenger = new MessagingTask();
		messenger.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mUsbReceiver);
		Log.d(TAG, "USB service onDestroy()");
	}

	public void init() {
		queue.add(new AccessoryCommand(AccessoryCommandType.INIT));
	}

	public void home() {
		queue.add(new AccessoryCommand(AccessoryCommandType.HOME));
	}

	public void reset() {
		queue.add(new AccessoryCommand(AccessoryCommandType.ALARM_RESET));
	}

	public void status() {
		queue.add(new AccessoryCommand(AccessoryCommandType.STATUS));
	}

	public void move(List<MoveResult> data) {
		queue.add(new AccessoryCommand(AccessoryCommandType.MOVE, data));
	}

	public void relay(byte data) {
		queue.add(new AccessoryCommand(AccessoryCommandType.RELAY, data));
	}

	public void download(List<MoveResult> data) {
		queue.add(new AccessoryCommand(AccessoryCommandType.DOWNLOAD, data));
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

	// This thread pulls messages off the the send queue, and based on the
	// type, waits for a response in a synchronous manner.
	protected class MessagingTask extends
			AsyncTask<InputStream, AccessoryResult, Void> {
		private FileInputStream is;
		private FileOutputStream os;
		private ParcelFileDescriptor pfd;
		boolean stopped = false;

		@Override
		protected void onPreExecute() {
			UsbAccessory[] accessories = mUSBManager.getAccessoryList();
			mAccessory = (accessories == null ? null : accessories[0]);
			if (mAccessory == null) {
				stopped = true;
				return;
			}
			if ((pfd = mUSBManager.openAccessory(mAccessory)) != null) {
				FileDescriptor fd = pfd.getFileDescriptor();
				if (fd.valid()) {
					is = new FileInputStream(fd);
					os = new FileOutputStream(fd);
				} else {
					Log.d(TAG, "FD for accessory not valid!");
					return;
				}
			} else {
				Log.d(TAG, "openAccessory failed!");
				return;
			}
		}

		@Override
		protected Void doInBackground(InputStream... params) {
			byte[] buffer = new byte[64];
			int res = 0;

			try {
				Log.d(TAG, "Starting read loop");
				while (!isCancelled() || stopped) {
					AccessoryCommand cmd = queue.take();
					switch (cmd.getType()) {
					case INIT:
						os.write('i');
						res = is.read(buffer, 0, buffer.length);
						break;
					case RELAY:
						os.write('a');
						os.write(cmd.getData());
						res = is.read(buffer, 0, buffer.length);
						break;
					case ALARM_RESET:
						os.write('r');
						res = is.read(buffer, 0, buffer.length);
						break;
					case HOME:
						os.write('h');
						os.write(cmd.getData());
						res = is.read(buffer, 0, buffer.length);
						break;
					case MOVE:
						os.write('c');
						for (byte s: cmd.getData()) {
							os.write(s);
							res = is.read(buffer, 0, buffer.length);
							if (res < 0 || buffer[0] != 'K')
								break;
						}
						break;
					case STATUS:
						os.write('s');
						res = is.read(buffer, 0, buffer.length);
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
				Log.e(TAG, "listener read: " + e.getMessage());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(AccessoryResult... values) {
			if (values.length > 0) {
				switch (values[0].getType()) {
				case INIT:
				case RELAY:
				case MOVE:
				case STATUS:
				case ALARM_RESET:
				case HOME:
				case DOWNLOAD:
				}
			}
		}

		@Override
		protected void onCancelled() {
			Log.i(TAG, "ListenerTask cancelled");
			try {
				if (is != null)
					is.close();
				if (os != null)
					os.close();
			} catch (IOException e) {
				Log.e(TAG, "Close: " + e.getMessage());
			}
			is = null;
			os = null;

			if (pfd != null) {
				try {
					pfd.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
