package khushboo.rohit.osmnavi;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class debugAcitivity extends AppCompatActivity {

    double currentLat, currentLong;
    MyApp app;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentLat = getIntent().getDoubleExtra("currentLat",0);
        currentLong = getIntent().getDoubleExtra("currentLong",0);
        app = (MyApp) this.getApplicationContext();
        db = app.myDb;
        setContentView(R.layout.debug_activity);
    }

    public void onDebugButton(View view) {
        Intent i = new Intent(getBaseContext(), Debug.class);
        startActivity(i);
    }

    public void onStopDebug(View view){
        super.onBackPressed();
    }

    public void showMessage1(View view) {
        StringBuffer buffer = new StringBuffer();

        buffer.append("Current Location: " + currentLat + " " + currentLong + "\n" + "\n");
        System.out.println("Location Details: \n" + buffer.toString());

        Cursor c = db.rawQuery("SELECT * FROM myLocation", null);
        Cursor cc = db.rawQuery("SELECT * FROM locationByTag", null);

        if ((c.getCount() == 0)&&(cc.getCount() == 0)) {
            System.out.println("No records found");
            Toast.makeText(getApplicationContext(), "No Database Found", Toast.LENGTH_LONG).show();
            return;
        }

        if ((c.getCount() != 0)) buffer.append("L A N D M A R K S : " + "\n");
        while (c.moveToNext()) {
            buffer.append("Latitude: " + c.getString(1) + "\n");
            buffer.append("Longitude: " + c.getString(2) + "\n");
            buffer.append("Description: " + c.getString(3) + "\n");
            buffer.append("Id: " + c.getString(0) + "\n");
            buffer.append("Previous Id: " + c.getString(5) + "\n");
            buffer.append("Next Id: " + c.getString(6) + "\n");
        }

        if ((c.getCount() != 0)) buffer.append("\nT A G S : " + "\n");
        while (cc.moveToNext()) {
            buffer.append("Tag id: ");
            buffer.append(cc.getString(1) + "\nNode id: ");
            buffer.append(cc.getString(2) + "\n");
        }

        Intent i = new Intent(getBaseContext(), ShowDb.class);
        i.putExtra("db", buffer.toString());
        startActivity(i);
//        Toast.makeText(getApplicationContext(),buffer.toString(),Toast.LENGTH_LONG).show();
    }

    public void clearData(View view) {
        db.execSQL("DELETE FROM myLocation;");
        db.execSQL("DELETE FROM myTags;");
        db.execSQL("DELETE FROM locationByTag;");
        db.execSQL("DELETE FROM trackdata;");
        db.execSQL("DELETE FROM trackdata30;");
        db.execSQL("DELETE FROM routes;");
        db.execSQL("DELETE FROM routebyinstructions;");
        db.execSQL("DELETE FROM tagroutebyinstructions;");
        Toast.makeText(this, "Data Cleared", Toast.LENGTH_SHORT).show();
    }
}
