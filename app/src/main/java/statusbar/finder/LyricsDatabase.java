package statusbar.finder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

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


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 更新数据表结构后在此处添加内容
    }
}
