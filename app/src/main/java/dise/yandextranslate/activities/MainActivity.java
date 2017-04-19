package dise.yandextranslate.activities;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import dise.yandextranslate.R;
import dise.yandextranslate.adapters.DictionaryAdapter;
import dise.yandextranslate.adapters.FavoriteAdapter;
import dise.yandextranslate.adapters.HistoryAdapter;
import dise.yandextranslate.asynctask.RequestInDictionary;
import dise.yandextranslate.asynctask.RequestInTranslateApi;
import dise.yandextranslate.db.ElementHistoryOrFavorite;
import dise.yandextranslate.db.TranslateDatabaseHelper;
import dise.yandextranslate.asynctask.GetAllLangs;
import ru.yandex.speechkit.Error;
import ru.yandex.speechkit.Recognizer;
import ru.yandex.speechkit.SpeechKit;
import ru.yandex.speechkit.Synthesis;
import ru.yandex.speechkit.Vocalizer;
import ru.yandex.speechkit.VocalizerListener;
import ru.yandex.speechkit.gui.RecognizerActivity;


// Стартовая страница, которая открывается при открытии приложения.
public class MainActivity extends AppCompatActivity implements VocalizerListener {

    // api-ключ для speechkit
    private final String API_KEY_SPEECHKIT = "9ee81cb1-ae80-4433-ab34-b7224b1d393a";

    // voсalizer ля прослушивания текста
    private Vocalizer vocalizer;

    //переменная для проверки распознания речи
    private static final int REQUEST_CODE = 31;
    //классы для работы с бд
    private TranslateDatabaseHelper translateDatabase;
    private SQLiteDatabase db;

    //arraylist который содержит id всех направлений перевода
    private ArrayList<Integer> idDirection;

    //arraylist строк, которые будут выводиться в выпадающем списке, которые отвечает за направление перевода
    private  ArrayList<String> allDirection;

    //переменная для хранения направления перевода
    private String directionTranslating;

    //arraylist хранит все данные, полученные от словаря - яндекс
    private ArrayList<ArrayList<String>> translatingText;

    //arrayList хранит данные, полученные от яндекс - переводчика
    private ArrayList<String> mainTranslatingData;

    //адаптеры для отображения истории
    private HistoryAdapter adapter;

    //адаптеры для отображения избранного
    private FavoriteAdapter adapterFavorite;

    private TabHost tabHost;

    //сохраняем номе ртекущей вкладки
    private Integer currentTab;

    // переменная для удаления элемнеов из истории
    private boolean deleteFromHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt("tab");
        } else {
            currentTab = 0;
        }
        setContentView(R.layout.activity_tab);

        install();

        //инициализируем speechKit
        SpeechKit.getInstance().configure(getApplicationContext(), API_KEY_SPEECHKIT);

        downloadDirectionTranslating();
        launch();
    }

    //запускаем проверку editText, чтобы при вводе текста сразу переводилось
    private void launch() {
        //находим editText, который содержит текст, который необходимо перевести
        final EditText editText = (EditText) findViewById(R.id.editText);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    String language = directionTranslating.split("-")[0];
                    if (language.equals("ru") | language.equals("en") || language.equals("tr") || language.equals("uk")){
                        findViewById(R.id.ib_speakerEdit).setVisibility(View.VISIBLE);
                    } else {
                        findViewById(R.id.ib_speakerEdit).setVisibility(View.INVISIBLE);
                    }
                    language = directionTranslating.split("-")[1];
                    if (language.equals("ru") | language.equals("en") || language.equals("tr") || language.equals("uk")){
                        findViewById(R.id.ib_speaker).setVisibility(View.VISIBLE);
                    } else {
                        findViewById(R.id.ib_speaker).setVisibility(View.INVISIBLE);
                    }

                    decisionAboutAdd(s.toString());
                    findViewById(R.id.buttonClearEdit).setVisibility(View.VISIBLE);
                    RequestInDictionary requestForTranslate = new RequestInDictionary();
                    requestForTranslate.execute(s.toString(), directionTranslating);
                    try {
                        parseTranslatingText(requestForTranslate.get(), s.toString());
                        TextView tv = (TextView) findViewById(R.id.tv_TranslatingText);
                        String value = mainTranslatingData.get(0);
                        tv.setText(value);

                        tv = (TextView) findViewById(R.id.tv_startingText);
                        value = mainTranslatingData.get(1);
                        tv.setText(value);

                        tv = (TextView) findViewById(R.id.tv_PartOfSpeech);
                        value = mainTranslatingData.get(2);
                        tv.setText(value);

                        ListView lv = (ListView) findViewById(R.id.lv_Dictionary);
                        DictionaryAdapter adapterDictionary = new DictionaryAdapter(MainActivity.this, translatingText);
                        lv.setAdapter(adapterDictionary);
                    } catch (Exception ex) {
                        if (mainTranslatingData.size() != 0) {
                            TextView tv = (TextView) findViewById(R.id.tv_TranslatingText);
                            String value = mainTranslatingData.get(0);
                            tv.setText(value);
                        }
                    }

                    final ImageButton ibFavorite = (ImageButton) findViewById(R.id.ib_favoriteMain);
                    ibFavorite.setVisibility(View.VISIBLE);
                    if (isFavorite()) {
                        ibFavorite.setImageResource(R.drawable.favorite_icon);
                    } else {
                        ibFavorite.setImageResource(R.drawable.favorite_false_icon);
                    }
                    ibFavorite.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!isFavorite()) {
                                ibFavorite.setImageResource(R.drawable.favorite_icon);
                                ContentValues record = new ContentValues();
                                record.put("textForTranslating", editText.getText().toString().toLowerCase().trim());
                                record.put("translatingText", mainTranslatingData.get(0).toLowerCase().trim());
                                record.put("codeTranslating", directionTranslating);
                                db.insert("favoriteTranslating", null, record);
                            } else {
                                ibFavorite.setImageResource(R.drawable.favorite_false_icon);
                                db.delete("favoriteTranslating", "textForTranslating = ? AND translatingText = ?" +
                                                " AND codeTranslating = ?",
                                        new String[]{editText.getText().toString().toLowerCase().trim(),
                                                mainTranslatingData.get(0).toLowerCase().trim(),
                                                directionTranslating});
                            }
                        }

                    });
                } else {
                    findViewById(R.id.ib_speakerEdit).setVisibility(View.INVISIBLE);
                    findViewById(R.id.ib_speaker).setVisibility(View.INVISIBLE);
                    findViewById(R.id.ib_favoriteMain).setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    findViewById(R.id.buttonClearEdit).setVisibility(View.INVISIBLE);

                    TextView tv = (TextView)findViewById(R.id.tv_TranslatingText);
                    tv.setText("");

                    tv = (TextView)findViewById(R.id.tv_startingText);
                    tv.setText("");

                    tv = (TextView)findViewById(R.id.tv_PartOfSpeech);
                    tv.setText("");

                    ListView lv = (ListView) findViewById(R.id.lv_Dictionary);
                    lv.setAdapter(new DictionaryAdapter(MainActivity.this, new ArrayList<ArrayList<String>>()));
                }
            }
        });
    }

    //метод, определяющий есть ли данные перевод в избранном
    public boolean isFavorite() {
        EditText et = (EditText) findViewById(R.id.editText);
        String text = et.getText().toString().toLowerCase().trim();
        Cursor cursor = db.query("favoriteTranslating", new String[]{"_id"},
                "textForTranslating = ? AND codeTranslating = ?", new String[]{text, directionTranslating}, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.close();
            return true;
        }
        cursor.close();
        return false;
    }

    // процедура зашгрузки направлений перевода
    public void downloadDirectionTranslating() {
        //проверяем содержатся ли уже в бд направления перевода, если нет - то делаем запрос на их получение
        //и добавляем их в бд
        if (checkingExist()) {
            GetAllLangs g = new GetAllLangs();
            g.execute();
            try {
                String allLang = g.get();
                parseAnswer(allLang);
            } catch (Exception ex) {
                ex.printStackTrace();
                Toast.makeText(this, "Пожалуйста, проверьте подключение к интернету", Toast.LENGTH_SHORT).show();
            }
        }

        //находим выпадающий список для отображения направления перевода и заполняем его данными
        Spinner spDirOfTra = (Spinner) findViewById(R.id.spDirectionOfTranslate);

        //считываем с бд все напрпавления перевода
        getAllDirectionOfTranslate();

        //добавляем в выпадающтй список все направления перевода
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_current, allDirection);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spDirOfTra.setAdapter(adapter);
        spDirOfTra.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                Cursor cursor = db.query("directionOfTranslation", new String[]{"codeTranslate"},
                        "_id = ?", new String[]{Integer.toString(idDirection.get(pos))}, null, null, null);
                cursor.moveToFirst();
                directionTranslating = cursor.getString(0);
                String language = directionTranslating.split("-")[0];
                if (language.equals("ru") | language.equals("en") || language.equals("tr") || language.equals("uk")){
                    findViewById(R.id.ib_speakerEdit).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.ib_speakerEdit).setVisibility(View.INVISIBLE);
                }
                language = directionTranslating.split("-")[1];
                if (language.equals("ru") | language.equals("en") || language.equals("tr") || language.equals("uk")){
                    findViewById(R.id.ib_speaker).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.ib_speaker).setVisibility(View.INVISIBLE);
                }
                EditText et = (EditText) findViewById(R.id.editText);
                String str = et.getText().toString();
                if (str.length() > 0) {
                    decisionAboutAdd(str);
                    RequestInDictionary request = new RequestInDictionary();
                    try {
                        request.execute(str, directionTranslating);
                        parseTranslatingText(request.get(), str);
                        TextView tv = (TextView)findViewById(R.id.tv_TranslatingText);
                        String value = mainTranslatingData.get(0);
                        tv.setText(value);
                        if (mainTranslatingData.size() > 1) {
                            tv = (TextView) findViewById(R.id.tv_startingText);
                            value = mainTranslatingData.get(1);
                            tv.setText(value);

                            tv = (TextView) findViewById(R.id.tv_PartOfSpeech);
                            value = mainTranslatingData.get(2);
                            tv.setText(value);
                        } else {
                            tv = (TextView) findViewById(R.id.tv_startingText);
                            tv.setText("");
                            tv = (TextView) findViewById(R.id.tv_PartOfSpeech);
                            tv.setText("");
                        }
                        ListView lv = (ListView) findViewById(R.id.lv_Dictionary);
                        DictionaryAdapter adapterDictionary = new DictionaryAdapter(MainActivity.this, translatingText);
                        lv.setAdapter(adapterDictionary);
                    } catch (Exception ex) {}
                }

                final ImageButton ibFavorite = (ImageButton) findViewById(R.id.ib_favoriteMain);
                if (et.getText().length() > 0) {
                    ibFavorite.setVisibility(View.VISIBLE);
                    if (isFavorite()) {
                        ibFavorite.setImageResource(R.drawable.favorite_icon);
                    } else {
                        ibFavorite.setImageResource(R.drawable.favorite_false_icon);
                    }
                }
                cursor.close();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    //нициализация переменных класса и создание вкладок
    public void install() {
        // инициализация переменных
        idDirection = new ArrayList<>();
        allDirection = new ArrayList<>();
        directionTranslating = "be-be";
        translatingText = new ArrayList<>();
        mainTranslatingData = new ArrayList<>();
        deleteFromHistory = false;

        //подключаемся к бд
        translateDatabase = new TranslateDatabaseHelper(this);
        db = translateDatabase.getWritableDatabase();

        //инициализируем вкладки
        setTabHost();

        // добавляем слушателя на кнопку удалить,
        // чтобы удалить элемент нужно сначала нажать значок "корзина", а затем нажать и держать несколько секунд
        // на записи, которую нужно удалить
        final ImageButton deleteButton = (ImageButton) findViewById(R.id.delete);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!deleteFromHistory) {
                    String str = getString(R.string.hint);
                    Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
                    deleteButton.setImageResource(R.drawable.delete_activate_icon);
                    deleteFromHistory = true;
                } else {
                    deleteButton.setImageResource(R.drawable.delete_icon);
                    deleteFromHistory = false;
                }
            }
        });

        // чтобы удалить элемент из избранного, нужно нажать на знвчок звезды
        final ImageButton deleteFavorite = (ImageButton) findViewById(R.id.deleteFavorite);
        deleteFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    String str = getString(R.string.hintFavorite);
                    Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
            }
        });


        // добавляем обработчик нажатия кнопки для очищения editText
        final ImageButton bClearEdit = (ImageButton) findViewById(R.id.buttonClearEdit);
        bClearEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText et = (EditText) findViewById(R.id.editText);
                et.setText("");
                TextView tv = (TextView) findViewById(R.id.tv_TranslatingText);
                tv.setText("");
                tv = (TextView) findViewById(R.id.tv_startingText);
                tv.setText("");
                tv = (TextView) findViewById(R.id.tv_PartOfSpeech);
                tv.setText("");
                bClearEdit.setVisibility(View.INVISIBLE);
            }
        });

        //устанавливаем прослушиватель исходного текста, если это русский, английсский, турецкий
        // или украинский язык
        ImageButton btn = (ImageButton) findViewById(R.id.ib_speakerEdit);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText et = (EditText) findViewById(R.id.editText);
                String str = et.getText().toString();
                resetVocalizer();
                String language = directionTranslating.split("-")[0];
                if (language.equals("ru") | language.equals("en") || language.equals("tr") || language.equals("uk")) {
                    vocalizer = Vocalizer.createVocalizer(language, str, true, Vocalizer.Voice.ERMIL);
                    vocalizer.setListener(MainActivity.this);
                    vocalizer.start();
                }
            }
        });

        //устанавливаем прослушиватель переведенного текста, если это русский, английсский, турецкий
        // или украинский язык
        btn = (ImageButton) findViewById(R.id.ib_speaker);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = (TextView) findViewById(R.id.tv_TranslatingText);
                String str = tv.getText().toString();
                resetVocalizer();
                String language = directionTranslating.split("-")[1];
                if (language.equals("ru") | language.equals("en") || language.equals("tr") || language.equals("uk")){
                    vocalizer = Vocalizer.createVocalizer(language, str, true, Vocalizer.Voice.ERMIL);
                    vocalizer.setListener(MainActivity.this);
                    vocalizer.start();

                }
            }
        });

        // настраиваем синтез речи
        ImageButton btnMicr = (ImageButton) findViewById(R.id.ib_microphone);
        btnMicr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RecognizerActivity.class);
                intent.putExtra(RecognizerActivity.EXTRA_MODEL, Recognizer.Model.QUERIES);
                intent.putExtra(RecognizerActivity.EXTRA_LANGUAGE, Recognizer.Language.RUSSIAN);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });
    }

    // формирование вкладки "избранное"
    public void printTabFavorite() {
        final ListView lv = (ListView) findViewById(R.id.lv_Favorite);
        EditText etSearch = (EditText) findViewById(R.id.searchEditTextFavorite);
        ArrayList<ElementHistoryOrFavorite> data = getFavorite();
        adapterFavorite = new FavoriteAdapter(this, data);
        lv.setAdapter(adapterFavorite);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence cs, int start, int before, int count) {
                adapterFavorite.getFilter().filter(cs);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    //функция для получения избранного из бд
    public ArrayList<ElementHistoryOrFavorite> getFavorite() {
        ArrayList<ElementHistoryOrFavorite> result = new ArrayList<>();
        Cursor cursor = db.query("favoriteTranslating", new String[] {"_id", "textForTranslating" , "translatingText",
                "codeTranslating"}, null,null, null, null, null);
        if (cursor.moveToFirst()){
            do {
                Integer id = cursor.getInt(0);
                String text = cursor.getString(1);
                String textTranslating = cursor.getString(2);
                String code = cursor.getString(3);
                ElementHistoryOrFavorite elem = new ElementHistoryOrFavorite(id, text, textTranslating, code);
                result.add(elem);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    // формирование вкладки "история"
    public void printTabHistory() {
        final ListView lv = (ListView) findViewById(R.id.lv_History);

        EditText etSearch = (EditText) findViewById(R.id.searchEditText);
        ArrayList<ElementHistoryOrFavorite> data = getHistory();

        adapter = new HistoryAdapter(this, data);
        lv.setAdapter(adapter);

        lv.setLongClickable(true);
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (deleteFromHistory) {
                    ElementHistoryOrFavorite tr = (ElementHistoryOrFavorite)lv.getItemAtPosition(position);
                    deleteRecordFromHistory(tr);
                    tabHost.setCurrentTab(1);
                }
                return false;
            }
        });
        //добавляем фильтр для поиска
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence cs, int start, int before, int count) {
                adapter.getFilter().filter(cs);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    //процедура удаления элемента из таблицы "historyTranslating" бд после нажатия значка корзины
    // а затем длительного нажатия элемента, который необходимо удалить
    public void deleteRecordFromHistory(ElementHistoryOrFavorite record) {
        db.delete("historyTranslating", "_id = ?", new String[] {record.getId().toString()});
        ArrayList<ElementHistoryOrFavorite> data = getHistory();
        adapter = new HistoryAdapter(this, data);
        tabHost.setCurrentTab(0);
    }

    //функция для получения истории переводов из бд
    public ArrayList<ElementHistoryOrFavorite> getHistory() {
        ArrayList<ElementHistoryOrFavorite> result = new ArrayList<>();
        Cursor cursor = db.query("historyTranslating", new String[] {"_id", "textForTranslating" , "translatingText",
                            "codeTranslating"}, null,null, null, null, null);
        if (cursor.moveToFirst()){
            do {
                Integer id = cursor.getInt(0);
                String text = cursor.getString(1);
                String textTranslating = cursor.getString(2);
                String code = cursor.getString(3);
                ElementHistoryOrFavorite elem = new ElementHistoryOrFavorite(id, text, textTranslating, code);
                result.add(elem);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    //инициализация вкладок
    public void setTabHost() {
        tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("translate");
        tabSpec.setContent(R.id.tab1);
        tabSpec.setIndicator("Переводчик");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("history");
        tabSpec.setContent(R.id.tab2);
        tabSpec.setIndicator("История");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("favorite");
        tabSpec.setContent(R.id.tab3);
        tabSpec.setIndicator("Избранное");
        tabHost.addTab(tabSpec);

        //устанавливаем слушатель на tabhost
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                //Скрываем клавиатуру по умолчанию
                final EditText editText = (EditText) findViewById(R.id.editText);
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                if (tabId.equals("history")) {
                    currentTab = 1;
                    ArrayList<ElementHistoryOrFavorite> result = getHistory();
                    adapter = new HistoryAdapter(MainActivity.this, result);
                    ListView lv = (ListView) findViewById(R.id.lv_History);
                    lv.setAdapter(adapter);

                } else if (tabId.equals("favorite")) {
                    currentTab = 2;
                    ArrayList<ElementHistoryOrFavorite> result = getFavorite();
                    adapterFavorite = new FavoriteAdapter(MainActivity.this, result);
                    ListView lv = (ListView) findViewById(R.id.lv_Favorite);
                    lv.setAdapter(adapterFavorite);
                } else if (tabId.equals("translate")) {
                    currentTab = 0;
                    EditText et = (EditText) findViewById(R.id.editText);
                    if (et.getText().length() == 0) {
                        ImageButton ib = (ImageButton) findViewById(R.id.ib_speakerEdit);
                        ib.setVisibility(View.INVISIBLE);
                        ib = (ImageButton) findViewById(R.id.ib_speaker);
                        ib.setVisibility(View.INVISIBLE);
                        ib = (ImageButton) findViewById(R.id.ib_favoriteMain);
                        ib.setVisibility(View.INVISIBLE);
                    } else {
                        final ImageButton ibFavorite = (ImageButton) findViewById(R.id.ib_favoriteMain);
                        if (isFavorite()) {
                            ibFavorite.setImageResource(R.drawable.favorite_icon);
                        } else {
                            ibFavorite.setImageResource(R.drawable.favorite_false_icon);
                        }
                    }
                }
            }
        });

        tabHost.setCurrentTab(currentTab);
        tabHost.getTabWidget().setBackgroundColor(Color.parseColor("#0000FF"));

        for (int i = 0; i < tabHost.getTabWidget().getChildCount(); ++i) {
            TextView tv = (TextView) tabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title); //Unselected Tabs
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(12);
        }
        printTabHistory();
        printTabFavorite();
    }

    // процедура добавления в историю
    public void decisionAboutAdd(final String str) {
        //если в течении 2 секунд текст в edittext не менялся, то добавляем в историю
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    TimeUnit.MILLISECONDS.sleep(2000);
                    EditText et = (EditText) findViewById(R.id.editText);
                    String current = et.getText().toString();
                    if (current.equals(str)) {
                        addInHistory(str);
                    }
                } catch (InterruptedException ex) {}
            }
        });
        t.start();
    }

    //метод добавления записи в таблицу, где хранятся истории перевода
    public void addInHistory(String str) {
        str = str.toLowerCase().trim();
        if (notExist(str)) {
            ContentValues record = new ContentValues();
            record.put("textForTranslating", str.toLowerCase().trim());
            RequestInTranslateApi request = new RequestInTranslateApi();
            request.execute(str, directionTranslating);
            try {
                String reponse = request.get();
                int start = reponse.indexOf("[");
                int end = reponse.indexOf("]");
                reponse = reponse.substring(start + 2, end - 1).toLowerCase().trim();
                record.put("translatingText", reponse);
                record.put("codeTranslating", directionTranslating);
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
            if (db.isOpen() && directionTranslating != null) {
                db.insert("historyTranslating", null, record);
            }
        }
    }

    // проверяем существует ли уже этот перевод в истории, если нет то возвращаем true, false -иначе
    public boolean notExist(String str) {
        if (db.isOpen()) {
            Cursor cursor = db.query("historyTranslating", new String[]{"_id", "textForTranslating", "translatingText",
                    "codeTranslating"}, "textForTranslating = ? AND codeTranslating = ?", new String[]{str, directionTranslating}, null, null, null);
            int s = cursor.getCount();
            if (cursor.moveToFirst()){
                do{
                    String str2 = cursor.getString(1);
                    String str3 = cursor.getString(2);
                    String str4 = cursor.getString(3);
                    String str5 = cursor.getString(3);
                }while (cursor.moveToNext());
            }
            if (cursor.getCount() > 0) {
                return false;
            }
            cursor.close();
        }
        return true;
    }

    // функция парсинга переведенного текста
    public void parseTranslatingText(String answer, String str) {
        translatingText.clear();
        mainTranslatingData.clear();
        //переменная для подсчета количества элементов, полученных от словаря
        int number = 1;
        // парсим ответы от переводчика и словаря
        JsonParser parser = new JsonParser();
        JsonObject response = (JsonObject) parser.parse(answer);
        JsonArray data = response.get("def").getAsJsonArray();

        // выполняем запрос на простой перевод этого слова
        RequestInTranslateApi request = new RequestInTranslateApi();
        request.execute(str, directionTranslating);

        try {
            String reponse = request.get();
            int start = reponse.indexOf("[");
            int end = reponse.indexOf("]");
            reponse = reponse.substring(start + 2, end - 1);
            mainTranslatingData.add(reponse);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Невозможно перевести это слово", Toast.LENGTH_SHORT).show();
            return;
        }
        if (data.size() == 0) {
            return;
        }
        for (int i = 0; i < data.size(); ++i) {
            JsonObject elem = data.get(i).getAsJsonObject();
            JsonObject object = elem.getAsJsonObject();
            String startText = object.get("text").getAsString();
            String partOfSpeech = object.get("pos").getAsString();
            mainTranslatingData.add(startText);
            mainTranslatingData.add(partOfSpeech);
            ArrayList<String> translatingVersion = new ArrayList<>();
            JsonArray translatingData = object.getAsJsonArray("tr");
            for (int j = 0; j < translatingData.size(); ++j) {
                JsonObject transObject = translatingData.get(j).getAsJsonObject();
                String res = transObject.get("text").getAsString();

                //пытаемся считать синонимы и их значения
                try {
                    translatingVersion.add(String.valueOf(number));
                    res += ", ";
                    JsonArray el = transObject.get("syn").getAsJsonArray();
                    for (int k = 0; k < el.size(); ++k) {
                        JsonObject obj = el.get(k).getAsJsonObject();
                        res += obj.get("text").getAsString();
                        res += ", ";
                    }
                    translatingVersion.add(res.substring(0, res.length() - 2));
                } catch (Exception ex) {
                    res = res.substring(0, res.length() - 2);
                    translatingVersion.add(res);
                }
                try {
                    JsonArray el = transObject.get("mean").getAsJsonArray();
                    res = "(";
                    for (int k = 0; k < el.size(); ++k) {
                        JsonObject obj = el.get(k).getAsJsonObject();
                        res += obj.get("text").getAsString() + ", ";
                    }
                    //удаляем лишнию запятую
                    res = res.substring(0, res.length() - 2);
                    translatingVersion.add(res + ")");
                } catch (Exception ex) {
                    translatingVersion.add("");
                }
                ArrayList<String> arList = new ArrayList<>();
                arList.addAll(translatingVersion);
                number++;
                translatingText.add(arList);
                translatingVersion.clear();
            }
        }
    }

    //функция проверки запрашивали ли ранее список всех направлений перевода
    // если да - то не нужно опять запрашивать, иначе делаем запрос для получения всех направлений
    public boolean checkingExist() {
        Cursor cursor = db.query("directionOfTranslation", new String[]{"_id"},
                null, null, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return  count == 0;
    }

    //процедура считывания из бд всех направлений перевода и заполнение переменных: idDirection и allDirection
    public void getAllDirectionOfTranslate() {
        Cursor cursor = db.query("directionOfTranslation", new String[]{"_id","fromLang", "toLang"},
                null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                Integer id = cursor.getInt(0);
                String from = cursor.getString(1);
                String to = cursor.getString(2);
                idDirection.add(id);
                allDirection.add(from + " ->" + to);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    // парсим полученный ответ, используя библиотеку gson, и добавляем направления перевода в таблицу directionOfTranslation
    public void parseAnswer(String answer) {
        JsonParser parser = new JsonParser();
        JsonArray mainObject = (JsonArray) parser.parse(answer);
        JsonArray res = mainObject.getAsJsonArray();

        for (int i = 0; i < res.size(); ++i) {
            String codeLanguages = res.get(i).getAsString();
            String[] languages = codeLanguages.split("-");
            //преобразовываем коды языков в их название
            String nameLanguageFrom = converterLanguages(languages[0]);
            String nameLanguageTo = converterLanguages(languages[1]);
            // добавдяем в бд
            addDirectionInDB(codeLanguages, nameLanguageFrom, nameLanguageTo);
        }
    }

    // процедура добавления в бд направления перевода
    public void addDirectionInDB (String code, String fromLanguage, String toLanguage) {
        ContentValues record = new ContentValues();
        record.put("codeTranslate", code);
        record.put("fromLang", fromLanguage);
        record.put("toLang", toLanguage);
        db.insert("directionOfTranslation", null, record);
    }

    // Функция которая переводит коды языков в название языков (например: "ru" в "Русский")
    public String converterLanguages(String codeLanguage) {
        switch (codeLanguage) {
            case "be" :
                return "Белорусский";
            case "ru" :
                return "Русский";
            case "bg" :
                return "Болгарский";
            case "cs" :
                return "Чешский";
            case "en" :
                return "Английский";
            case "da" :
                return "Датский";
            case "de" :
                return "Немецкий";
            case "tr" :
                return "Турецкий";
            case "el" :
                return "Греческий";
            case "pt" :
                return "Португальский";
            case "lv" :
                return "Латышский";
            case "fr" :
                return "Французский";
            case "fi" :
                return "Финский";
            case "sk" :
                return "Словацкий";
            case "no" :
                return "Норвежский";
            case "mhr" :
                return "Марийский";
            case "mrj" :
                return "Горномарийский";
            case "pl" :
                return "Польский";
            case "uk" :
                return "Украинский";
            case "nl" :
                return "Нидерландский";
            case "hu" :
                return "Венгерский";
            case "tt" :
                return "Татарский";
            case "sv" :
                return "Шведский";
            case "lt" :
                return "Литовский";
            case "it" :
                return "Итальянский";
            case "et" :
                return "Эстонский";
            case "es" :
                return "Испанский";
            default:
                return codeLanguage;
        }
    }

    //сохраняем текущую вкладку
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("tab", currentTab);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void resetVocalizer() {
        if (vocalizer != null) {
            vocalizer.cancel();
            vocalizer = null;
        }
    }

    //методы для озвучивания текста
    @Override
    public void onVocalizerError(Vocalizer vocalizer, Error error) {
        resetVocalizer();
    }

    @Override
    public void onSynthesisBegin(Vocalizer vocalizer) {}

    @Override
    public void onSynthesisDone(Vocalizer vocalizer, Synthesis synthesis) {}

    @Override
    public void onPlayingBegin(Vocalizer vocalizer) {}

    @Override
    public void onPlayingDone(Vocalizer vocalizer) {}


    //метод для синтеза произнесенной речи
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, requestCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RecognizerActivity.RESULT_OK && data != null) {
                final String result = data.getStringExtra(RecognizerActivity.EXTRA_RESULT);
                EditText et = (EditText) findViewById(R.id.editText);
                et.setText(result);
            } else if (resultCode == RecognizerActivity.RESULT_ERROR) {
                String error = ((ru.yandex.speechkit.Error) data.getSerializableExtra(RecognizerActivity.EXTRA_ERROR)).getString();
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            db.close();
            deleteFromHistory = false;
        } catch (Exception ex){}
    }
}