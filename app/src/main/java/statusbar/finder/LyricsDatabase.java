package statusbar.finder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaMetadata;
import android.util.Log;
import androidx.annotation.Nullable;
import statusbar.finder.provider.ILrcProvider;

public class LyricsDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Lyrics.db";
    private static final int DATABASE_VERSION = 1;



    public LyricsDatabase(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS Lyrics (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "song TEXT NOT NULL," +
                "artist TEXT," +
                "album TEXT," +
                "duration BIGINT," +
                "distance BIGINT," +
                "lyric TEXT," +
                "translated_lyric TEXT," +
                "lyric_source TEXT," +
                "added_date DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "_offset INTEGER)";
        db.execSQL(createTableSQL);
    }

    public boolean insertLyricIntoDatabase(ILrcProvider.LyricResult lyricResult, MediaMetadata mediaMetadata) {
        String song = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        String album = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
        long duration = mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION);

        if (song == null || artist == null) {
            return false;
        }

        String query = "INSERT INTO Lyrics (song, artist, album, duration, distance, lyric, translated_lyric, lyric_source, _offset) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL(query, new Object[]{song, artist, album, duration, lyricResult.mDistance, lyricResult.mLyric, lyricResult.mTranslatedLyric, lyricResult.mSource, lyricResult.mOffset});

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
        }
    }

    @SuppressLint("Range")
    public ILrcProvider.LyricResult searchLyricFromDatabase(MediaMetadata mediaMetadata) {
        String song = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        String album = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
        long duration = mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        @SuppressLint("Recycle") Cursor cursor;
        if (song == null || artist == null) {
            return null;
        }

        ILrcProvider.LyricResult result = new ILrcProvider.LyricResult();
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("searchLyricFromDatabase: ", String.format("SearchInfo : %s - %s - %s - %d", song, artist, album, duration));
        if (album != null) {
            String query = "SELECT lyric, translated_lyric, lyric_source, distance, _offset FROM Lyrics WHERE song = ? AND artist = ? AND album = ? AND duration = ?";
            cursor = db.rawQuery(query, new String[]{song, artist, album, String.valueOf(duration)});
        } else {
            String query = "SELECT lyric, translated_lyric, lyric_source, distance, _offset FROM Lyrics WHERE song = ? AND artist = ? AND duration = ?";
            cursor = db.rawQuery(query, new String[]{song, artist, String.valueOf(duration)});
        }


        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result.mLyric = cursor.getString(cursor.getColumnIndex("lyric"));
                result.mTranslatedLyric = cursor.getString(cursor.getColumnIndex("translated_lyric"));
                result.mSource = cursor.getString(cursor.getColumnIndex("lyric_source"));
                result.mDistance = cursor.getLong(cursor.getColumnIndex("distance"));
                result.mOffset = (int) cursor.getLong(cursor.getColumnIndex("_offset"));

                cursor.close();
                return result;
            }
            cursor.close();
        }
        return null;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 更新数据表结构后在此处添加内容
    }
}
