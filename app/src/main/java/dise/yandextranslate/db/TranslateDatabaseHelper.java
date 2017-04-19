package dise.yandextranslate.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

//класс создания бд
public class TranslateDatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "yandexTranslate";

    public TranslateDatabaseHelper (Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // создаем таблицу directionOfTranslation для сохранения всех возможных направлений
        // перевода, чтобы не запрашивать их каждый раз заново
        db.execSQL("create table directionOfTranslation ("
                + "_id integer primary key autoincrement, "
                + "fromLang text, "
                + "codeTranslate text, "
                + "toLang text "
                +");");

        //таблица для хранения историй перевода
        db.execSQL("create table historyTranslating (" +
                "_id integer primary key autoincrement, " +
                "textForTranslating text, " +
                "translatingText text, " +
                "codeTranslating text "
                +");");

        //таблица для хранения избранных переводов
        db.execSQL("create table favoriteTranslating (" +
                "_id integer primary key autoincrement, " +
                "textForTranslating text, " +
                "translatingText text, " +
                "codeTranslating text "
                +");");

    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Удаляем старую таблицу и создаём новую
        db.execSQL("DROP TABLE IF EXISTS directionOfTranslation");
        db.execSQL("DROP TABLE IF EXISTS historyTranslating");
        db.execSQL("DROP TABLE IF EXISTS favoriteTranslating");
        onCreate(db);
    }
}
