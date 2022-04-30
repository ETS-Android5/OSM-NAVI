package khushboo.rohit.osmnavi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SuggestRoute extends Activity {

    String routeName = "";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suggest_route);
        Intent intent = getIntent();
        String startLoc = intent.getStringExtra("startLoc");
        String endLoc = intent.getStringExtra("endLoc");
        Button saveSuggest = (Button) findViewById(R.id.button_suggest);
        routeName = startLoc + " to " + endLoc;
        saveSuggest.setText(routeName);
    }

    public void onSaveSuggest(View view) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("name", routeName);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }
    public void onEditSuggest(View view) {
        Intent i = new Intent(getBaseContext(), SaveRoute.class);
        startActivityForResult(i, 1);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1){
            String routeName = data.getStringExtra("name");
            Intent returnIntent = new Intent();
            returnIntent.putExtra("name", routeName);
            setResult(Activity.RESULT_OK,returnIntent);
            finish();
        }
    }
}
