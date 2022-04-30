package khushboo.rohit.osmnavi;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.LinkedList;

public class POIList extends AppCompatActivity {

    double current_lat, current_long;
    String poi_type;
    OverpassAPIProvider2 overpassProvider;
    MyApp app;
    TextView tv2;
    SQLiteDatabase db;
    ArrayList<POI> namePOI;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poilist);
        tv2 = (TextView) findViewById(R.id.textViewpoilist);
        tv2.setText("");
        current_lat = getIntent().getDoubleExtra("currentLat", 0);
        current_long = getIntent().getDoubleExtra("currentLong",0);
        poi_type = getIntent().getStringExtra("POI");
        app = (MyApp) this.getApplicationContext();
        db = app.myDb;
        namePOI = new ArrayList<>();
        ListView lvItems = (ListView) findViewById(R.id.listViewpoilist);
        lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                System.out.println(position);
                retSelection(position);
            }
        });

    }

    @Override
    public void onStart(){
        super.onStart();
        searchPOIfromName(poi_type);
    }

    public void searchPOIfromName(String POI_type) {

        double lat_float = current_lat;
        double long_float = current_long;
        ArrayList<GeoPoint> poi_tags;
        poi_tags = new ArrayList<>();
        float[] results = new float[3];

        BoundingBox bb = new BoundingBox(lat_float + 0.003, long_float + 0.003, lat_float - 0.003, long_float - 0.003);
        overpassProvider = new OverpassAPIProvider2();
        String urlforpoirequest = overpassProvider.urlForPOISearch("\"" + "amenity" + "\"", bb, 20, 5);
        System.out.println(urlforpoirequest);
        ArrayList<POI> allPOI = overpassProvider.getPOIsFromUrl(urlforpoirequest);

        if (allPOI != null) {

            System.out.println("Size is: " + allPOI.size());
            int ptr = 0;
            String desc = "," + POI_type;
            System.out.println(desc);
            while (ptr < allPOI.size()) {
                String t = allPOI.get(ptr).mDescription;
                System.out.println(t);
                if (t.equals(desc)) {
                    namePOI.add(allPOI.get(ptr));
                }
                ptr++;
            }
            allPOI.clear();
        }

        if (namePOI.size() == 0) {
            Toast.makeText(this, ("No " + POI_type + " nearby"), Toast.LENGTH_SHORT).show();
            tv2.setText("No " + POI_type + " Found");

        } else storeList(POI_type);
    }

    public void storeList(String POI_type){

        db.execSQL("CREATE TABLE IF NOT EXISTS tempPOIsearch(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name VARCHAR, distance VARCHAR );");

        if (namePOI.size() != 0) tv2.setText("Locations Found : " + "\n" + "\n");
        for(int i=0; i<namePOI.size(); i++){
            String name = "", distance = "";
            if(namePOI.get(i).mType!=null) name = (namePOI.get(i).mType + " ");
            name = name + POI_type;
            float[] results = new float[3];
            Location.distanceBetween(current_lat, current_long, namePOI.get(i).mLocation.getLatitude(), namePOI.get(i).mLocation.getLongitude(), results);
            distance = (int) results[0] + " metres";
            db.execSQL("INSERT INTO tempPOIsearch VALUES(NULL, '" + name + "','" + distance + "');");
        }
        viewList();
        db.execSQL("DELETE FROM tempPOIsearch;");
    }

    public void viewList(){
        Cursor pc = db.rawQuery("SELECT  * FROM tempPOIsearch", null);
        ListView lvItems = (ListView) findViewById(R.id.listViewpoilist);
        PoiListCursorAdapter todoAdapter = new PoiListCursorAdapter(this, pc);
        lvItems.setAdapter(todoAdapter);
    }

    public void retSelection(int position){
        POI sel = namePOI.get(position);
        double destLat = sel.mLocation.getLatitude();
        double destLong = sel.mLocation.getLongitude();
        Intent returnIntent = new Intent();
        returnIntent.putExtra("destLat",destLat);
        returnIntent.putExtra("destLong",destLong);
        String name = "";
        if(sel.mType!=null) name = (sel.mType + " ");
        name = name + poi_type;
        returnIntent.putExtra("name", name);

        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

    public void on_backp(View view) {
        namePOI.clear();
        super.onBackPressed();
    }
}
