package dise.yandextranslate.asynctask;

import android.os.AsyncTask;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// класс для выполнения запроса к переводчику yandex на перевод текста
public class RequestInTranslateApi extends AsyncTask<String, Void, String> {

    private final String SERVER = "https://translate.yandex.net/api/v1.5/tr.json/translate?";
    private final String KEY = "trnsl.1.1.20170405T195426Z.69f86b2689d81796.b920c7127f54525006559715596d944ec7ada547";
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
        request += "&format=" + "plain";
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
