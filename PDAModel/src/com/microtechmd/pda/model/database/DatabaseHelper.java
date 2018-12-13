package com.microtechmd.pda.model.database;


import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DatabaseHelper extends SQLiteOpenHelper
{
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "pda.db";


	public DatabaseHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}


	@Override
	public void onCreate(SQLiteDatabase db)
	{
		try
		{
//			db.execSQL("CREATE TABLE " + DataSetHistory.TABLE_NAME + " (" +
//				DataSetHistory.FIELD_ID +
//				" INTEGER PRIMARY KEY AUTOINCREMENT," +
//				DataSetHistory.FIELD_RF_ADDRESS + " TEXT," +
//				DataSetHistory.FIELD_DATE_TIME + " TEXT," +
//				DataSetHistory.FIELD_STATUS_SHORT0 + " INTEGER," +
//				DataSetHistory.FIELD_EVENT_INDEX + " INTEGER," +
//				DataSetHistory.FIELD_EVENT_VALUE + " INTEGER);");

		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}


	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		try
		{
//			db.execSQL("DROP TABLE IF EXISTS " + DataSetHistory.TABLE_NAME);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		onCreate(db);
	}
}
