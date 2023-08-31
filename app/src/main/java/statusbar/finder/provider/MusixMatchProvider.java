package statusbar.finder.provider;

import android.media.MediaMetadata;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import statusbar.finder.provider.utils.HttpRequestUtil;

// 试做
public class MusixMatchProvider implements ILrcProvider {

    private static final String MUSIXMATCH_BASE_URL = "https://apic.musixmatch.com/ws/1.1/";
    private static final String MUSIXMATCH_TOKEN_URL_FORMAT = MUSIXMATCH_BASE_URL + "token.get?guid=%s&app_id=android-player-v1.0&format=json";
    private static final String MUSIXMATCH_LRC_URL_FORMAT = MUSIXMATCH_BASE_URL + "macro.subtitles.get?tags=playing&subtitle_format=lrc&usertoken=%s&q_track=%s&q_artist=%s&q_album=%s&app_id=android-player-v1.0&format=json";

    private static String MUSIXMATCH_USERTOKEN;
    @Override
    public LyricResult getLyric(MediaMetadata data) throws IOException {
        JSONObject lrcFullJson;
        String lrcUrl;
        if (MUSIXMATCH_USERTOKEN  == null) {
            try{
                // Form Google
                JSONObject tokenJson;
                String tokenURL = String.format(MUSIXMATCH_TOKEN_URL_FORMAT, "");
                tokenJson = HttpRequestUtil.getJsonResponse(tokenURL);
                MUSIXMATCH_USERTOKEN = tokenJson.getJSONObject("message").getJSONObject("body").getString("user_token");
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        try{
            String track = URLEncoder.encode(data.getString(MediaMetadata.METADATA_KEY_TITLE), "UTF-8");
            String artist = URLEncoder.encode(data.getString(MediaMetadata.METADATA_KEY_ARTIST), "UTF-8");
            String album = URLEncoder.encode(data.getString(MediaMetadata.METADATA_KEY_ALBUM), "UTF-8");
            lrcUrl = String.format(MUSIXMATCH_LRC_URL_FORMAT,
                    MUSIXMATCH_USERTOKEN,
                    track,
                    artist,
                    album);
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
            return null;
        }
        try{
            LyricResult result = new LyricResult();
            JSONObject subTitleJson;
            lrcFullJson = HttpRequestUtil.getJsonResponse(lrcUrl);
            subTitleJson = lrcFullJson.getJSONObject("message").getJSONObject("body").getJSONObject("macro_calls").getJSONObject("track.subtitles.get").getJSONObject("message").getJSONObject("body").getJSONArray("subtitle_list").getJSONObject(0).getJSONObject("subtitle");
            result.mLyric = subTitleJson.getString("subtitle_body");
            result.mDistance = subTitleJson.getLong("subtitle_length") * 1000;
            result.source = "MusixMatch";
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

}
