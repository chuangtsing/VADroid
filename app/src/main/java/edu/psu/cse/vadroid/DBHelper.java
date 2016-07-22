package edu.psu.cse.vadroid;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    // Logcat tag
    // private static final String LOG = DBHelper.class.getName();

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "VideoAnalyticsDB";

    // Table Names
    private static final String TABLE_VIDEO = "video";
    private static final String TABLE_TAG = "tag";
    private static final String TABLE_VIDEO_TAG = "video_tag";

    // Common column names
    private static final String KEY_ID = "id";

    // NOTES Table - column names
    private static final String VIDEO_NAME = "name";
    private static final String VIDEO_PATH = "path";
    private static final String VIDEO_DATE = "date";
    private static final String VIDEO_SIZE = "size";
    private static final String VIDEO_DURATION = "duration";
    private static final String VIDEO_MIME = "mime";
    private static final String VIDEO_BITRATE = "bitrate";
    private static final String VIDEO_LAT = "loc_lat";
    private static final String VIDEO_LONG = "loc_long";
    private static final String VIDEO_HEIGHT = "height";
    private static final String VIDEO_WIDTH = "width";
    private static final String VIDEO_ROTATION = "rotation";
    private static final String VIDEO_TAGS = "tags";
    private static final String VIDEO_FPS = "frame_rate";
    private static final String VIDEO_FRAMES_PROCESSED = "frames_processed";
    private static final String VIDEO_TOTAL_FRAMES = "total_frames";


    /*// TAGS Table - column names
    private static final String TAG_NAME = "tag";

    // NOTE_TAGS Table - column names
    private static final String KEY_VIDEO_ID = "video_id";
    private static final String KEY_TAG_ID = "tag_id";*/

    // Table Create Statements
    // Video table create statement
    private static final String CREATE_TABLE_VIDEO = "CREATE TABLE "
            + TABLE_VIDEO + "(" + VIDEO_NAME + "  TEXT, " + VIDEO_PATH + " TEXT NOT NULL UNIQUE, "
            + VIDEO_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " + VIDEO_SIZE
            + " INTEGER, " + VIDEO_DURATION + " INTEGER, " + VIDEO_MIME
            + " TEXT, " + VIDEO_BITRATE + " INTEGER, " + VIDEO_LAT + " REAL, "
            + VIDEO_LONG + " REAL, " + VIDEO_HEIGHT + " INTEGER, "
            + VIDEO_WIDTH + " INTEGER, " + VIDEO_ROTATION + " INTEGER, " + VIDEO_TAGS + " TEXT, "
            + VIDEO_FPS + " REAL, " + VIDEO_FRAMES_PROCESSED + " INTEGER, " + VIDEO_TOTAL_FRAMES
            + " INTEGER)";

    /*private static final String CREATE_TABLE_TAG = "CREATE TABLE " + TABLE_TAG
            + "(" + KEY_ID + " INTEGER PRIMARY KEY, " + TAG_NAME + " TEXT)";

    private static final String CREATE_TABLE_VIDEO_TAG = "CREATE TABLE "
            + TABLE_VIDEO_TAG + "(" + KEY_VIDEO_ID + " INTEGER, " + KEY_TAG_ID
            + " TEXT, FOREIGN KEY(" + KEY_VIDEO_ID + ") REFERENCES "
            + TABLE_VIDEO + "(" + TABLE_TAG + "), FOREIGN KEY(" + KEY_TAG_ID
            + ") REFERENCES " + TABLE_VIDEO + "(" + TABLE_TAG + "))";*/

    public static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";

    private Context context;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_VIDEO);
        //db.execSQL(CREATE_TABLE_TAG);
        //db.execSQL(CREATE_TABLE_VIDEO_TAG);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub

    }

    public List<Video> getAllVideos() {
        List<Video> videos = new ArrayList<Video>();
        String query = "SELECT * FROM " + TABLE_VIDEO;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c;
        try {
            c = db.rawQuery(query, null);
        } catch (SQLException e) {
            String text = "Error adding tag: " + e.getMessage();
            Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
            toast.show();
            return null;
        }

        Video vid = null;

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                vid = new Video();
                vid.name = (c.getString(c.getColumnIndex(VIDEO_NAME)));
                vid.timestamp = Timestamp.valueOf(c.getString(c
                        .getColumnIndex(VIDEO_DATE)));
                vid.path = (c.getString(c.getColumnIndex(VIDEO_PATH)));
                vid.size = (c.getLong(c.getColumnIndex(VIDEO_SIZE)));
                vid.duration = (c.getInt(c.getColumnIndex(VIDEO_DURATION)));
                vid.bitrate = (c.getInt(c.getColumnIndex(VIDEO_BITRATE)));
                vid.mime = (c.getString(c.getColumnIndex(VIDEO_MIME)));
                vid.location = new Location("CaffeApp");
                vid.location.setLatitude(c.getInt(c
                        .getColumnIndex(VIDEO_LAT)));
                vid.location.setLatitude(c.getInt(c
                        .getColumnIndex(VIDEO_LONG)));
                vid.height = c.getInt(c.getColumnIndex(VIDEO_HEIGHT));
                vid.width = c.getInt(c.getColumnIndex(VIDEO_WIDTH));
                vid.rotation = c.getInt(c.getColumnIndex(VIDEO_ROTATION));
                vid.fps = c.getFloat(c.getColumnIndex(VIDEO_FPS));
                vid.framesProcessed = c.getInt(c.getColumnIndex(VIDEO_FRAMES_PROCESSED));
                vid.totalFrames = c.getInt(c.getColumnIndex(VIDEO_TOTAL_FRAMES));
                String[] pairs = c.getString(c.getColumnIndex(VIDEO_TAGS)).split(";");
                for (String pair : pairs) {
                    vid.tags.add(Integer.parseInt(pair.split(",")[0]));
                }
                if (vid != null) {
                    videos.add(vid);
                }
            } while (c.moveToNext());
        }

        return videos;
    }

    public List<Video> getVideosByTags(List<String> tags) {
        List<Video> videos = new ArrayList<Video>();
        SQLiteDatabase db = this.getReadableDatabase();

		/*
         * String queryBase = "SELECT * FROM " + TABLE_VIDEO_TAG +
		 * " INNER JOIN " + TABLE_VIDEO + " ON " + TABLE_VIDEO_TAG + "." +
		 * KEY_VIDEO_ID + "=" + TABLE_VIDEO + "." + KEY_ID + " INNER JOIN " +
		 * TABLE_TAG + " ON " + TABLE_TAG + "." + KEY_TAG_ID + "=" + TABLE_TAG +
		 * "." + KEY_ID + "WHERE " + TABLE_TAG + "." + TAG_NAME + " IN (";
		 */

        String queryBase = "SELECT * FROM " + TABLE_VIDEO + " WHERE ";
        StringBuilder sb = new StringBuilder(queryBase);
        int size = tags.size();
        for (int i = 0; i < size; i++) {
            sb.append(VIDEO_TAGS + " REGEX '[^0-9]*" + tags.get(i) + "[^0-9]*' ");
            if (i < size - 1) {
                sb.append(", ");
            }
        }
        sb.append("ORDER BY " + VIDEO_DATE);

        // Initial query to obtain video ID's
        Cursor c = db.rawQuery(sb.toString(), null);
        Video vid = null;

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                vid = new Video();
                vid.name = (c.getString(c.getColumnIndex(VIDEO_NAME)));
                vid.timestamp = Timestamp.valueOf(c.getString(c
                        .getColumnIndex(VIDEO_DATE)));
                vid.path = (c.getString(c.getColumnIndex(VIDEO_PATH)));
                vid.size = (c.getLong(c.getColumnIndex(VIDEO_SIZE)));
                vid.duration = (c.getInt(c.getColumnIndex(VIDEO_DURATION)));
                vid.bitrate = (c.getInt(c.getColumnIndex(VIDEO_BITRATE)));
                vid.mime = (c.getString(c.getColumnIndex(VIDEO_MIME)));
                vid.location = new Location("CaffeApp");
                vid.location.setLatitude(c.getInt(c
                        .getColumnIndex(VIDEO_LAT)));
                vid.location.setLatitude(c.getInt(c
                        .getColumnIndex(VIDEO_LONG)));
                vid.height = c.getInt(c.getColumnIndex(VIDEO_HEIGHT));
                vid.width = c.getInt(c.getColumnIndex(VIDEO_WIDTH));
                vid.rotation = c.getInt(c.getColumnIndex(VIDEO_ROTATION));
                vid.fps = c.getFloat(c.getColumnIndex(VIDEO_FPS));
                vid.framesProcessed = c.getInt(c.getColumnIndex(VIDEO_FRAMES_PROCESSED));
                vid.totalFrames = c.getInt(c.getColumnIndex(VIDEO_TOTAL_FRAMES));
                String[] pairs = c.getString(c.getColumnIndex(VIDEO_TAGS)).split(";");
                for (String pair : pairs) {
                    vid.tags.add(Integer.parseInt(pair.split(",")[0]));
                }
                if (vid != null) {
                    videos.add(vid);
                }
            } while (c.moveToNext());
        }

        return videos;
    }

    /*public long addTag(String tag) {
        long id;
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(TAG_NAME, tag);
        try {
            id = db.insertOrThrow(TABLE_TAG, null, values);
        } catch (SQLException e) {
            String text = "Error adding tag: " + e.getMessage();
            Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
            toast.show();
            return -1;
        }
        String text = "Tag added";
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        toast.show();
        return id;
    }*/

    public void addVideo(Video vid) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        long vidId, tagId;
        Cursor c;
        values.put(VIDEO_NAME, vid.name);
        values.put(VIDEO_PATH, vid.path);
        values.put(VIDEO_DATE, vid.timestamp.toString());
        values.put(VIDEO_SIZE, vid.size);
        values.put(VIDEO_DURATION, vid.duration);
        values.put(VIDEO_BITRATE, vid.bitrate);
        values.put(VIDEO_MIME, vid.mime);
        values.put(VIDEO_LAT, vid.location.getLatitude());
        values.put(VIDEO_LONG, vid.location.getLongitude());
        values.put(VIDEO_HEIGHT, vid.height);
        values.put(VIDEO_WIDTH, vid.width);
        values.put(VIDEO_ROTATION, vid.rotation);
        StringBuilder sb = new StringBuilder();
        for (int tag : vid.tags)
            sb.append(tag).append(',');
        values.put(VIDEO_TAGS, sb.toString());
        values.put(VIDEO_FPS, vid.fps);
        values.put(VIDEO_FRAMES_PROCESSED, vid.framesProcessed);
        values.put(VIDEO_TOTAL_FRAMES, vid.totalFrames);

        try {
            vidId = db.insertOrThrow(TABLE_VIDEO, null, values);
        } catch (SQLException e) {
            String text = "Error adding video: " + e.getMessage();
            Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        /*for (String tag : vid.tags) {

            String query = "SELECT " + KEY_ID + " FROM " + TABLE_TAG
                    + " WHERE " + TAG_NAME + "='" + tag + "'";
            try {
                c = db.rawQuery(query, null);
            } catch (SQLException e) {
                String text = "Error adding video tag: " + e.getMessage();
                Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                toast.show();
                return;
            }

            if (c.moveToFirst()) {
                tagId = c.getInt(c.getColumnIndex(KEY_ID));
            } else {
                tagId = addTag(tag);
            }

            values.clear();
            values.put(KEY_VIDEO_ID, vidId);
            values.put(KEY_TAG_ID, tagId);
            try {
                db.insertOrThrow(TABLE_VIDEO_TAG, null, values);
            } catch (SQLException e) {
                String text = "Error adding video tag: " + e.getMessage();
                Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                toast.show();
                return;
            }
        }*/
        String text = "Video added";
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        toast.show();
    }

    public void sync() {
        List<Video> vids = this.getAllVideos();
        List<Video> newVids = new ArrayList<Video>();
        File dir = new File(CaffeMobile.VIDEOS_DIR);
        String[] projection = { MediaStore.Video.Media._ID};
        Cursor cursor = new CursorLoader(VADroid.getContext(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
                null, // Return all rows
                null, null).loadInBackground();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id);
            newVids.add(new Video(uri.getPath()));
        }
        for(Video vid : vids) {
            if (Video.hasVideo(newVids, vid)) {}
        }
    }

}