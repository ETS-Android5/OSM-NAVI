package khushboo.rohit.osmnavi;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Created by rohit on 19/1/17.
 */

public class Debug extends Activity {

    MyApp app;
    SQLiteDatabase db;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (MyApp) this.getApplicationContext();
        setContentView(R.layout.debug_view);
        db=app.myDb;


    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView tv = (TextView)findViewById(R.id.textViewDebug);
        Cursor c=db.rawQuery("SELECT * FROM routebyinstructions", null);
        if(c.getCount()==0)  {
            tv.setText("No records found");
        } else {
            tv.setText("");
            while(c.moveToNext()) {
                String row = c.getString(0) + "\t" + c.getString(1) + "\t" + c.getString(2) + "\t" + c.getString(3) + "\t" + c.getString(4) + "\n";
                tv.append(row);
            }
        }
    }

    public void onDebug(View view) {
        Intent i = new Intent(getBaseContext(), ModifyRoute.class);
        startActivityForResult(i, 1);
        return;
    }
}
