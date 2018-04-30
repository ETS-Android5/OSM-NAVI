package khushboo.rohit.osmnavi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Created by rohit on 19/1/17.
 */

public class ShowDb extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_db);
        TextView tv2 = (TextView) findViewById(R.id.textView2);
        tv2.setText(getIntent().getStringExtra("db"));
    }
}
