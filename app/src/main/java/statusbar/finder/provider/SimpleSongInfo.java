package statusbar.finder.provider;

import android.media.MediaMetadata;

public class SimpleSongInfo {
    public String title;
    public String artist;
    public String album;
    public long duration;
    public SimpleSongInfo(String title, String artist, String album, long duration) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
    }

    public SimpleSongInfo(MediaMetadata mediaMetadata) {
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
}
