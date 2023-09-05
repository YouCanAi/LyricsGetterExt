package statusbar.finder.provider;

import android.media.MediaMetadata;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.NullPointerException;
import java.net.URLEncoder;

import statusbar.finder.misc.checkStringLang;
import statusbar.finder.provider.utils.HttpRequestUtil;

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
            MUSIXMATCH_USERTOKEN = getMusixMatchUserToken("");
            if (MUSIXMATCH_USERTOKEN  == null) {
                return null;
            }
        }
        try{
            String track = toSimpleURLEncode(data.getString(MediaMetadata.METADATA_KEY_TITLE));
            String artist = toSimpleURLEncode(data.getString(MediaMetadata.METADATA_KEY_ARTIST));
            String album = toSimpleURLEncode(data.getString(MediaMetadata.METADATA_KEY_ALBUM));
            lrcUrl = String.format(MUSIXMATCH_LRC_URL_FORMAT,
                    MUSIXMATCH_USERTOKEN,
                    track,
                    artist,
                    album);
        } catch (NullPointerException e) {
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

    private String toSimpleURLEncode(String input) {
        String result = input;
        if (!checkStringLang.isJapanese(input)) {
            result = ZhConverterUtil.toSimple(result);
        }
        try {
            URLEncoder.encode(result, "UTF-8");
        } catch (UnsupportedEncodingException e) {
           return null;
        }
        return result;
    }

    private String getMusixMatchUserToken(String guid) { // 获取 MusixMatch Token
        String result;
        try{
            // Form Google
            JSONObject tokenJson;
            String tokenURL = String.format(MUSIXMATCH_TOKEN_URL_FORMAT, guid);
            tokenJson = HttpRequestUtil.getJsonResponse(tokenURL);
            result = tokenJson.getJSONObject("message").getJSONObject("body").getString("user_token");
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

}
