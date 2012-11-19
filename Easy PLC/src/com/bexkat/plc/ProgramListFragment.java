package com.bexkat.plc;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemLongClickListener;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class ProgramListFragment extends SherlockListFragment {
	private static final int PROGRAM_ADD = 1;
	private static final int PROGRAM_EDIT = 2;
	private ArrayAdapter<Program> mAdapter;
	private ProgramTable mProgramDB;
	private Program selectedProgram;
	private ActionMode mActionMode;
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.program, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (item.getItemId() == R.id.menu_program_delete) {
				mProgramDB.deleteProgram(selectedProgram);
				mAdapter.remove(selectedProgram);
				mAdapter.notifyDataSetChanged();
				mode.finish();
				return true;
			}
			if (item.getItemId() == R.id.menu_program_edit) {
				Intent intent = new Intent(getActivity(), ProgramActivity.class);
				intent.putExtra("com.bexkat.plc.ProgramID", selectedProgram.getId());
				startActivityForResult(intent, PROGRAM_EDIT);
				mode.finish();
				return true;
			}
			if (item.getItemId() == R.id.menu_program_copy) {
				Program newProg = mProgramDB.copyProgram(selectedProgram);
				mAdapter.add(newProg);
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
		setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(Bundle saveInstanceState) {
		super.onActivityCreated(saveInstanceState);
		mProgramDB = new ProgramTable(getActivity());
		mProgramDB.open();
		List<Program> programs = mProgramDB.getAllPrograms();
		mAdapter = new ArrayAdapter<Program>(getActivity(),
				android.R.layout.simple_list_item_1, programs);
		setListAdapter(mAdapter);

		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (mActionMode != null)
					return false;
				selectedProgram = (Program) parent.getItemAtPosition(position);
				// Start the CAB using the ActionMode.Callback defined above
				mActionMode = getSherlockActivity().startActionMode(
						mActionModeCallback);
				view.setSelected(true);
				return true;
			}
		});
	}

	@Override
	public void onResume() {
		mProgramDB.open();
		super.onResume();
	}

	@Override
	public void onPause() {
		mProgramDB.close();
		super.onPause();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// TODO
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.home, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_program:
			Program program = mProgramDB.createProgram("New Program",
					"New Description");

			Intent intent = new Intent(getActivity(), ProgramActivity.class);
			intent.putExtra("com.bexkat.plc.ProgramID", program.getId());
			startActivityForResult(intent, PROGRAM_ADD);
			return true;
		}
		return false;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		mProgramDB.open();
		if (requestCode == PROGRAM_ADD) {
			if (resultCode == Activity.RESULT_OK) {
				long programId = data.getLongExtra(
						"com.bexkat.plc.ProgramID", 0);
				Program program = mProgramDB.getProgram(programId);
				mAdapter.add(program);
				mAdapter.notifyDataSetChanged();
			}
		}
		if (requestCode == PROGRAM_EDIT) {
			if (resultCode == Activity.RESULT_OK) {
				long programId = data.getLongExtra(
						"com.bexkat.plc.ProgramID", 0);
				Program newProgram = mProgramDB.getProgram(programId);
				mAdapter.remove(selectedProgram);
				mAdapter.add(newProgram);
				mAdapter.notifyDataSetChanged();
			}
		}
	}
}
