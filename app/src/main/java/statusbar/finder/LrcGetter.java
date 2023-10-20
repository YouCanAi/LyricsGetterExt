package statusbar.finder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadata;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import android.util.Log;
import cn.zhaiyifan.lyric.LyricUtils;
import cn.zhaiyifan.lyric.model.Lyric;
import statusbar.finder.LyricsDatabase;
import statusbar.finder.misc.Constants;
import statusbar.finder.provider.*;
import statusbar.finder.provider.utils.LyricSearchUtil;
import statusbar.finder.misc.checkStringLang;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

public class LrcGetter {

    private static final ILrcProvider[] providers = {
            new MusixMatchProvider(),
            new NeteaseProvider(),
            new KugouProvider(),
            // new QQMusicProvider(), Can't working.
    };
    private static MessageDigest messageDigest;

    public static Lyric getLyric(Context context, MediaMetadata mediaMetadata, String sysLang) {
        LyricsDatabase lyricsDatabase = new LyricsDatabase(context);

        if (messageDigest == null) {
            try {
                messageDigest = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
        }

        ILrcProvider.LyricResult currentResult = searchLyricFromDatabase(lyricsDatabase, mediaMetadata);
        if (currentResult != null) {
            return LyricUtils.parseLyric(currentResult, mediaMetadata);
        }
        for (ILrcProvider provider : providers) {
            try {
                ILrcProvider.LyricResult lyricResult = provider.getLyric(mediaMetadata);
                if (lyricResult != null) {
                    if (LyricSearchUtil.isLyricContent(lyricResult.mLyric) && (currentResult == null || currentResult.mDistance > lyricResult.mDistance)) {
                        currentResult = lyricResult;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if  (currentResult == null) {
            return null;
        }
        String allLyrics;
        if (Constants.isTranslateCheck) {
            if (currentResult.mTranslatedLyric != null) {
                allLyrics = LyricUtils.getAllLyrics(false, currentResult.mTranslatedLyric);
            } else {
                allLyrics = LyricUtils.getAllLyrics(false, currentResult.mLyric);
            }
        } else {
            allLyrics = LyricUtils.getAllLyrics(false, currentResult.mLyric);
        }

        if (Objects.equals(sysLang, "zh-CN") && !checkStringLang.isJapanese(allLyrics)) {
            if (currentResult.mTranslatedLyric != null) {
                currentResult.mTranslatedLyric = ZhConverterUtil.toSimple(currentResult.mTranslatedLyric);
            } else {
                currentResult.mLyric = ZhConverterUtil.toSimple(currentResult.mLyric);
            }
        } else if (Objects.equals(sysLang, "zh-TW") && !checkStringLang.isJapanese(allLyrics)) {
            if (currentResult.mTranslatedLyric != null) {
                currentResult.mTranslatedLyric = ZhConverterUtil.toTraditional(currentResult.mTranslatedLyric);
            } else {
                currentResult.mLyric = ZhConverterUtil.toTraditional(currentResult.mLyric);
            }
        }

        if (insertLyricIntoDatabase(lyricsDatabase, currentResult, mediaMetadata)) {
            return LyricUtils.parseLyric(currentResult, mediaMetadata);
        }
        return null;
    }

    @SuppressLint("Range")
    private static ILrcProvider.LyricResult searchLyricFromDatabase(LyricsDatabase lyricsDatabase, MediaMetadata mediaMetadata) {
        String song = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        String album = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
        long duration = mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        @SuppressLint("Recycle") Cursor cursor;
        if (song == null || artist == null) {
            return null;
        }

        ILrcProvider.LyricResult result = new ILrcProvider.LyricResult();
        SQLiteDatabase db = lyricsDatabase.getReadableDatabase();
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

    private static boolean insertLyricIntoDatabase(LyricsDatabase lyricsDatabase, ILrcProvider.LyricResult lyricResult, MediaMetadata mediaMetadata) {
        String song = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        String album = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
        long duration = mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION);

        if (song == null || artist == null) {
            return false;
        }

        String query = "INSERT INTO Lyrics (song, artist, album, duration, distance, lyric, translated_lyric, lyric_source, _offset) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        SQLiteDatabase db = lyricsDatabase.getWritableDatabase();
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
}
