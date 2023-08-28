package statusbar.finder.provider;

import static statusbar.finder.provider.utils.LyricSearchUtil.getMusixMatchSearchKey;

import android.media.MediaMetadata;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import statusbar.finder.provider.utils.HttpRequestUtil;
import statusbar.finder.provider.utils.LyricSearchUtil;
import statusbar.finder.misc.Constants;

// 试做
public class MusixMatchProvider implements ILrcProvider {

    private static final String MUSIXMATCH_BASE_URL = "https://apic-premium.musixmatch.com/ws/1.1/macro.subtitles.get";
    @Override
    public LyricResult getLyric(MediaMetadata data) throws IOException, JSONException {
        JSONObject fullJson;
        LyricResult result = new LyricResult();
        try {
            String lrcUrl = getLrcUrl(data);
            Log.d("URL", lrcUrl);
            fullJson = HttpRequestUtil.getJsonResponse(lrcUrl);
            result.mLyric = fullJson.getJSONObject("message").getJSONObject("body").getJSONObject("macro_calls").getJSONObject("track.subtitles.get")
                    .getJSONObject("message").getJSONObject("body").getJSONArray("subtitle_list").getJSONObject(0).getJSONObject("subtitle").getString("subtitle_body");
            result.mDistance = fullJson.getJSONObject("message").getJSONObject("body").getJSONObject("macro_calls").getJSONObject("track.subtitles.get")
                    .getJSONObject("message").getJSONObject("body").getJSONArray("subtitle_list").getJSONObject(0).getJSONObject("subtitle").getLong("subtitle_length") * 1000;
            // MusixMatch's Distance Only Sec.
            // So... *1000
            
            return result;

            // Toooooooooooo long

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getLrcUrl(MediaMetadata mediaMetadata) throws JSONException, URISyntaxException {
        String[] SearchKey = getMusixMatchSearchKey(mediaMetadata);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("tags", "playing");
        queryParams.put("subtitle_format", "lrc");
        queryParams.put("q_track", SearchKey[0]);
        queryParams.put("q_artist",SearchKey[1]);
        queryParams.put("q_album", SearchKey[2]);
        queryParams.put("usertoken", Constants.MUSIXMATCH_USERTOKEN); // Don't Push This.
        Log.d("MusixMatch", Constants.MUSIXMATCH_USERTOKEN);
        queryParams.put("app_id", "android-player-v1.0");
        queryParams.put("format", "json");
        URI uri = buildURI(MUSIXMATCH_BASE_URL, queryParams);
        return uri.toString();
    }

    private static URI buildURI(String baseUrl, Map<String, String> queryParams) throws URISyntaxException {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (query.length() > 0) {
                query.append("&");
            }
            query.append(entry.getKey()).append("=").append(entry.getValue());
        }

        return new URI(baseUrl + "?" + query.toString());
    }
}
