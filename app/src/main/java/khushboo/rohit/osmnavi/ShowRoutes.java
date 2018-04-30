package khushboo.rohit.osmnavi;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * Created by rohit on 19/1/17.
 */

public class ShowRoutes extends ListActivity {

    private String[] lv_arr = {};
    private ListView mainListView = null;
    ArrayList<String> route_names;
    ArrayList<Integer> route_ids;
    ArrayList<String> route_distances;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.showroutes);


        // Prepare an ArrayList of todo items
        route_names = getIntent().getStringArrayListExtra("route_names");
        route_ids = getIntent().getIntegerArrayListExtra("route_ids");
        route_distances = getIntent().getStringArrayListExtra("route_distances");

        this.mainListView = getListView();

        // Bind the data with the list
        lv_arr = route_names.toArray(new String[0]);
        mainListView.setAdapter(new ArrayAdapter<String>(this,
                R.layout.showroutes, R.id.myoutput, lv_arr));

    }

    @Override
    protected void onListItemClick (ListView l, View v, int position, long id) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("selectedRoute", route_ids.get(position));
        returnIntent.putExtra("selectedRouteDistance", route_distances.get(position));
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }
}