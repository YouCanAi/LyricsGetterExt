package statusbar.finder.provider.utils;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.CookieHandler;
import java.net.CookieManager;

public class HttpRequestUtil {
    static {
    // 创建 Cookie 管理器
    CookieManager cookieManager = new CookieManager();
    CookieHandler.setDefault(cookieManager);
}

    public static JSONObject getJsonResponse(String url) throws IOException, JSONException {
        return getJsonResponse(url, null);
    }

    public static JSONObject getJsonResponse(String url, String referer) throws IOException, JSONException {
        URL httpUrl = new URL(url);
        JSONObject jsonObject;
        HttpURLConnection connection = (HttpURLConnection) httpUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:77.0) Gecko/20100101 Firefox/77.0");
        if (!TextUtils.isEmpty(referer)) {
            connection.setRequestProperty("Referer", referer);
        }
        connection.setConnectTimeout(2500);
        connection.setReadTimeout(2500);
        connection.connect();
        if (connection.getResponseCode() == 200) {
            // 处理搜索结果
            InputStream in = connection.getInputStream();
            byte[] data = readStream(in);
            // Log.d("data", new String(data));
            try {
                jsonObject = new JSONObject(new String(data));
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
            in.close();
            connection.disconnect();
            return jsonObject;
        } else if (connection.getResponseCode() == 301 || connection.getResponseCode() == 302) { // 处理重定向
            URL movedHttpUrl = new URL(connection.getHeaderField("Location"));
            HttpURLConnection movedConnection = (HttpURLConnection) movedHttpUrl.openConnection();
            movedConnection.setRequestMethod("GET");
            movedConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:77.0) Gecko/20100101 Firefox/77.0");
            if (!TextUtils.isEmpty(referer)) {
                movedConnection.setRequestProperty("Referer", referer);
            }
            String cookies = connection.getHeaderField("Set-Cookie");
            if (cookies != null) {
                movedConnection.setRequestProperty("Cookie", cookies);
            }
            // 设置Cookies
            movedConnection.setConnectTimeout(1000);
            movedConnection.setReadTimeout(1000);
            movedConnection.connect();
            InputStream in = movedConnection.getInputStream();
            byte[] data = readStream(in);
            // Log.d("movedData", new String(data));
            try {
                jsonObject = new JSONObject(new String(data));
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
            in.close();
            connection.disconnect();
            return jsonObject;
        }
        connection.disconnect();
        return null;
    }

    public static byte[] readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            bout.write(buffer, 0, len);
        }
        bout.close();
        inputStream.close();

        return bout.toByteArray();
    }
}
