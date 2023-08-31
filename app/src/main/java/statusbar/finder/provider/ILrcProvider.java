package statusbar.finder.provider;

import android.media.MediaMetadata;

import org.json.JSONException;

import java.io.IOException;
import java.net.URISyntaxException;

public interface ILrcProvider {
    LyricResult getLyric(MediaMetadata data) throws IOException;

    class LyricResult {
        public String mLyric;
        public long mDistance;

        public String source = "Local";
    }
}
