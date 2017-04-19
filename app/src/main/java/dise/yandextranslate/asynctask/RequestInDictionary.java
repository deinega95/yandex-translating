package dise.yandextranslate.asynctask;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

// класс для выполнения запроса к словарю yandex на перевод текста
public class RequestInDictionary extends AsyncTask<String, Void, String> {

    private final String SERVER = "https://dictionary.yandex.net/api/v1/dicservice.json/lookup?";
    private final String KEY = "dict.1.1.20170407T082357Z.5c56fb193c7a3c4c.26b95418b08ae72cfff5496bf1e7503a6c4de26f";
    // переменная, которая сохраняет ответ от сервера
    private String answer;

    @Override
    protected String doInBackground(String... params) {
        try {
            String text = params[0];
            String code = params[1];
            // создаем тело запроса из направления перевода и текста, который нужно перевести
            String body = createBodyRequest(text, code);
            // выполняем запрос
            getRequest(body);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return answer;
    }

    // дописываем к строке запроса данные
    public String createBodyRequest(String text, String code) {
        String request = "";
        request += "key=" + KEY;

        request += "&lang=" + code;
        text = text.replaceAll(" ", "%20");
        request += "&text=" + text;
        //отображение частей речи на русском языке
        request += "&ui=" + "ru";
        return request;
    }

    // выполняем get-запрос
    public void getRequest(String bodyRequest) {
        try {
            URL url = new URL(SERVER + bodyRequest);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            answer = readAnswerFromServer(connection.getInputStream());
        } catch (Exception ex) {ex.printStackTrace();}
    }

    //функция для корректного отображения ответа сервера
    public String readAnswerFromServer(InputStream in) {
        StringBuffer buf = new StringBuffer();
        try {
            InputStreamReader reader = new InputStreamReader(in);
            int c;
            while ((c = reader.read()) != -1) {
                buf.append((char)c);
            }
        } catch (Exception ex) {}
        return buf.toString();
    }
}
