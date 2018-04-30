package khushboo.rohit.osmnavi;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.Locale;

/**
 * Created by rohit on 26/4/17.
 */


public class MyApp extends Application{
    public boolean hasRefreshed;
    SQLiteDatabase myDb;

    @Override
    public void onCreate() {
        super.onCreate();
        hasRefreshed=false;
        myDb=openOrCreateDatabase("StudentDB", Context.MODE_PRIVATE, null);
    }

}
