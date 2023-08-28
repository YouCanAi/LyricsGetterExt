package statusbar.finder.provider;

import android.media.MediaMetadata;

import org.json.JSONException;

import java.io.IOException;
import java.net.URISyntaxException;

public interface ILrcProvider {
    LyricResult getLyric(MediaMetadata data) throws IOException, JSONException, URISyntaxException;

    class LyricResult {
        public String mLyric;
        public long mDistance;
    }
}
