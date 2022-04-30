package khushboo.rohit.osmnavi;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.speech.RecognizerIntent;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class SearchPOI extends AppCompatActivity {

    private static final int REQ_CODE_CUSTOM_SEARCH = 1;
    private static final int REQ_CODE_SELECT_POI = 2;
    private static final int NAV_TYPE_NAVIGATE = 0;
    double currentLat, currentLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_poi);
        currentLat = getIntent().getDoubleExtra("currentLat", 0);
        currentLong = getIntent().getDoubleExtra("currentLong",0);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_CUSTOM_SEARCH) {
            System.out.println(1);
            if (resultCode == RESULT_OK && data != null) {
                System.out.println(2);
                String result = data.getStringExtra("result");
                System.out.println(result);
                view_poi_list(result);
            }
        } else if(requestCode == REQ_CODE_SELECT_POI) {
            if (resultCode == RESULT_OK && data != null){

                double final_lat = data.getDoubleExtra("destLat", 0);
                double final_long = data.getDoubleExtra("destLong", 0);
                String name = data.getStringExtra("name");

                Intent intentNA = new Intent(getBaseContext(), NavigationActivity.class);
                intentNA.putExtra("Type", NAV_TYPE_NAVIGATE);
                intentNA.putExtra("startLat", currentLat);
                intentNA.putExtra("startLon", currentLong);
                intentNA.putExtra("endLat", final_lat);
                intentNA.putExtra("endLon", final_long);
                intentNA.putExtra("destination", name);

                startActivity(intentNA);
            }
        }
    }

    public void on_cs(View view) {
        Intent i = new Intent(SearchPOI.this, InputPhrase.class);
        startActivityForResult(i, REQ_CODE_CUSTOM_SEARCH);
    }

    //Generate search string URL and call

    // refer https://taginfo.openstreetmap.org/keys/amenity#values
    //amenity tag value =
    public void on_sm(View view) {
        view_poi_list("market_place");
    }

    public void on_sbs(View view) {
        view_poi_list("bus_station");
    }

    public void on_satm(View view) {
        view_poi_list("atm");
    }

    public void view_poi_list(String poi){

        Intent myIntent = new Intent(SearchPOI.this, POIList.class);
        myIntent.putExtra("currentLat", currentLat);
        myIntent.putExtra("currentLong", currentLong);
        myIntent.putExtra("POI", poi);
        SearchPOI.this.startActivityForResult(myIntent, REQ_CODE_SELECT_POI);

    }
}
