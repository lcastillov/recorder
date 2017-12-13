package com.upkoder.recorder.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.upkoder.recorder.RecordsSQLiteAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by leandro on 3/18/2016.
 */
public class Record extends Entity {
    public interface Callback {
        void finished(Record record);
    }

    //----------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------
    public final static String TABLE_NAME = "record";

    public final static String ID = "_id";
    public final static String DATE = "_date";
    public final static String NAME = "_name";
    public final static String DURATION = "_duration";
    public final static String SIZE = "_size";

    public final static String[] _SELECTION = new String[] {
            Record.ID,
            Record.DATE,
            Record.NAME,
            Record.DURATION,
            Record.SIZE
    };

    //----------------------------------------------------------------------
    // Constructor(s)
    //----------------------------------------------------------------------
    private Record(Context context) {
        this.context = context;
    }

    //----------------------------------------------------------------------
    // Context field
    //----------------------------------------------------------------------
    private Context context;

    //----------------------------------------------------------------------
    // Fields
    //----------------------------------------------------------------------
    private long id;
    private String date;
    private String name;
    private long duration;
    private long size;

    //----------------------------------------------------------------------
    // Getters
    //----------------------------------------------------------------------
    public long getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public String getVerboseDuration() {
        if (duration < 1000)
            return String.format("%d ms", duration);
        if (duration < 60 * 1000)
            return (duration / 1000) + "s";
        long M = (duration / (60 * 1000));
        long S = (duration % (60 * 1000)) / 1000;
        if (M < 60)
            return String.format("%d m %d s", M, S);
        long H = M / 60;
        M = H % 60;
        if (H < 24)
            return String.format("%d h %d m %d s", H, M, S);
        long D = H / 24;
        H = H % 24;
        return String.format("%d d %d h %d m", D, H, M);
    }

    public String getVerboseSize() {
        if (size < 1024)
            return String.format("%d b", size);
        if (size < 1024 * 1024)
            return String.format("%d KB", size / 1024);
        if (size < 1024 * 1024 * 1024)
            return String.format("%d MB", size / (1024 * 1024));
        return String.format("%d GB", size / (1024 * 1024 * 1024));
    }

    //----------------------------------------------------------------------
    // Entity methods
    //----------------------------------------------------------------------
    @Override
    public void persist() {
        ContentValues values = new ContentValues();
        values.put(Record.ID, id);
        values.put(Record.DATE, date);
        values.put(Record.NAME, name);
        values.put(Record.DURATION, duration);
        values.put(Record.SIZE, size);
        RecordsSQLiteAdapter DB = new RecordsSQLiteAdapter(context);
        DB.open();
        DB.update(Record.TABLE_NAME, id, values);
        DB.close();
    }

    @Override
    public void delete() {
        RecordsSQLiteAdapter DB = new RecordsSQLiteAdapter(context);
        DB.open();

        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), getName());
        if (file.exists()) {
            boolean result = file.delete();
            if (result)
                Log.d("ZZZ", "Record::delete => audio removed");
        }

        DB.delete(Record.TABLE_NAME, id);
        DB.close();
    }

    public void send() {
        send(null);
    }

    public void send(final Record.Callback callback) {
        Log.d("[upkoder-debug]", "sended");
    }

    //----------------------------------------------------------------------
    // Static methods
    //----------------------------------------------------------------------
    private static List<Record> recordsFromCursor(Cursor cursor, Context context) {
        ArrayList<Record> records = new ArrayList<>();
        if (cursor != null && !cursor.isClosed() && cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                Record record = new Record(context);
                record.id = cursor.getLong(cursor.getColumnIndex(Record.ID));
                record.date = cursor.getString(cursor.getColumnIndex(Record.DATE));
                record.name = cursor.getString(cursor.getColumnIndex(Record.NAME));
                record.duration = cursor.getLong(cursor.getColumnIndex(Record.DURATION));
                record.size = cursor.getLong(cursor.getColumnIndex(Record.SIZE));
                records.add(record);
                cursor.moveToNext();
            }
            cursor.close();
        }
        return records;
    }

    public static List<Record> all(Context context) {
        RecordsSQLiteAdapter DB = new RecordsSQLiteAdapter(context);
        DB.open();
        List<Record> records =
                recordsFromCursor(DB.fetchAll(Record.TABLE_NAME, Record._SELECTION), context);
        DB.close();
        return records;
    }

    public static Record get(Context context, long ID) {
        RecordsSQLiteAdapter DB = new RecordsSQLiteAdapter(context);
        DB.open();
        List<Record> records =
                recordsFromCursor(DB.fetchEntity(Record.TABLE_NAME, Record._SELECTION, ID), context);
        DB.close();
        return records.size() != 1 ? null : records.get(0);
    }

    public static Record create(Context context, String name, String date) {
        ContentValues values = new ContentValues();
        values.put(Record.NAME, name);
        values.put(Record.DATE, date);
        final File audio = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), name);
        if (!audio.exists()) {
            return null;
        }
        long size = audio.length();
        long duration = -1;
        MediaPlayer MP = new MediaPlayer();
        try {
            MP.setDataSource(audio.getPath());
            MP.prepare();
            duration = MP.getDuration();
        } catch(Exception e) {
            values = null;
        } finally {
            MP.release();
        }
        if (values == null)
            return null;
        values.put(Record.SIZE, size);
        values.put(Record.DURATION, duration);
        RecordsSQLiteAdapter DB = new RecordsSQLiteAdapter(context);
        DB.open();
        long ID = DB.insertEntity(Record.TABLE_NAME, values);
        DB.close();
        return ID > 0 ? Record.get(context, ID) : null;
    }
}
