package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * <p/>
 * Please read:
 * <p/>
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * <p/>
 * before you start to get yourself familiarized with ContentProvider.
 * <p/>
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 */
public class GroupMessengerProvider extends ContentProvider {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final String dbName = "keyValue.db";
    private MainDatabaseHelper mOpenHelper;
    private SQLiteDatabase db;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        long resultOfQuery = 0;
        db = mOpenHelper.getWritableDatabase();


        resultOfQuery = db.insertWithOnConflict("chats", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (resultOfQuery == -1) {
            Log.v(TAG, "Insert Failed");
        } else {
            Log.v(TAG, "Insert Success");
        }

        String insertedKey = values.getAsString("key");
        String insertedValue = values.getAsString("value");
        Log.v(TAG,"insertKey="+insertedKey+"||insertValue"+insertedValue);
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        mOpenHelper = new MainDatabaseHelper(getContext());
        return true;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        String columns[] = {"key", "value"};
        Cursor cursorToReturn = null;
        String sqlLiteSelection = " key = '" + selection + "'";
        Log.v(TAG,"query="+ sqlLiteSelection);
        try {
            cursorToReturn = db.query("chats", columns, sqlLiteSelection, null, null, null, null);
        } catch (Exception e) {
            Log.e(TAG,"query Error");
        }
        return cursorToReturn;
    }

    protected static final class MainDatabaseHelper extends SQLiteOpenHelper {

        static final String TAG = MainDatabaseHelper.class.getSimpleName();

        // A string that defines the SQL statement for creating a table
        private static final String SQL_CREATE_MAIN = "CREATE TABLE chats (_ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, key TEXT UNIQUE, value TEXT)";

        /**
         * Helper class that actually creates and manages the provider's underlying data repository.
         */


        /*
         * Instantiates an open helper for the provider's SQLite data repository
         * Do not do database creation and upgrade here.
         */
        MainDatabaseHelper(Context context) {
            super(context, dbName, null, 1);
        }

        /*
         * Creates the data repository. This is called when the provider attempts to open the
         * repository and SQLite reports that it doesn't exist.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {

            // Creates the main table
            db.execSQL(SQL_CREATE_MAIN);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.e(TAG, "XXX");
        }
    }
}
