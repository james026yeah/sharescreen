package archermind.dlna.mobile.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class TvDevicesProvider extends ContentProvider {
	private static final String TAG = "TvDevicesProvider";
	//authority
	private static final String AUTHORITY = "org.tvdevice.TvProvider";
	//database name
	public static final String DATABASE_NAME = "tv.db";
	//database version
	private static final int DATABASE_VERSION = 1;
	//tables name
	private static final String TV_TABLE = "tvdata";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TV_TABLE);

	//match URI
	private static final int TV = 1;
	private static UriMatcher mUriMatcher;
	private DatabaseHelper mDatabaseHelper;

    //tvdata table columns name
	public static final String _ID = "_id";
	public static final String SS_ID = "code_id";
	public static final String FRIENDY_NAME = "friendy_name";
	public static final String PASS_WORD = "password";
	public static final String LINK_STATE = "link_state";
	public static final String SCAN_DATE = "scan_date";

	static {
		    mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		    mUriMatcher.addURI(AUTHORITY, TV_TABLE, TV);
	}

	private static final String CREATE_DATABASE_TABLE = "CREATE TABLE "
	                                        + TV_TABLE + " ("
	                                        + _ID + " INTEGER PRIMARY KEY,"
	                                        + SS_ID + " TEXT,"
	                                        + FRIENDY_NAME + " TEXT,"
	                                        + PASS_WORD + " TEXT,"
	                                        + LINK_STATE + " INTEGER,"
	                                        + SCAN_DATE + " TEXT" + ");";
	/**
	 * function      : delete data from database
	 * @uri          : table uri
	 * @selection    : delete condition
	 * @selectionArgs: delete columns
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		int index = 0;

		switch(mUriMatcher.match(uri)) {
		    case TV: {
               index = db.delete(TV_TABLE, selection, selectionArgs);
               break;
		    }
		    default: {
		        throw new IllegalArgumentException("Unknown uri:" + uri);
		    }
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return index;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	/**
	 * function: insert data into database
	 * @uri    : table uri
	 * @values : insert data
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		long id;
		Uri newUri = null;

       switch(mUriMatcher.match(uri)) {
           case TV: {
               id = db.insert(TV_TABLE, _ID, values);
               if (id > 0) {
                   newUri = ContentUris.withAppendedId(CONTENT_URI, id);
                 }
               break;
            }
           default: {
               throw new IllegalArgumentException("Unknown uri:" + uri);
            }
	    }

		return newUri;
	}

	@Override
	public boolean onCreate() {
		mDatabaseHelper = new DatabaseHelper(getContext());
		return true;
	}

	/**
	 * function      : query data from database
	 * @uri          : table uri
	 * @projection   : query column
	 * @selection    : query condition
	 * @selectionArgs: query condition values
	 * @sortOrder    : query result sortresult
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

		switch(mUriMatcher.match(uri)) {
		    case TV: {
			    return db.query(TV_TABLE, projection, selection, selectionArgs, null, null, sortOrder);
		    }
		    default: {
	           throw new IllegalArgumentException("Unknown uri:" + uri);
	        }
		}
	}

	/**
	 * function      :update database data
	 * @uri          : table uri
	 * @selection    : update condition
	 * @selectionArgs: update condition values
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		int index = 0;

		switch(mUriMatcher.match(uri)) {
		    case TV: {
		    	index = db.update(TV_TABLE, values, selection, selectionArgs);
               break;
		    }
		    default: {
		    	 throw new IllegalArgumentException("Unknown uri:" + uri);
		    }
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return index;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		public DatabaseHelper(Context context, int version) {
			super(context, DATABASE_NAME, null, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_DATABASE_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TV_TABLE);
			onCreate(db);
		}
	}

	private void print(String msg) {
		Log.d(TAG, msg);
	}
}
