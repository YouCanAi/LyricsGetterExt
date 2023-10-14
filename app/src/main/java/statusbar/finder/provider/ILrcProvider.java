package statusbar.finder.provider;

import android.media.MediaMetadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public interface ILrcProvider {
    LyricResult getLyric(MediaMetadata data) throws IOException;

    class LyricResult {
        public String mLyric;
        public String mTranslatedLyric;
        public long mDistance;
        public String mSource = "Local";
        public int mOffset = 0;

        public String toSting() {
            return "Distance: " + mDistance + "\n" +
                    "Source: " + mSource + "\n" +
                    "Offset: " + mOffset + "\n" +
                    "Lyric: " + mLyric + "\n" +
                    "TranslatedLyric: " + mTranslatedLyric + "\n";
        }
    }
}
