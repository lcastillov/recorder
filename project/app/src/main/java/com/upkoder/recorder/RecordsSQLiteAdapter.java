package com.upkoder.recorder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.upkoder.recorder.models.Record;

public class RecordsSQLiteAdapter {
    private static final String DATABASE_NAME = "recorder-db";
    private static final int DATABASE_VERSION = 1;

    private RecordsSQLiteHelper mDbHelper;
    private SQLiteDatabase mDb;
    private final Context mCtx;

    public RecordsSQLiteAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    private static final String DATABASE_CREATE_RECORD_TABLE =
            "create table " + Record.TABLE_NAME + " ("
                    + "_id integer primary key autoincrement, "
                    + Record.NAME + " text not null, "
                    + Record.SIZE + " integer not null, "
                    + Record.DURATION + " integer not null, "
                    + Record.DATE + " text not null);";

    private class RecordsSQLiteHelper extends SQLiteOpenHelper {

        public RecordsSQLiteHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE_RECORD_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO: Handle this in a better way !!!
        }
    }

    public RecordsSQLiteAdapter open() throws SQLException {
        mDbHelper = new RecordsSQLiteHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    public void update(String TABLE, long ID, ContentValues values) {
        mDb.update(TABLE, values, "_id=?", new String[]{ String.valueOf(ID) });
    }

    public void delete(String TABLE, long ID) {
        mDb.delete(TABLE, "_id=?", new String[]{String.valueOf(ID)});
    }

    public Cursor fetchAll(String TABLE, String[] selection) {
        return mDb.query(TABLE, selection, null, null, null, null, "_id desc");
    }

    public Cursor fetchEntity(String TABLE, String[] selection, long ID) throws SQLException {
        Cursor mCursor = mDb.query(true, TABLE, selection, "_id=" + ID, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public long insertEntity(String TABLE, ContentValues values) {
        return mDb.insert(TABLE, null, values);
    }
}
