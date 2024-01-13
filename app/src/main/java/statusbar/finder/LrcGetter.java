package statusbar.finder;

import android.content.Context;
import android.media.MediaMetadata;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import cn.zhaiyifan.lyric.LyricUtils;
import cn.zhaiyifan.lyric.model.Lyric;
import com.moji4j.MojiConverter;
import com.moji4j.MojiDetector;
import statusbar.finder.misc.Constants;
import statusbar.finder.provider.*;
import statusbar.finder.provider.utils.LyricSearchUtil;
import statusbar.finder.misc.checkStringLang;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

public class LrcGetter {
    static final String TAG = "LrcGetter";
    private static final ILrcProvider[] providers = {
            new MusixMatchProvider(),
            new NeteaseProvider(),
            new KugouProvider(),
            // new QQMusicProvider(), Can't working.
    };
    private static MessageDigest messageDigest;

    public static Lyric getLyric(Context context, MediaMetadata mediaMetadata, String sysLang) {
        LyricsDatabase lyricsDatabase = new LyricsDatabase(context);
        // Log.d(TAG, "curMediaData" + new SimpleSongInfo(mediaMetadata));
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
        currentResult = searchLyricsResultByInfo(mediaMetadata);
        if  (currentResult == null) {
            MojiDetector detector = new MojiDetector();
            MojiConverter converter = new MojiConverter();
            if (!detector.hasKana(mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE)) && detector.hasLatin(mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE))) {
                ILrcProvider.MediaInfo mediaInfo = new ILrcProvider.MediaInfo(mediaMetadata);
                mediaInfo.title = converter.convertRomajiToHiragana(mediaInfo.title);
                if (detector.hasLatin(mediaInfo.title)) {
                    return null;
                }
                // Log.d(TAG, "newSearchInfo:" + new SimpleSongInfo(mediaMetadata));
                currentResult = searchLyricsResultByInfo(mediaInfo);

                if (currentResult == null) {
                    mediaInfo.title = converter.convertRomajiToKatakana(mediaInfo.title);
                    // Log.d(TAG, "newSearchInfo:" + new SimpleSongInfo(mediaMetadata));
                    currentResult = searchLyricsResultByInfo(mediaInfo);
                }

            }
            if (currentResult == null) {
                lyricsDatabase.insertLyricIntoDatabase(null, mediaMetadata);
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

    private static ILrcProvider.LyricResult searchLyricsResultByInfo(MediaMetadata mediaMetadata) {
        return searchLyricsResultByInfo(new ILrcProvider.MediaInfo(mediaMetadata));
    }

    private static ILrcProvider.LyricResult searchLyricsResultByInfo(ILrcProvider.MediaInfo mediaInfo) {
        ILrcProvider.LyricResult currentResult = null;
        for (ILrcProvider provider : providers) {
            try {
                ILrcProvider.LyricResult lyricResult = provider.getLyric(mediaInfo);
                if (lyricResult != null) {
                    if (LyricSearchUtil.isLyricContent(lyricResult.mLyric) && (currentResult == null || currentResult.mDistance > lyricResult.mDistance)) {
                        currentResult = lyricResult;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return currentResult;
    }
}
