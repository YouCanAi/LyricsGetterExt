package statusbar.finder.provider;

import android.media.MediaMetadata;

import java.io.IOException;
import java.util.Locale;

public interface ILrcProvider {
    LyricResult getLyric(MediaMetadata data) throws IOException;
    LyricResult getLyric(MediaInfo mediaInfo) throws IOException;

    class LyricResult {
        public String mLyric;
        public String mTranslatedLyric;
        public long mDistance;
        public String mSource = "Local";
        public int mOffset = 0;
        public MediaInfo realInfo;

        public String toSting() {
            return "Distance: " + mDistance + "\n" +
                    "Source: " + mSource + "\n" +
                    "Offset: " + mOffset + "\n" +
                    "Lyric: " + mLyric + "\n" +
                    "TranslatedLyric: " + mTranslatedLyric + "\n" +
                    "RealInfo: " + realInfo;
        }
    }

    class MediaInfo {
        public String title;
        public String artist;
        public String album;
        public long duration;
        public MediaInfo(String title, String artist, String album, long duration) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.duration = duration;
        }

        public MediaInfo(MediaMetadata mediaMetadata) {
            this.title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            this.artist = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            this.album = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
            this.duration = mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION);

            if (this.title == null) {
                this.title = "";
            }
            if (this.artist == null) {
                this.artist = "";
            }
            if (this.album == null) {
                this.album = "";
            }
        }

        public String toString() {
            return String.format(Locale.getDefault(),"title: %s\nartist: %s\nalbum: %s\nduration: %d", title, artist, album, duration);
        }
    }

}
