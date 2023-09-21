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

        public String getAllLyrics(boolean newLine) {
            StringBuilder lyricsBuilder = new StringBuilder();
            String[] lines = this.mLyric.split("\n");
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("[") && trimmedLine.contains("]")) {
                    int endIndex = trimmedLine.indexOf("]");
                    String lyricText = trimmedLine.substring(endIndex + 1).trim();
                    lyricsBuilder.append(lyricText).append("\n");
                }
            }
            if (newLine){
                return lyricsBuilder.toString();
            } else {
                return lyricsBuilder.toString().replace("\n", "");
            }
        }
    }
}
