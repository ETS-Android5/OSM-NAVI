package khushboo.rohit.osmnavi;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by rohit on 19/1/17.
 */

public class ModifyRoute extends Activity {

    MyApp app;
    SQLiteDatabase db;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (MyApp) this.getApplicationContext();
        setContentView(R.layout.modify_route);
        db=app.myDb;


    }

    public void onInstructionChange(View view) {
        EditText et_id = (EditText) findViewById(R.id.editText3);
        String id = et_id.getText().toString();
        EditText et_instruction = (EditText) findViewById(R.id.editText4);
        String instruction = et_instruction.getText().toString();
        db.execSQL("UPDATE routebyinstructions SET description = '" + instruction + "' WHERE id = '" + id + "';");
        finish();
    }
}
