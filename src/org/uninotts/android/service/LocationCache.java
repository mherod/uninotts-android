package org.uninotts.android.service;

import org.studentnow.Location;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocationCache extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "location_cache";

	private static final String TABLE_FIXES = "fixes";

	private static final String FIELD_FIX_TIME = "time";
	private static final String FIELD_FIX_PRVID = "provider";
	private static final String FIELD_FIX_LAT = "lat";
	private static final String FIELD_FIX_LNG = "lng";

	public LocationCache(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		db.execSQL("CREATE TABLE " + TABLE_FIXES

		+ "("

		+ FIELD_FIX_TIME + " LONG PRIMARY KEY,"

		+ FIELD_FIX_PRVID + " TEXT,"

		+ FIELD_FIX_LAT + " DOUBLE,"

		+ FIELD_FIX_LNG + " DOUBLE" +

		")");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_FIXES);
		onCreate(db);
	}

	public int getLocationFixCount() {

		String countQuery = "SELECT  * FROM " + TABLE_FIXES;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		cursor.close();

		return cursor.getCount();

	}

	public void empty() {
		onUpgrade(this.getWritableDatabase(), DATABASE_VERSION,
				DATABASE_VERSION);

	}

	public CachedLocation getLastLocation() {

		String selectQuery = "SELECT  * "

		+ "FROM " + TABLE_FIXES + " "

		+ "ORDER BY " + FIELD_FIX_TIME + " "

		+ "DESC LIMIT 1";

		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		if (cursor.moveToFirst()) {
			double lat = cursor.getDouble(2);
			double lng = cursor.getDouble(3);
			CachedLocation loc = new CachedLocation(lat, lng);
			loc.setTime(cursor.getLong(0));
			loc.setProvider(cursor.getString(1));
			return loc;
		}
		return null;
	}

	public void storeLocation(android.location.Location loc) {

		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(FIELD_FIX_TIME, loc.getTime());
		values.put(FIELD_FIX_PRVID, loc.getProvider());
		values.put(FIELD_FIX_LAT, loc.getLatitude());
		values.put(FIELD_FIX_LNG, loc.getLongitude());

		db.insert(TABLE_FIXES, null, values);
		db.close();

	}

	public class CachedLocation implements Location {

		private String provider = "";
		private long time = 0;

		double lat, lng;

		public CachedLocation(double lat, double lng) {
			this.lat = lat;
			this.lng = lng;
		}

		public long getTime() {
			return time;
		}

		public void setTime(long time) {
			this.time = time;
		}

		public String getProvider() {
			return provider;
		}

		public void setProvider(String provider) {
			this.provider = provider;
		}

		@Override
		public double getLat() {
			return lat;
		}

		@Override
		public double getLng() {
			return lng;
		}

		public String getString() {
			return lat + "," + lng;
		}

	}

}
