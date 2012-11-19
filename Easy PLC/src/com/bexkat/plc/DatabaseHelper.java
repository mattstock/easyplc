package com.bexkat.plc;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "dbprog";

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
        ProgramTable.onCreate(db);
        CommandTable.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        ProgramTable.onUpgrade(db, oldVersion, newVersion);
        CommandTable.onUpgrade(db, oldVersion, newVersion);
	}

}
