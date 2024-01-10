package statusbar.finder.provider;

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

import android.media.MediaMetadata;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import android.util.Base64;
import android.util.Pair;

import statusbar.finder.provider.utils.HttpRequestUtil;
import statusbar.finder.provider.utils.LyricSearchUtil;

public class KugouProvider implements ILrcProvider {

    private static final String KUGOU_BASE_URL = "https://lyrics.kugou.com/";
    private static final String KUGOU_SEARCH_URL_FORMAT = KUGOU_BASE_URL + "search?ver=1&man=yes&client=pc&keyword=%s&duration=%d";
    private static final String KUGOU_LRC_URL_FORMAT = KUGOU_BASE_URL + "download?ver=1&client=pc&id=%d&accesskey=%s&fmt=lrc&charset=utf8";

    @Override
    public LyricResult getLyric(MediaMetadata data) throws IOException {
        return getLyric(new SimpleSongInfo(data));
    }

    @Override
    public LyricResult getLyric(SimpleSongInfo simpleSongInfo) throws IOException {
        String searchUrl = String.format(Locale.getDefault(), KUGOU_SEARCH_URL_FORMAT, LyricSearchUtil.getSearchKey(simpleSongInfo), simpleSongInfo.duration);
        JSONObject searchResult;
        try {
            searchResult = HttpRequestUtil.getJsonResponse(searchUrl);
            if (searchResult != null && searchResult.getLong("status") == 200) {
                JSONArray array = searchResult.getJSONArray("candidates");
                Pair<String, Long> pair = getLrcUrl(array, simpleSongInfo);
                if(pair != null){
                    JSONObject lrcJson = HttpRequestUtil.getJsonResponse(pair.first);
                    if (lrcJson == null) {
                        return null;
                    }
                    LyricResult result = new LyricResult();
                    result.mLyric = unescapeHtml4(new String(Base64.decode(lrcJson.getString("content").getBytes(), Base64.DEFAULT)));
                    result.mDistance = pair.second;
                    result.mSource = "Kugou";
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

//    private static Pair<String, Long> getLrcUrl(JSONArray jsonArray, MediaMetadata mediaMetadata) throws JSONException {
//        return getLrcUrl(jsonArray, mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE), mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM), mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
//    }

    private static Pair<String, Long> getLrcUrl(JSONArray jsonArray, SimpleSongInfo simpleSongInfo) throws JSONException {
        return getLrcUrl(jsonArray, simpleSongInfo.title, simpleSongInfo.artist, simpleSongInfo.album);
    }

    private static Pair<String, Long> getLrcUrl(JSONArray jsonArray, String songTitle, String songArtist, String songAlbum) throws JSONException {
        String currentAccessKey = "";
        long minDistance = 10000;
        long currentId = -1;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String soundName = jsonObject.getString("song");
            String artist = jsonObject.getString("singer");
            long dis = LyricSearchUtil.calculateSongInfoDistance(songTitle, songArtist, songAlbum, soundName, artist, null);
            if (dis < minDistance) {
                minDistance = dis;
                currentId = jsonObject.getLong("id");
                currentAccessKey = jsonObject.getString("accesskey");
            }
        }
        if (currentId == -1) {return null;}
        return new Pair<>(String.format(Locale.getDefault(), KUGOU_LRC_URL_FORMAT, currentId, currentAccessKey), minDistance);
    }
}
