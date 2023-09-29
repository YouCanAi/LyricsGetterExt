package statusbar.finder.provider;

import android.media.MediaMetadata;
import android.util.Log;
import android.util.Pair;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import statusbar.finder.misc.Constants;
import statusbar.finder.misc.checkStringLang;
import statusbar.finder.provider.utils.HttpRequestUtil;
import statusbar.finder.provider.utils.LyricSearchUtil;
import statusbar.finder.provider.utils.UnicodeUtil;

public class MusixMatchProvider implements ILrcProvider {

    private static final String MUSIXMATCH_BASE_URL = "https://apic.musixmatch.com/ws/1.1/";
    private static final String MUSIXMATCH_TOKEN_URL_FORMAT = MUSIXMATCH_BASE_URL + "token.get?guid=%s&app_id=android-player-v1.0&format=json";
    private static final String MUSIXMATCH_LRC_URL_FORMAT = MUSIXMATCH_BASE_URL + "macro.subtitles.get?tags=playing&subtitle_format=lrc&usertoken=%s&track_id=%d&app_id=android-player-v1.0&format=json";
    private static final String MUSIXMATCH_SERACH_URL_FORMAT = MUSIXMATCH_BASE_URL + "macro.search?app_id=android-player-v1.0&usertoken=%s&q=%s";
    private static final String MUSIXMATCH_LRC_SERACH_URL_FORMAT = MUSIXMATCH_BASE_URL + "macro.subtitles.get?tags=playing&subtitle_format=lrc&usertoken=%s&q_track=%s&q_artist=%s&q_album=%s&app_id=android-player-v1.0&format=json";
    private static final String MUSIXMATCH_TRANS_LRC_URL_FORMAT = MUSIXMATCH_BASE_URL + "crowd.track.translations.get?usertoken=%s&translation_fields_set=minimal&selected_language=%s&track_id=%d&comment_format=text&part=user&format=json&app_id=android-player-v1.0&tags=playing";
    private static String MUSIXMATCH_USERTOKEN;
    
    @Override
    public LyricResult getLyric(MediaMetadata data) throws IOException {
        if (MUSIXMATCH_USERTOKEN  == null) {
            MUSIXMATCH_USERTOKEN = getMusixMatchUserToken("");
            if (MUSIXMATCH_USERTOKEN  == null) {
                return null;
            }
        }
        String searchUrl = String.format(Locale.getDefault(), MUSIXMATCH_SERACH_URL_FORMAT, MUSIXMATCH_USERTOKEN , LyricSearchUtil.getSearchKey(data));
        JSONObject searchResult;
        try{
            searchResult = HttpRequestUtil.getJsonResponse(searchUrl);
            if (searchResult != null && searchResult.getJSONObject("message").getJSONObject("header").getLong("status_code") == 200) {
                JSONArray array = searchResult.getJSONObject("message").getJSONObject("body").getJSONObject("macro_result_list").getJSONArray("track_list");
                Pair<String, Long> pair = getLrcUrl(array, data);
                LyricResult result = new LyricResult();
                long trackId = -1;
                if (pair != null) {
                    JSONObject lrcJson = HttpRequestUtil.getJsonResponse(pair.first);
                    result.mLyric = lrcJson.getJSONObject("message").getJSONObject("body").getJSONObject("macro_calls").getJSONObject("track.subtitles.get").getJSONObject("message").getJSONObject("body").getJSONArray("subtitle_list").getJSONObject(0).getJSONObject("subtitle").getString("subtitle_body");
                    JSONObject infoJson = lrcJson.getJSONObject("message").getJSONObject("body").getJSONObject("macro_calls").getJSONObject("matcher.track.get").getJSONObject("message").getJSONObject("body").getJSONObject("track");
                    trackId = infoJson.getLong("track_id");
                    result.mDistance = pair.second;
                    result.source = "MusixMatch";
                } else {
                    // 无法通过 id 寻找到歌词时
                    // 则尝试使用直接搜索歌词的方法
                    String lrcUrl;
                    String track = toSimpleURLEncode(data.getString(MediaMetadata.METADATA_KEY_TITLE));
                    String artist = toSimpleURLEncode(data.getString(MediaMetadata.METADATA_KEY_ARTIST));
                    String album = toSimpleURLEncode(data.getString(MediaMetadata.METADATA_KEY_ALBUM));
                    lrcUrl = String.format(Locale.getDefault(), MUSIXMATCH_LRC_SERACH_URL_FORMAT,
                            MUSIXMATCH_USERTOKEN,
                            track,
                            artist,
                            album);
                    JSONObject lrcJson = HttpRequestUtil.getJsonResponse(lrcUrl);
                    JSONObject subTitleJson = lrcJson.getJSONObject("message").getJSONObject("body").getJSONObject("macro_calls").getJSONObject("track.subtitles.get").getJSONObject("message").getJSONObject("body").getJSONArray("subtitle_list").getJSONObject(0).getJSONObject("subtitle");
                    JSONObject infoJson = lrcJson.getJSONObject("message").getJSONObject("body").getJSONObject("macro_calls").getJSONObject("matcher.track.get").getJSONObject("message").getJSONObject("body").getJSONObject("track");
                    result.mLyric = subTitleJson.getString("subtitle_body");
                    String soundName = infoJson.getString("track_name");
                    String albumName = infoJson.getString("album_name");
                    String artistName = infoJson.getString("artist_name");
                    trackId = infoJson.getLong("track_id");
                    result.mDistance = LyricSearchUtil.getMetadataDistance(data, soundName, artistName, albumName);
                    result.source = "MusixMatch";
                }
                if (Constants.isTransCheck){result.mTransLyric = getTransLyric(result.mLyric, trackId);};
                return result;
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
            String artistName = jsonObject.getString("artist_name");
            long dis = LyricSearchUtil.getMetadataDistance(mediaMetadata, soundName, artistName, albumName);
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
        if (input != null) {
            if (!checkStringLang.isJapanese(input)) {
                result = ZhConverterUtil.toSimple(result);
            }
            try {
                URLEncoder.encode(result, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
            return result;
        } else {
            return null;
        }
    }

    private String getTransLyric(String lyricText, long trackId) {
        String[] languageOptions = {"zh", "tw"};
        int maxAttempts = languageOptions.length;

        for (String selectLang : languageOptions) {
            JSONArray transList = getTranslationsList(trackId, selectLang);
            List<String> modifiedLyricText = new ArrayList<>();
            if (transList != null) {
                for (int curLyricLine = 0; curLyricLine < transList.length(); curLyricLine++) { // 获取每行的原歌词及翻译歌词
                    try {
                        JSONObject currentLyricLineObject = transList.getJSONObject(curLyricLine).getJSONObject("translation");
                        String encodedSnippet = currentLyricLineObject.getString("snippet");
                        String encodedDescription = currentLyricLineObject.getString("description");
                        // 解码 Unicode
                        String snippet = UnicodeUtil.unicodeStr2String(encodedSnippet);
                        String description = UnicodeUtil.unicodeStr2String(encodedDescription);
                        Log.d("getTransLyric: ", String.format(Locale.getDefault(),"s: %s d: %s", snippet, description));
                        for (String lyricLine : lyricText.split("\n")) {
                            String[] lyric = extractLyric(lyricLine); // 提取歌词
                            if (lyric != null) {
                                if (Objects.equals(lyric[1], snippet)){
                                    modifiedLyricText.add("[" + lyric[0] + "] " + description);
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                Log.d("mLyTe", String.join("\n", modifiedLyricText));
                return String.join("\n", modifiedLyricText);
            }
        }
        return null;
    }

    private String[] extractLyric(String lyricLine) { // 解析歌词行 [0] 时间戳 [1] 歌词文本
        int startIndex = lyricLine.indexOf("[");
        int endIndex = lyricLine.indexOf("]");

        if (startIndex != -1 && endIndex != -1) {
            String timeStamp = lyricLine.substring(startIndex + 1, endIndex);
            String lyricText = lyricLine.substring(endIndex + 1).trim();
            return new String[]{timeStamp, lyricText};
        }

        return null;
    }



    private JSONArray getTranslationsList(long trackId, String selectLang) { // 获取翻译歌词列表
        String transLyricURL = String.format(Locale.getDefault(), MUSIXMATCH_TRANS_LRC_URL_FORMAT, MUSIXMATCH_USERTOKEN, selectLang, trackId);

        try {
            JSONObject transResult = HttpRequestUtil.getJsonResponse(transLyricURL);
            JSONObject header = transResult.getJSONObject("message").getJSONObject("header");
            int statusCode = header.getInt("status_code");

            if (statusCode != 200) {
                return null;
            }

            JSONArray translationsList = transResult.getJSONObject("message").getJSONObject("body").getJSONArray("translations_list");
            return translationsList.length() > 0 ? translationsList : null;
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    private String getMusixMatchUserToken(String guid) { // 获取 MusixMatch Token
        String result;
        try{
            // Form Google
            JSONObject tokenJson;
            String tokenURL = String.format(Locale.getDefault(), MUSIXMATCH_TOKEN_URL_FORMAT, guid);
            tokenJson = HttpRequestUtil.getJsonResponse(tokenURL);
            result = tokenJson.getJSONObject("message").getJSONObject("body").getString("user_token");
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

}
