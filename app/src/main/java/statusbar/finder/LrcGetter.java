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
import com.github.houbb.opencc4j.core.ZhConvert;
import com.moji4j.MojiConverter;
import com.moji4j.MojiDetector;
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

        ILrcProvider.LyricResult currentResult = lyricsDatabase.searchLyricFromDatabase(mediaMetadata);
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
            MojiDetector detector = new MojiDetector();
            if (!detector.hasKana(mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE)) && detector.hasLatin(mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE))) {
                SimpleSongInfo simpleSongInfo = new SimpleSongInfo(mediaMetadata);
                simpleSongInfo.title = new MojiConverter().convertRomajiToHiragana(simpleSongInfo.title);
                for (ILrcProvider provider : providers) {
                    try {
                        ILrcProvider.LyricResult lyricResult = provider.getLyric(simpleSongInfo);
                        if (lyricResult != null) {
                            if (LyricSearchUtil.isLyricContent(lyricResult.mLyric) && (currentResult == null || currentResult.mDistance > lyricResult.mDistance)) {
                                currentResult = lyricResult;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (currentResult == null) {
                return null;
            }
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

        if (lyricsDatabase.insertLyricIntoDatabase(currentResult, mediaMetadata)) {
            return LyricUtils.parseLyric(currentResult, mediaMetadata);
        }
        return null;
    }
}
