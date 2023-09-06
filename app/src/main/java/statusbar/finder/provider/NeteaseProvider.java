package statusbar.finder.provider;

import android.media.MediaMetadata;
import android.util.Pair;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

import statusbar.finder.provider.utils.HttpRequestUtil;
import statusbar.finder.provider.utils.LyricSearchUtil;

public class NeteaseProvider implements ILrcProvider {

    private static final String NETEASE_BASE_URL = "http://music.163.com/api/";

    private static final String NETEASE_SEARCH_URL_FORMAT = NETEASE_BASE_URL + "search/get?s=%s&type=1&offset=0&limit=5";
    private static final String NETEASE_LRC_URL_FORMAT = NETEASE_BASE_URL + "song/lyric?os=pc&id=%d&lv=-1&kv=-1&tv=-1";

    @Override
    public LyricResult getLyric(MediaMetadata data) throws IOException {
        String searchUrl = String.format(NETEASE_SEARCH_URL_FORMAT, LyricSearchUtil.getSearchKey(data));
        JSONObject searchResult;
        try {
            searchResult = HttpRequestUtil.getJsonResponse(searchUrl);
            if (searchResult != null && searchResult.getLong("code") == 200) {
                JSONArray array = searchResult.getJSONObject("result").getJSONArray("songs");
                Pair<String, Long> pair = getLrcUrl(array, data);
                if (pair != null) {
                    JSONObject lrcJson = HttpRequestUtil.getJsonResponse(pair.first);
                    LyricResult result = new LyricResult();
                    result.mLyric = lrcJson.getJSONObject("lrc").getString("lyric");
                    result.mDistance = pair.second;
                    result.source = "Netease";
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
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String soundName = jsonObject.getString("name");
            String albumName = jsonObject.getJSONObject("album").getString("name");
            JSONArray artists = jsonObject.getJSONArray("artists");
            if (mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST).contains(artists.getJSONObject(0).getString("name"))
            || ZhConverterUtil.toSimple(mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST)).contains(artists.getJSONObject(0).getString("name"))){
                continue; // 由于网易云收录翻唱过多，容易造成词库混乱，所以添加一次 Artist 检测
            }
            long dis = LyricSearchUtil.getMetadataDistance(mediaMetadata, soundName, LyricSearchUtil.parseArtists(artists, "name"), albumName);
            if (dis < minDistance) {
                minDistance = dis;
                currentID = jsonObject.getLong("id");
            }
        }
        if (currentID == -1) {return null;}
        return new Pair<>(String.format(Locale.getDefault(), NETEASE_LRC_URL_FORMAT, currentID), minDistance);
    }
}
