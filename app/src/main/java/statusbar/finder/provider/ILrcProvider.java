package statusbar.finder.provider;

import android.media.MediaMetadata;

import java.io.IOException;

public interface ILrcProvider {
    LyricResult getLyric(MediaMetadata data) throws IOException;
    LyricResult getLyric(SimpleSongInfo simpleSongInfo) throws IOException;

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
