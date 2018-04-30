package khushboo.rohit.osmnavi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * Created by rohit on 6/4/17.
 */

public class AddTags extends Activity {

    boolean[] tags = {false, false, false, false};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_tags);
    }

    public void sendMessage(View view) {
        CheckBox cb1 = (CheckBox) findViewById(R.id.checkBox);
        CheckBox cb2 = (CheckBox) findViewById(R.id.checkBox2);
        CheckBox cb3 = (CheckBox) findViewById(R.id.checkBox3);
        CheckBox cb4 = (CheckBox) findViewById(R.id.checkBox4);
        if (cb1.isChecked())
            tags[0] = true;
        if (cb2.isChecked())
            tags[1] = true;
        if (cb3.isChecked())
            tags[2] = true;
        if (cb4.isChecked())
            tags[3] = true;
        Intent returnIntent = new Intent();
        returnIntent.putExtra("tags", tags);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

}
