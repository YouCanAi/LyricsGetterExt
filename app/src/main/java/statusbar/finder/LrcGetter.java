package statusbar.finder;

import android.content.Context;
import android.media.MediaMetadata;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import cn.zhaiyifan.lyric.LyricUtils;
import cn.zhaiyifan.lyric.model.Lyric;
import statusbar.finder.provider.*;
import statusbar.finder.provider.utils.LyricSearchUtil;
import statusbar.finder.misc.checkStringLang;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

public class LrcGetter {

    private static final ILrcProvider[] providers = {
            new MusixMatchProvider(),
            new KugouProvider(),
            new QQMusicProvider(),
            new NeteaseProvider(),
    };

    private static MessageDigest messageDigest;
    private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

    public static Lyric getLyric(Context context, MediaMetadata mediaMetadata, String sysLang) {
        if (messageDigest == null) {
            try {
                messageDigest =  MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
        }
        File cachePath = context.getCacheDir();
        String meta = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE) + "," + mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST) + "," +
                mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM) + ", " + mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        File requireLrcPath = new File(cachePath, printHexBinary(messageDigest.digest(meta.getBytes())) + ".lrc");
        if (requireLrcPath.exists()) {
            return LyricUtils.parseLyric(requireLrcPath, "UTF-8");
        }
        ILrcProvider.LyricResult currentResult = null;
        for (ILrcProvider provider : providers) {
            try {
                ILrcProvider.LyricResult lyricResult = provider.getLyric(mediaMetadata);
                if (lyricResult != null && LyricSearchUtil.isLyricContent(lyricResult.mLyric) && (currentResult == null || currentResult.mDistance > lyricResult.mDistance)) {
                    currentResult = lyricResult;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (currentResult != null && LyricSearchUtil.isLyricContent(currentResult.mLyric)) {
            String allLyrics = currentResult.getAllLyrics(false);
            if (Objects.equals(sysLang, "zh-CN") && !checkStringLang.isJapanese(allLyrics)) {
                currentResult.mLyric = ZhConverterUtil.toSimple(currentResult.mLyric);
            } else if (Objects.equals(sysLang, "zh-TW") && !checkStringLang.isJapanese(allLyrics)) {
                currentResult.mLyric = ZhConverterUtil.toTraditional(currentResult.mLyric);
            }
            try {
                FileOutputStream lrcOut = new FileOutputStream(requireLrcPath);
                lrcOut.write(currentResult.mLyric.getBytes());
                lrcOut.close();
                return LyricUtils.parseLyric(requireLrcPath, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String printHexBinary(byte[] data) {
        StringBuilder r = new StringBuilder(data.length * 2);
        for (byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[b & 0xF]);
        }
        return r.toString();
    }
}
