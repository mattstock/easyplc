package com.bexkat.plc;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class ProgramTable implements BaseColumns {
	public static final String TABLE_NAME = "programs";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_DESC = "description";
	public static final String CREATE_TABLE = "CREATE TABLE "
			+ TABLE_NAME 
			+ " (" 
			+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ COLUMN_NAME + " TEXT NOT NULL,"
			+ COLUMN_DESC + " TEXT"
			+ ");";
	public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
	private SQLiteDatabase mDatabase;
	private DatabaseHelper dbHelper;
	private CommandTable cmdDB;
	private String[] allProgramColumns = {
			_ID,
			COLUMN_NAME,
			COLUMN_DESC
	};
	
	public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
	}

	public static void onUpgrade(SQLiteDatabase db, int oldVersion,
			int newVersion) {			
	}

	public ProgramTable(Context context) {
		cmdDB = new CommandTable(context);
		dbHelper = new DatabaseHelper(context);
	}
	
	public void open() throws SQLException {
		mDatabase = dbHelper.getWritableDatabase();
		cmdDB.open();
	}
	
	public void close() {
		cmdDB.close();
		dbHelper.close();
	}
	
	public Program createProgram(String name, String description) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_NAME, name);
		values.put(COLUMN_DESC, description);
		long insertId = mDatabase.insert(TABLE_NAME, null, values);
		Cursor cursor = mDatabase.query(TABLE_NAME, allProgramColumns, 
				_ID + " = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		Program newProgram = cursorToProgram(cursor);
		cursor.close();
		return newProgram;
	}

	public Program copyProgram(Program oldProgram) {
		Program newProgram = createProgram("copy of " + oldProgram.getName(), oldProgram.getDescription());		

		for (Command cmd : cmdDB.getAllCommands(oldProgram.getId())) {
			cmdDB.copyCommand(cmd, newProgram.getId());
		}
		
		return newProgram;
	}
	
	public void modifyName(Program program) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_NAME, program.getName());
		String strFilter = _ID + " = " + program.getId();
		mDatabase.update(TABLE_NAME, values, strFilter, null);
	}
	
	public void modifyDescription(Program program) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_DESC, program.getDescription());
		String strFilter = _ID + " = " + program.getId();
		mDatabase.update(TABLE_NAME, values, strFilter, null);		
	}
	
	public Program getProgram(long id) {
		Cursor cursor = mDatabase.query(TABLE_NAME, allProgramColumns, 
				_ID + " = " + id, null, null, null, null);
		cursor.moveToFirst();
		Program newProgram = cursorToProgram(cursor);
		cursor.close();
		return newProgram;		
	}
	
	public List<Command> getAllCommands(Program program) {
		return cmdDB.getAllCommands(program.getId());
	}
	
	public void deleteProgram(Program program) {
		long id = program.getId();
		mDatabase.delete(TABLE_NAME, _ID + " = " + id, null);
	}
	
	public List<Program> getAllPrograms() {
		List<Program> programs = new ArrayList<Program>();
		
		Cursor cursor = mDatabase.query(TABLE_NAME, allProgramColumns,
				null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Program program = cursorToProgram(cursor);
			programs.add(program);
			cursor.moveToNext();
		}
		cursor.close();
		return programs;
	}
	
	private Program cursorToProgram(Cursor cursor) {
		Program program = new Program();
		program.setId(cursor.getLong(0));
		program.setName(cursor.getString(1));
		program.setDescription(cursor.getString(2));
		return program;
	}

	public Command addCommand(Program program, String command) {
		return cmdDB.createCommand(program.getId(), command);
	}

	public void deleteCommand(Program mProgram, Command command) {
		cmdDB.deleteCommand(command);		
	}
}
