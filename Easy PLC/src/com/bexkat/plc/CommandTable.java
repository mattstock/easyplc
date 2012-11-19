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
	public static final String CREATE_TABLE = "CREATE TABLE "
			+ TABLE_NAME
			+ " ("
			+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ COLUMN_PROG_ID + " INTEGER NOT NULL,"
			+ COLUMN_CMD + " TEXT NOT NULL"
			+ ");";
	public static final String DROP_TABLE = "DROP TABLE IF EXISTS "
			+ TABLE_NAME;
	private SQLiteDatabase mDatabase;
	private DatabaseHelper dbHelper;
	private String[] allCommandColumns = {
			_ID,
			COLUMN_PROG_ID,
			COLUMN_CMD
	};

	public static void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE);
	}

	public static void onUpgrade(SQLiteDatabase db, int oldVersion,
			int newVersion) {
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

	public Command createCommand(long programId, String commandStr) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_PROG_ID, programId);
		values.put(COLUMN_CMD, commandStr);
		
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
		command.setCommand(cursor.getString(2));
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
		createCommand(newProgramId, cmd.getCommand());
	}

	public void deleteCommand(Command command) {
		long id = command.getId();
		mDatabase.delete(TABLE_NAME, _ID + " = " + id, null);
	}
}
