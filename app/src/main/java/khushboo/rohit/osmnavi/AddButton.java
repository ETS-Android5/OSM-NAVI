package khushboo.rohit.osmnavi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Created by rohit on 19/1/17.
 */

public class AddButton extends Activity {

    boolean[] tags = {false, false, false, false};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_view);
    }

    public void addTags(View view) {
        Intent i = new Intent(getBaseContext(), AddTags.class);
        startActivityForResult(i, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2) {
            if(resultCode == Activity.RESULT_OK){
                tags = data.getBooleanArrayExtra("tags");
            }
        }
    }

    public void sendMessage(View view) {
        TextView myDescription = (TextView)findViewById(R.id.editText);
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result",myDescription.getText().toString());
        returnIntent.putExtra("tags", tags);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }
}
