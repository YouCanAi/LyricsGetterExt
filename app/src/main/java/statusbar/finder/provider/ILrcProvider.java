package statusbar.finder.provider;

import android.media.MediaMetadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public interface ILrcProvider {
    LyricResult getLyric(MediaMetadata data) throws IOException;

    class LyricResult {
        public String mLyric;
        public String mTransLyric;
        public long mDistance;

        public String source = "Local";
    }
}
