package com.microtechmd.pda.model.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;

import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.comm.RFAddress;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.utility.LogPDA;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;


public class DataSetHistory
{
	public static final String TABLE_NAME = "history";
	public static final String FIELD_ID = "_id";
	public static final String FIELD_RF_ADDRESS = "rf_address";
	public static final String FIELD_DATE_TIME = "date_time";
	public static final String FIELD_STATUS_BYTE0 = "status_byte0";
	public static final String FIELD_STATUS_BYTE1 = "status_byte1";
	public static final String FIELD_STATUS_SHORT0 = "status_short0";
	public static final String FIELD_STATUS_SHORT1 = "status_short1";
	public static final String FIELD_EVENT_INDEX = "event_index";
	public static final String FIELD_EVENT_PORT = "event_port";
	public static final String FIELD_EVENT_TYPE = "event_type";
	public static final String FIELD_EVENT_URGENCY = "event_urgency";
	public static final String FIELD_EVENT_VALUE = "event_value";


	boolean mIsOneLimit = false;
	String mRFAddress = RFAddress.RF_ADDRESS_UNPAIR;
	DatabaseHelper mDatabaseHelper = null;
	LogPDA mLog = null;


	public DataSetHistory(Context context)
	{
		if (mDatabaseHelper == null)
		{
			mDatabaseHelper = new DatabaseHelper(context);
			mLog = new LogPDA();
		}
	}


	public void setRFAddress(final String address)
	{
		mRFAddress = address;
	}


	public void insertHistory(final History history)
	{
		if (history == null)
		{
			return;
		}

		Status status = history.getStatus();
		Event event = history.getEvent();
		ContentValues contentValues = new ContentValues();
		contentValues.put(FIELD_RF_ADDRESS, mRFAddress);
		contentValues.put(FIELD_DATE_TIME, history.getDateTime().getBCD());
		contentValues.put(FIELD_STATUS_BYTE0, status.getByteValue1());
		contentValues.put(FIELD_STATUS_BYTE1, status.getByteValue2());
		contentValues.put(FIELD_STATUS_SHORT0, status.getShortValue1());
		contentValues.put(FIELD_STATUS_SHORT1, status.getShortValue2());
		contentValues.put(FIELD_EVENT_INDEX, event.getIndex());
		contentValues.put(FIELD_EVENT_PORT, event.getPort());
		contentValues.put(FIELD_EVENT_TYPE, event.getEvent());
		contentValues.put(FIELD_EVENT_URGENCY, event.getUrgency());
		contentValues.put(FIELD_EVENT_VALUE, event.getValue());

		try
		{
			SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
			database.insert(TABLE_NAME, null, contentValues);
			database.close();
		}
		catch (SQLiteException e)
		{
			e.printStackTrace();
		}
	}


	public DataList queryHistory(final DataList filter)
	{
		DataList dataList = new DataList();

		try
		{
			mIsOneLimit = false;
			String queryString = buildQuery(buildFilter(filter));

			if (mIsOneLimit)
			{
				queryString += " LIMIT 1";
			}

			mLog.Debug(getClass(), queryString);
			SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
			Cursor cursor = database.rawQuery(queryString, null);
			History history = new History();

			while (cursor.moveToNext())
			{
				history = getHistory(cursor, history);
				dataList.pushData(history.getByteArray());
			}

			cursor.close();
			database.close();
		}
		catch (SQLiteException e)
		{
			e.printStackTrace();
		}

		return dataList;
	}


	public void exportHistory(final DataList filter)
	{
		final String FILE_PATH = Environment.getExternalStorageDirectory() +
			"/" + History.class.getSimpleName() + ".csv";
		final String SPLIT_TAG = ",";


		if (Environment.getExternalStorageState()
			.equals(Environment.MEDIA_MOUNTED))
		{
			try
			{
				BufferedWriter bufferdWriter =
					new BufferedWriter(new FileWriter(FILE_PATH));
				bufferdWriter
					.write(FIELD_RF_ADDRESS + SPLIT_TAG + FIELD_DATE_TIME +
						SPLIT_TAG + FIELD_STATUS_BYTE0 + SPLIT_TAG +
						FIELD_STATUS_BYTE1 + SPLIT_TAG + FIELD_STATUS_SHORT0 +
						SPLIT_TAG + FIELD_STATUS_SHORT1 + SPLIT_TAG +
						FIELD_EVENT_INDEX + SPLIT_TAG + FIELD_EVENT_PORT +
						SPLIT_TAG + FIELD_EVENT_TYPE + SPLIT_TAG +
						FIELD_EVENT_URGENCY + SPLIT_TAG + FIELD_EVENT_VALUE);
				bufferdWriter.newLine();

				try
				{
					mIsOneLimit = false;
					String queryString = buildQuery(buildFilter(filter));

					if (mIsOneLimit)
					{
						queryString += " LIMIT 1";
					}

					SQLiteDatabase database =
						mDatabaseHelper.getReadableDatabase();
					Cursor cursor = database.rawQuery(queryString, null);

					if (cursor.moveToLast())
					{
						do
						{
							bufferdWriter.write(cursor.getString(
								cursor.getColumnIndex(FIELD_RF_ADDRESS)) +
								SPLIT_TAG +
								cursor.getLong(
									cursor.getColumnIndex(FIELD_DATE_TIME)) +
								SPLIT_TAG +
								cursor.getInt(
									cursor.getColumnIndex(FIELD_STATUS_BYTE0)) +
								SPLIT_TAG +
								cursor.getInt(
									cursor.getColumnIndex(FIELD_STATUS_BYTE1)) +
								SPLIT_TAG +
								cursor.getInt(cursor
									.getColumnIndex(FIELD_STATUS_SHORT0)) +
								SPLIT_TAG +
								cursor.getInt(cursor
									.getColumnIndex(FIELD_STATUS_SHORT1)) +
								SPLIT_TAG +
								cursor.getInt(
									cursor.getColumnIndex(FIELD_EVENT_INDEX)) +
								SPLIT_TAG +
								cursor.getInt(
									cursor.getColumnIndex(FIELD_EVENT_PORT)) +
								SPLIT_TAG +
								cursor.getInt(
									cursor.getColumnIndex(FIELD_EVENT_TYPE)) +
								SPLIT_TAG +
								cursor.getInt(cursor
									.getColumnIndex(FIELD_EVENT_URGENCY)) +
								SPLIT_TAG + cursor.getInt(
									cursor.getColumnIndex(FIELD_EVENT_VALUE)));
							bufferdWriter.newLine();
							bufferdWriter.flush();
						}
						while (cursor.moveToPrevious());
					}

					cursor.close();
					database.close();
				}
				catch (SQLiteException e)
				{
					e.printStackTrace();
				}

				bufferdWriter.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}


	private String buildQuery(String filterString)
	{
		String queryString = "SELECT * FROM " + TABLE_NAME;
		queryString += filterString;
		queryString +=
			" ORDER BY " + FIELD_DATE_TIME + " DESC, " + FIELD_ID + " DESC";

		return queryString;
	}


	private String buildFilter(final DataList filter)
	{
		String filterString = "";

		if ((filter != null) && (filter.getCount() > 0))
		{
			if (filter.getCount() == 1)
			{
				History history = new History(filter.getData(0));
				filterString += buildFilterDateTime(history.getDateTime(), "=");
				filterString += buildFilterStatus(history.getStatus(), "=");
				filterString += buildFilterEvent(history.getEvent(), "=");
			}
			else
			{
				History history = new History(filter.getData(0));
				filterString +=
					buildFilterDateTime(history.getDateTime(), ">=");
				filterString += buildFilterStatus(history.getStatus(), ">=");
				filterString += buildFilterEvent(history.getEvent(), ">=");
				history.setByteArray(filter.getData(1));
				filterString += buildFilterDateTime(history.getDateTime(), "<");
				filterString += buildFilterStatus(history.getStatus(), "<");
				filterString += buildFilterEvent(history.getEvent(), "<");
			}

			if (!filterString.equals(""))
			{
				filterString = filterString.replaceFirst("AND", "WHERE");
			}
		}

		return filterString;
	}


	private String buildFilterDateTime(final DateTime dateTime,
		final String operator)
	{
		String filterString = "";
		DecimalFormat decimalFormat = new DecimalFormat("#");

		if (operator.equals("="))
		{
			decimalFormat.setMinimumIntegerDigits(2);

			if (dateTime.getYear() != -1)
			{
				filterString += decimalFormat
					.format(DateTime.YEAR_BASE + dateTime.getYear());
			}
			else
			{
				filterString += "____";
			}

			if (dateTime.getMonth() != -1)
			{
				filterString += decimalFormat.format(dateTime.getMonth());
			}
			else
			{
				filterString += "__";
			}

			if (dateTime.getDay() != -1)
			{
				filterString += decimalFormat.format(dateTime.getDay());
			}
			else
			{
				filterString += "__";
			}

			if (dateTime.getHour() != -1)
			{
				filterString += decimalFormat.format(dateTime.getHour());
			}
			else
			{
				filterString += "__";
			}

			if (dateTime.getMinute() != -1)
			{
				filterString += decimalFormat.format(dateTime.getMinute());
			}
			else
			{
				filterString += "__";
			}

			if (dateTime.getSecond() != -1)
			{
				filterString += decimalFormat.format(dateTime.getSecond());
			}
			else
			{
				filterString += "__";
			}

			if (filterString.equals("______________"))
			{
				filterString = "";
			}
			else
			{
				filterString =
					" AND " + FIELD_DATE_TIME + " LIKE '" + filterString + "'";
			}
		}
		else if ((operator.equals(">=")) || (operator.equals("<")))
		{
			decimalFormat.setMinimumIntegerDigits(14);
			long filterValue = 0;

			if (dateTime.getYear() > 0)
			{
				filterValue += (long)(DateTime.YEAR_BASE + dateTime.getYear()) *
					10000000000l;
			}

			if (dateTime.getMonth() > 0)
			{
				filterValue += (long)dateTime.getMonth() * 100000000l;
			}

			if (dateTime.getDay() > 0)
			{
				filterValue += (long)dateTime.getDay() * 1000000l;
			}

			if (dateTime.getHour() > 0)
			{
				filterValue += (long)dateTime.getHour() * 10000l;
			}

			if (dateTime.getMinute() > 0)
			{
				filterValue += (long)dateTime.getMinute() * 100l;
			}

			if (dateTime.getSecond() > 0)
			{
				filterValue += (long)dateTime.getSecond();
			}

			if (filterValue > 0)
			{
				filterString = " AND " + FIELD_DATE_TIME + operator + "'" +
					decimalFormat.format(filterValue) + "'";
			}
		}

		return filterString;
	}


	private String buildFilterStatus(final Status status, final String operator)
	{
		String filterString = "";

		if (status.getByteValue1() != -1)
		{
			filterString += " AND " + FIELD_STATUS_BYTE0 + operator +
				status.getByteValue1();
		}

		if (status.getByteValue2() != -1)
		{
			filterString += " AND " + FIELD_STATUS_BYTE1 + operator +
				status.getByteValue2();
		}

		if (status.getShortValue1() != -1)
		{
			filterString += " AND " + FIELD_STATUS_SHORT0 + operator +
				status.getShortValue1();
		}

		if (status.getShortValue2() != -1)
		{
			filterString += " AND " + FIELD_STATUS_SHORT1 + operator +
				status.getShortValue2();
		}

		return filterString;
	}


	private String buildFilterEvent(final Event event, final String operator)
	{
		String filterString = "";

		if (event.getIndex() != -1)
		{
			if (event.getIndex() < Event.EVENT_INDEX_MIN)
			{
				mIsOneLimit = true;
			}
			else
			{
				filterString +=
					" AND " + FIELD_EVENT_INDEX + operator + event.getIndex();
			}
		}

		if (event.getPort() != -1)
		{
			filterString +=
				" AND " + FIELD_EVENT_PORT + operator + event.getPort();
		}

		if (event.getEvent() != -1)
		{
			filterString +=
				" AND " + FIELD_EVENT_TYPE + operator + event.getEvent();
		}

		if (event.getUrgency() != -1)
		{
			filterString +=
				" AND " + FIELD_EVENT_URGENCY + operator + event.getUrgency();
		}

		if (event.getValue() != -1)
		{
			filterString +=
				" AND " + FIELD_EVENT_VALUE + operator + event.getValue();
		}

		return filterString;
	}


	private History getHistory(Cursor cursor, History history)
	{
		if (history == null)
		{
			history = new History();
		}

		DateTime dateTime = new DateTime();
		Status status = new Status();
		Event event = new Event();


		dateTime.setBCD(cursor.getLong(cursor.getColumnIndex(FIELD_DATE_TIME)));
		status.setByteValue1(
			cursor.getInt(cursor.getColumnIndex(FIELD_STATUS_BYTE0)));
		status.setByteValue2(
			cursor.getInt(cursor.getColumnIndex(FIELD_STATUS_BYTE1)));
		status.setShortValue1(
			cursor.getInt(cursor.getColumnIndex(FIELD_STATUS_SHORT0)));
		status.setShortValue2(
			cursor.getInt(cursor.getColumnIndex(FIELD_STATUS_SHORT1)));
		event.setIndex(cursor.getInt(cursor.getColumnIndex(FIELD_EVENT_INDEX)));
		event.setPort(cursor.getInt(cursor.getColumnIndex(FIELD_EVENT_PORT)));
		event.setEvent(cursor.getInt(cursor.getColumnIndex(FIELD_EVENT_TYPE)));
		event.setUrgency(
			cursor.getInt(cursor.getColumnIndex(FIELD_EVENT_URGENCY)));
		event.setValue(cursor.getInt(cursor.getColumnIndex(FIELD_EVENT_VALUE)));
		history.setDateTime(dateTime);
		history.setStatus(status);
		history.setEvent(event);

		return history;
	}
}
