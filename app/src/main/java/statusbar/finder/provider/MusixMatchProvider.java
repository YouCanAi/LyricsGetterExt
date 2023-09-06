package statusbar.finder.provider;

import android.media.MediaMetadata;
import android.util.Pair;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.NullPointerException;
import java.net.URLEncoder;
import java.util.Locale;

import statusbar.finder.misc.checkStringLang;
import statusbar.finder.provider.utils.HttpRequestUtil;
import statusbar.finder.provider.utils.LyricSearchUtil;

public class MusixMatchProvider implements ILrcProvider {

    private static final String MUSIXMATCH_BASE_URL = "https://apic.musixmatch.com/ws/1.1/";
    private static final String MUSIXMATCH_TOKEN_URL_FORMAT = MUSIXMATCH_BASE_URL + "token.get?guid=%s&app_id=android-player-v1.0&format=json";
    // private static final String MUSIXMATCH_LRC_URL_FORMAT = MUSIXMATCH_BASE_URL + "macro.subtitles.get?tags=playing&subtitle_format=lrc&usertoken=%s&q_track=%s&q_artist=%s&q_album=%s&app_id=android-player-v1.0&format=json";
    private static final String MUSIXMATCH_LRC_URL_FORMAT = MUSIXMATCH_BASE_URL + "macro.subtitles.get?tags=playing&subtitle_format=lrc&usertoken=%s&track_id=%s&app_id=android-player-v1.0&format=json";
    private static final String MUSIXMATCH_SERACH_URL_FORMAT = MUSIXMATCH_BASE_URL + "macro.search?app_id=android-player-v1.0&usertoken=%s&q=%s";
    private static String MUSIXMATCH_USERTOKEN;
    @Override
    public LyricResult getLyric(MediaMetadata data) throws IOException {
        if (MUSIXMATCH_USERTOKEN  == null) {
            MUSIXMATCH_USERTOKEN = getMusixMatchUserToken("");
            if (MUSIXMATCH_USERTOKEN  == null) {
                return null;
            }
        }
        String searchUrl = String.format(MUSIXMATCH_SERACH_URL_FORMAT, MUSIXMATCH_USERTOKEN , LyricSearchUtil.getSearchKey(data));
        JSONObject searchResult;
        try{
            searchResult = HttpRequestUtil.getJsonResponse(searchUrl);
            if (searchResult != null && searchResult.getJSONObject("message").getJSONObject("header").getLong("status_code") == 200) {
                JSONArray array = searchResult.getJSONObject("message").getJSONObject("body").getJSONObject("macro_result_list").getJSONArray("track_list");
                Pair<String, Long> pair = getLrcUrl(array, data);
                if (pair != null) {
                    JSONObject lrcJson = HttpRequestUtil.getJsonResponse(pair.first);
                    LyricResult result = new LyricResult();
                    result.mLyric = lrcJson.getJSONObject("message").getJSONObject("body").getJSONObject("macro_calls").getJSONObject("track.subtitles.get").getJSONObject("message").getJSONObject("body").getJSONArray("subtitle_list").getJSONObject(0).getJSONObject("subtitle").getString("subtitle_body");
                    result.mDistance = pair.second;
                    result.source = "MusixMatch";
                    return result;
                } else {
                    return null;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private static Pair<String, Long> getLrcUrl(JSONArray jsonArray, MediaMetadata mediaMetadata) throws JSONException {
        long currentID = -1;
        long minDistance = 10000;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i).getJSONObject("track");
            String soundName = jsonObject.getString("track_name");
            String albumName = jsonObject.getString("album_name");
            String artists = jsonObject.getString("artist_name");
            long dis = LyricSearchUtil.getMetadataDistance(mediaMetadata, soundName, artists, albumName);
            if (dis < minDistance) {
                minDistance = dis;
                currentID = jsonObject.getLong("track_id");
            }
        }
        if (currentID == -1) {return null;}
        return new Pair<>(String.format(Locale.getDefault(), MUSIXMATCH_LRC_URL_FORMAT, MUSIXMATCH_USERTOKEN, currentID), minDistance);
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
