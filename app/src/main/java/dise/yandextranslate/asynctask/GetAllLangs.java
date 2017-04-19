package dise.yandextranslate.asynctask;

import android.os.AsyncTask;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// Класс выполняет запрос для получения всех возможных направлений переводов

public class GetAllLangs extends AsyncTask<Void, Void, String> {

    private final String SERVER = "https://dictionary.yandex.net/api/v1/dicservice.json/getLangs?";
    private final String KEY = "dict.1.1.20170407T082357Z.5c56fb193c7a3c4c.26b95418b08ae72cfff5496bf1e7503a6c4de26f";

    @Override
    protected String doInBackground(Void... params) {
        String body = createBodyRequest();
        String answer = getRequest(body);
        return answer;
    }

    //добавляем к запросу
    public String createBodyRequest() {
        String request = "";
        request += "key=" + KEY;
        return request;
    }

    // Функция для отправки get-запроса
    public String getRequest(String bodyRequest) {
        try {
            URL url = new URL(SERVER + bodyRequest);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            return readAnswerFromServer(connection.getInputStream());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
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
