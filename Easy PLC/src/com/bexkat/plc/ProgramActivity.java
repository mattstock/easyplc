package com.bexkat.plc;

import java.util.List;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bexkat.plc.USBAccessory.USBAccessoryService;
import com.bexkat.plc.USBAccessory.USBAccessoryService.PLCBinder;
import com.bexkat.plc.compiler.ByteCompiler;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemLongClickListener;

public class ProgramActivity extends SherlockListActivity {
	private static final float STEPVAL = 5.0f;
	private USBAccessoryService plc;
	private ServiceConnection connection = new PLCServiceConnection();
	private ProgramTable mProgramDB;
	private ArrayAdapter<Command> mAdapter;
	private Command selectedCommand;
	private Program program;
	private ActionMode mActionMode;
	private long progid;
	private float x,y,z = 0;
	private BroadcastReceiver intentReceiver;
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.command, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (item.getItemId() == R.id.menu_command_delete) {
				mProgramDB.deleteCommand(program, selectedCommand);
				mAdapter.remove(selectedCommand);
				mAdapter.notifyDataSetChanged();
				mode.finish();
				return true;
			}
			mode.finish();
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.program);
		
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (mActionMode != null)
					return false;
				selectedCommand = (Command) parent.getItemAtPosition(position);
				// Start the CAB using the ActionMode.Callback defined above
				mActionMode = startActionMode(mActionModeCallback);
				view.setSelected(true);
				return true;
			}
		});

		mProgramDB = new ProgramTable(this);
		mProgramDB.open();
		
		// Grab program we're working on
		Intent intent = getIntent();
		// Make sure we return the program id value so it's added to the program list
		setResult(Activity.RESULT_OK, intent);
		progid = intent.getLongExtra("com.bexkat.plc.ProgramID", -1);
		if (progid == -1)
			finish();
		program = mProgramDB.getProgram(progid);
		// Pull command list from DB
		List<Command> commands = mProgramDB.getAllCommands(program);
		mAdapter = new ArrayAdapter<Command>(this,
				android.R.layout.simple_list_item_1, commands);
		setListAdapter(mAdapter);
		bindService(new Intent(getApplicationContext(), USBAccessoryService.class), connection,
				Context.BIND_AUTO_CREATE);
	}
	
    @Override
    protected void onResume() {
        super.onResume();
        
		mProgramDB.open();
		program = mProgramDB.getProgram(progid);
		
		IntentFilter updateFilter = new IntentFilter();
		updateFilter.addAction(USBAccessoryService.POSITION_INTENT);
		intentReceiver = new AccessoryServiceReceiver();
		registerReceiver(intentReceiver, updateFilter);
		
		// Bunch of UI updates
		EditText v = (EditText) findViewById(R.id.program_name);
		v.setText(program.getName());
		v = (EditText) findViewById(R.id.program_description);
		v.setText(program.getDescription());	
		v = (EditText) findViewById(R.id.x_position);
		v.setText(Float.toString(x));
        v = (EditText) findViewById(R.id.y_position);
		v.setText(Float.toString(y));
        v = (EditText) findViewById(R.id.z_position);
		v.setText(Float.toString(z));
    }

	@Override
	protected void onPause() {
		// Save any changes to the name or description
		TextView v = (TextView) findViewById(R.id.program_name);
		program.setName(v.getText().toString());
		mProgramDB.modifyName(program);
		v = (TextView) findViewById(R.id.program_description);		
		program.setDescription(v.getText().toString());
		mProgramDB.modifyDescription(program);
		
		unregisterReceiver(intentReceiver);
		
		mProgramDB.close();

		super.onPause();
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
	
	public void onClick(View v) {
		TextView tv;
		Command cmd;
		
		switch (v.getId()) {
		case R.id.home:
			plc.home();
			break;
		case R.id.init:
			plc.init();
			break;
		case R.id.reset:
			plc.reset();
			break;
		case R.id.download:
			plc.download(ByteCompiler.compile(mProgramDB.getAllCommands(program)));
			break;
		case R.id.xpos:
			// TODO need variable increments
			x += STEPVAL;
			plc.move(ByteCompiler.moveStep(ByteCompiler.MASK_X, STEPVAL));
			break;
		case R.id.xneg:
			x -= STEPVAL;
			plc.move(ByteCompiler.moveStep(ByteCompiler.MASK_X, -STEPVAL));
			break;
		case R.id.ypos:
			y += STEPVAL;
			plc.move(ByteCompiler.moveStep(ByteCompiler.MASK_Y, STEPVAL));
			break;
		case R.id.yneg:
			y -= STEPVAL;
			plc.move(ByteCompiler.moveStep(ByteCompiler.MASK_Y, -STEPVAL));
			break;
		case R.id.zpos:
			z += STEPVAL;
			plc.move(ByteCompiler.moveStep(ByteCompiler.MASK_Z, STEPVAL));
			break;
		case R.id.zneg:
			z -= STEPVAL;
			plc.move(ByteCompiler.moveStep(ByteCompiler.MASK_Z, -STEPVAL));
			break;
		case R.id.test_program:
			plc.move(ByteCompiler.compile(mProgramDB.getAllCommands(program)));
			break;
		case R.id.store_move:
			cmd = mProgramDB.addCommand(program, Command.TYPE_POS, x, y, z);
			mAdapter.add(cmd);
			mAdapter.notifyDataSetChanged(); 
			break;
		case R.id.relay_air:
			if (((ToggleButton) findViewById(R.id.relay_air)).isChecked()) {
				plc.relay(ByteCompiler.relayByte(1, Command.RELAY_AIR));				
				cmd = mProgramDB.addCommand(program, Command.TYPE_RELAY, 1, Command.RELAY_AIR);
			} else {
				plc.relay(ByteCompiler.relayByte(0, Command.RELAY_AIR));				
				cmd = mProgramDB.addCommand(program, Command.TYPE_RELAY, 0, Command.RELAY_AIR);
			}
			mAdapter.add(cmd);
			mAdapter.notifyDataSetChanged();			
			break;
		case R.id.relay_mold:
			if (((ToggleButton) findViewById(R.id.relay_mold)).isChecked()) {
				plc.relay(ByteCompiler.relayByte(1, Command.RELAY_MOULD));				
				cmd = mProgramDB.addCommand(program, Command.TYPE_RELAY, 1, Command.RELAY_MOULD);
			} else {
				plc.relay(ByteCompiler.relayByte(0, Command.RELAY_MOULD));				
				cmd = mProgramDB.addCommand(program, Command.TYPE_RELAY, 0, Command.RELAY_MOULD);
			}
			mAdapter.add(cmd);
			mAdapter.notifyDataSetChanged();			
			break;
		}
	}
	
	private class AccessoryServiceReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			EditText v;
			
			Bundle b = intent.getExtras();
			String action = intent.getAction();
			
			if (action.equals(USBAccessoryService.POSITION_INTENT)) {
				switch (b.getInt("axis")) {
				case 0:
					x = b.getInt("position") / 100.0f;
					v = (EditText) findViewById(R.id.x_position);
					v.setText(Float.toString(x));
					break;
				case 1:
					y = b.getInt("position") / 100.0f;
					v = (EditText) findViewById(R.id.y_position);
					v.setText(Float.toString(y));
					break;
				case 2:
					z = b.getInt("position") / 100.0f;
					v = (EditText) findViewById(R.id.z_position);
					v.setText(Float.toString(z));
					break;
				default:
					// TODO - add relay support
					break;
				}
			}
		}
	}
}
