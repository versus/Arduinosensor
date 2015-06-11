package arduinosensors.tk.arduinosensors.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by nastenko on 11.06.15.
 */
public class DbHelper extends SQLiteOpenHelper {
    public final static String COLUMN_DATETIME ="sensor_datetime";
    public final static String COLUMN_NAME ="sensor_name";
    public final static String COLUMN_VALUE ="sensor_value";
    public final static int DB_VERSION = 1;
    public final static String DB_NAME = "mSensor";
    public final static String TABLE_NAME_SENSOR ="sensor";
    public final String CREATE_TABLE = "create table "+ TABLE_NAME_SENSOR +" ( id integer primary key autoincrement,"
            + COLUMN_DATETIME + " int, "
            + COLUMN_NAME + " text, "
            + COLUMN_VALUE  + " real );";

    public DbHelper(Context context) {
        // конструктор суперкласса
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("SensorActivity", "--- onCreate database ---");
        Log.d("SensorActivity", "CREATE TABLE: " + CREATE_TABLE);
        // создаем таблицу с полями
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}