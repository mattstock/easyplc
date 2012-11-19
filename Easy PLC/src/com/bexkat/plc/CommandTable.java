package com.bexkat.plc;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class CommandTable implements BaseColumns {
	public static final String TABLE_NAME = "commands";
	public static final String COLUMN_PROG_ID = "program";
	public static final String COLUMN_CMD = "command";
	public static final String COLUMN_STATE = "state";
	public static final String COLUMN_RELAY = "relay";
	public static final String COLUMN_X = "x";
	public static final String COLUMN_Y = "y";
	public static final String COLUMN_Z = "z";
	public static final String CREATE_TABLE = "CREATE TABLE "
			+ TABLE_NAME
			+ " ("
			+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ COLUMN_PROG_ID + " INTEGER NOT NULL,"
			+ COLUMN_CMD + " INTEGER NOT NULL,"
			+ COLUMN_STATE + " INTEGER,"
			+ COLUMN_RELAY + " INTEGER,"
			+ COLUMN_X + " REAL,"
			+ COLUMN_Y + " REAL,"
			+ COLUMN_Z + " REAL"
			+ ");";
	public static final String DROP_TABLE = "DROP TABLE IF EXISTS "
			+ TABLE_NAME;
	private SQLiteDatabase mDatabase;
	private DatabaseHelper dbHelper;
	private String[] allCommandColumns = {
			_ID,
			COLUMN_PROG_ID,
			COLUMN_CMD,
			COLUMN_STATE,
			COLUMN_RELAY,
			COLUMN_X,
			COLUMN_Y,
			COLUMN_Z
	};

	public static void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE);
	}

	public static void onUpgrade(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		db.execSQL(DROP_TABLE);
		db.execSQL(CREATE_TABLE);
	}
	
	public CommandTable(Context context) {
		dbHelper = new DatabaseHelper(context);
	}
	
	public void open() throws SQLException {
		mDatabase = dbHelper.getWritableDatabase();
	}
	
	public void close() {
		dbHelper.close();
	}

	// Convenience function
	public Command createCommand(long programId, int cmd, int state, int relay) {
		if (cmd != Command.TYPE_RELAY)
			return null;
		return createCommand(programId, cmd, state, relay, 0, 0, 0);
	}
	
	// Convenience function
	public Command createCommand(long programId, int cmd, float x, float y, float z) {
		if (cmd != Command.TYPE_POS)
			return null;
		return createCommand(programId, cmd, 0, 0, x, y, z);
	}
	
	public Command createCommand(long programId, int cmd, int state, int relay, float x, float y, float z) {
		ContentValues values = new ContentValues();
		
		values.put(COLUMN_PROG_ID, programId);
		values.put(COLUMN_CMD, cmd);
		
		switch (cmd) {
		case Command.TYPE_POS:
			values.put(COLUMN_X, x);
			values.put(COLUMN_Y, y);
			values.put(COLUMN_Z, z);
			break;
		case Command.TYPE_RELAY:
			values.put(COLUMN_STATE, state);
			values.put(COLUMN_RELAY, relay);
			break;
		default:
		} 
		
		long insertId = mDatabase.insert(TABLE_NAME, null, values);
		Cursor cursor = mDatabase.query(TABLE_NAME, allCommandColumns, 
				_ID + " = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		Command newCommand = cursorToCommand(cursor);
		cursor.close();
		return newCommand;
	}

	private Command cursorToCommand(Cursor cursor) {
		Command command = new Command();
		command.setId(cursor.getLong(0));
		command.setProgram(cursor.getLong(1));
		command.setCommand(cursor.getInt(2));
		if (!cursor.isNull(3))
			command.setState(cursor.getInt(3));
		if (!cursor.isNull(4))
			command.setRelay(cursor.getInt(4));
		if (!cursor.isNull(5))
			command.setX(cursor.getFloat(5));
		if (!cursor.isNull(6))
			command.setY(cursor.getFloat(6));
		if (!cursor.isNull(7))
			command.setZ(cursor.getFloat(7));		
		return command;
	}

	public List<Command> getAllCommands(long programId) {
		ArrayList<Command> cmds = new ArrayList<Command>();
		
		Cursor cursor = mDatabase.query(TABLE_NAME, allCommandColumns, 
				COLUMN_PROG_ID + " = " + programId, null, null, null, _ID + " ASC");
		cursor.moveToFirst();
		while (cursor.isAfterLast() == false) {
			cmds.add(cursorToCommand(cursor));
			cursor.moveToNext();
		}
		cursor.close();		
		
		return cmds;
	}

	public void copyCommand(Command cmd, long newProgramId) {
		createCommand(newProgramId, cmd.getCommand(), cmd.getState(), cmd.getRelay(), cmd.getX(), cmd.getY(), cmd.getZ());
	}

	public void deleteCommand(Command command) {
		long id = command.getId();
		mDatabase.delete(TABLE_NAME, _ID + " = " + id, null);
	}
}
